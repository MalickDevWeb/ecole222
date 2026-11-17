package ecole_221.sn.ecole.modules.archivage;

import ecole_221.sn.ecole.modules.archivage.application.ArchivageEcolesService;
import ecole_221.sn.ecole.modules.archivage.application.CatalogueBase;
import ecole_221.sn.ecole.modules.archivage.application.FichiersArchive;
import ecole_221.sn.ecole.modules.archivage.application.JournalArchivage;
import ecole_221.sn.ecole.modules.archivage.application.MoteurArchivage;
import ecole_221.sn.ecole.modules.archivage.application.MoteurRestauration;
import ecole_221.sn.ecole.modules.archivage.application.PlanArchivage;
import ecole_221.sn.ecole.modules.archivage.application.RegistreTables;
import ecole_221.sn.ecole.modules.archivage.application.ReglagesArchivageService;
import ecole_221.sn.ecole.modules.archivage.application.TransactionsArchivage;
import ecole_221.sn.ecole.modules.archivage.application.format.ChiffrementFlux;
import ecole_221.sn.ecole.modules.archivage.domain.exception.ArchivageException;
import ecole_221.sn.ecole.modules.archivage.domain.model.ArchivageEcole;
import ecole_221.sn.ecole.modules.archivage.domain.model.Declenchement;
import ecole_221.sn.ecole.modules.archivage.domain.model.EtapeArchivage;
import ecole_221.sn.ecole.modules.archivage.domain.model.EtatArchivage;
import ecole_221.sn.ecole.modules.archivage.domain.port.CleDesArchives;
import ecole_221.sn.ecole.modules.archivage.domain.port.StockageArchive;
import ecole_221.sn.ecole.modules.archivage.infrastructure.persistence.ArchivagesEcolesJdbc;
import ecole_221.sn.ecole.modules.archivage.infrastructure.persistence.EcolesArchivablesJdbc;
import ecole_221.sn.ecole.modules.archivage.infrastructure.stockage.StockageArchiveLocal;
import ecole_221.sn.ecole.modules.audit.infrastructure.persistence.JournalAudit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Aller-retour RÉEL sur PostgreSQL 15 (base jetable, migrations Flyway V1 à V24) : archivage → purge → restauration,
 * comparaison ligne à ligne de TOUTES les tables avant / après ; refus en cas de conflit sans rien écrire ; échec de
 * relecture sans rien supprimer ; reprise d'une purge interrompue ; suppression définitive seulement confirmée ;
 * archivage automatique au seuil ; refus en production sans stockage cloud. Données de test nommées comme telles.
 */
@EnabledIf("ecole_221.sn.ecole.modules.archivage.PostgresJetable#disponible")
class ArchivageAllerRetourPostgresTest {

    private static final String SLUG_A = "ecole-a-test";
    /** Tables dont le contenu change légitimement pendant l'opération (suivi, journal, statut de l'école). */
    private static final Set<String> HORS_COMPARAISON = Set.of("flyway_schema_history", "archivages_ecoles",
            "archivages_ecoles_journal", "audit_logs", "organizations");

    @TempDir
    Path dossier;
    private String base;
    private DataSource source;
    private JdbcTemplate jdbc;
    private byte[] cle;
    private StockageArchiveLocal local;
    private ArchivagesEcolesJdbc archivages;

    @BeforeEach
    void baseNeuve() throws Exception {
        base = "archivage_" + Long.toHexString(System.nanoTime());
        source = PostgresJetable.nouvelleBase(base);
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(source);
        cle = ChiffrementFlux.nouvelleCle();
        local = new StockageArchiveLocal(dossier.resolve("archives"), false, false);
        archivages = new ArchivagesEcolesJdbc(jdbc);
        peupler();
    }

    @AfterEach
    void nettoyer() {
        PostgresJetable.supprimerBase(base);
    }

    private ArchivageEcolesService service(StockageArchive stockage, Executor executeur) {
        TransactionsArchivage tx = new TransactionsArchivage(source);
        EcolesArchivablesJdbc ecoles = new EcolesArchivablesJdbc(jdbc);
        CleDesArchives cles = () -> new CleDesArchives.CleMaitresse(cle, ChiffrementFlux.identifiant(cle));
        JournalAudit audit = new JournalAudit(jdbc);
        JournalArchivage journal = new JournalArchivage(archivages, audit);
        ReglagesArchivageService reglages = new ReglagesArchivageService(jdbc, audit);
        FichiersArchive fichiers = new FichiersArchive(dossier.resolve("tmp"));
        MoteurArchivage moteur = new MoteurArchivage(tx, archivages, ecoles, stockage, cles, journal, reglages, fichiers, 2);
        MoteurRestauration restauration = new MoteurRestauration(source, tx, archivages, ecoles, stockage, cles, journal, fichiers);
        return new ArchivageEcolesService(archivages, ecoles, stockage, cles, reglages, journal, moteur, restauration, tx, executeur);
    }

    private ArchivageEcolesService service() {
        return service(local, Runnable::run);
    }

    // ── Données de test (écoles A et B ; A suspendue depuis 40 jours) ───────────

    private void peupler() {
        String[] sql = {
            "INSERT INTO organizations (id, name, slug, type, status, created_at, suspended_at, settings) VALUES "
                + "('org-a', 'École A (test archivage)', 'ecole-a-test', 'ECOLE', 'SUSPENDED', '2026-01-05 08:00', localtimestamp - interval '40 days', '{\"theme\": \"vert\"}'),"
                + "('org-b', 'École B (test archivage)', 'ecole-b-test', 'ECOLE', 'ACTIVE', '2026-01-06 08:00', NULL, NULL),"
                + "('org-c', 'École C (test archivage)', 'ecole-c-test', 'ECOLE', 'SUSPENDED', '2026-01-07 08:00', localtimestamp - interval '10 days', NULL)",
            "INSERT INTO users (id, email, nom, prenom, role, telephone, matricule, photo_url) VALUES "
                + "('u-admin-a', 'admin.a@test.invalid', 'Test', 'Admin A', 'ADMIN', '+221700000001', 'ADM-A', NULL),"
                + "('u-eleve-a', 'eleve.a@test.invalid', 'Test', 'Élève A', 'ETUDIANT', '+221700000002', 'ELV-A', 'https://res.cloudinary.com/demo/image/upload/photo-eleve-a.png'),"
                + "('u-parent', 'parent@test.invalid', 'Test', 'Parent A et B', 'PARENT', '+221700000003', NULL, NULL),"
                + "('u-admin-b', 'admin.b@test.invalid', 'Test', 'Admin B', 'ADMIN', '+221700000004', 'ADM-B', NULL)",
            "INSERT INTO organization_users (id, organization_id, user_id, role, status, joined_at, permissions) VALUES "
                + "('ou-1', 'org-a', 'u-admin-a', 'ADMIN', 'ACTIVE', '2026-01-05 08:00', NULL),"
                + "('ou-2', 'org-a', 'u-eleve-a', 'ETUDIANT', 'ACTIVE', '2026-01-05 08:00', NULL),"
                + "('ou-3', 'org-a', 'u-parent', 'PARENT', 'ACTIVE', '2026-01-05 08:00', ARRAY['lire', 'écrire']),"
                + "('ou-4', 'org-b', 'u-parent', 'PARENT', 'ACTIVE', '2026-01-06 08:00', NULL),"
                + "('ou-5', 'org-b', 'u-admin-b', 'ADMIN', 'ACTIVE', '2026-01-06 08:00', NULL)",
            "INSERT INTO parents (id, organization_id, email, nom, prenom, telephone, user_id, created_at) VALUES "
                + "('par-a', 'org-a', 'parent.a@test.invalid', 'Test', 'Parent', '+221700000003', 'u-parent', '2026-01-05 09:00:00.123456'),"
                + "('par-b', 'org-b', 'parent.b@test.invalid', 'Test', 'Parent', '+221700000003', 'u-parent', '2026-01-06 09:00')",
            "INSERT INTO parent_enfant (id, organization_id, parent_id, enfant_id, relation) VALUES ('pe-a', 'org-a', 'par-a', 'u-eleve-a', 'Mère')",
            "INSERT INTO classes (id, organization_id, capacity, current_size, name) VALUES ('cl-a1', 'org-a', 30, 1, '6e A'), ('cl-b1', 'org-b', 25, 0, '6e B')",
            "INSERT INTO inscriptions_classes (id, organization_id, classe_id, user_id, updated_at) VALUES ('ins-a1', 'org-a', 'cl-a1', 'u-eleve-a', '2026-02-01 10:00')",
            "INSERT INTO attendances (id, organization_id, student_id, date, status, recorded_at) VALUES "
                + "('att-a1', 'org-a', 'u-eleve-a', '2026-03-02', 'ABSENT', '2026-03-02 08:15:30.5'),"
                + "('att-a2', 'org-a', 'u-eleve-a', NULL, NULL, NULL), ('att-b1', 'org-b', 'u-admin-b', '2026-03-02', 'PRESENT', NULL)",
            "INSERT INTO notifications (id, organization_id, recipient_id, title, message, is_read, created_at, additional_data) VALUES "
                + "('not-a1', 'org-a', 'u-eleve-a', 'Absence', E'Ligne 1\\nLigne 2 « guillemets » \"doubles\" ; virgule, point-virgule', false, '2026-03-02 09:00', '{\"x\": 1}')",
            "INSERT INTO group_chats (id, organization_id, chat_type, creator_id, created_at, name, avatar_url, archived) VALUES "
                + "('gc-a1', 'org-a', 'GROUP', 'u-admin-a', '2026-02-01 10:00', 'Classe 6e A', 'https://res.cloudinary.com/demo/image/upload/avatar-a.png', false),"
                + "('gc-b1', 'org-b', 'GROUP', 'u-admin-b', '2026-02-01 10:00', 'Classe 6e B', NULL, false)",
            "INSERT INTO group_chat_participants (chat_id, user_id) VALUES ('gc-a1', 'u-admin-a'), ('gc-a1', 'u-eleve-a'), ('gc-a1', 'u-eleve-a'), ('gc-b1', 'u-admin-b')",
            "INSERT INTO group_chat_admins (chat_id, user_id) VALUES ('gc-a1', 'u-admin-a'), ('gc-b1', 'u-admin-b')",
            "INSERT INTO roles (id, organization_id, name, permissions, is_system_role) VALUES "
                + "('role-a', 'org-a', 'Surveillant', ARRAY['presences.lire'], false), ('role-b', 'org-b', 'Surveillant', NULL, false)",
            "INSERT INTO permissions (id, action, category, name, resource) VALUES ('perm-1', 'lire', 'vie', 'presences.lire', 'presences')",
            "INSERT INTO role_permissions (id, role_id, permission_id, conditions) VALUES "
                + "('rp-a', 'role-a', 'perm-1', '{\"portee\": \"ecole\"}'), ('rp-b', 'role-b', 'perm-1', NULL)",
            "INSERT INTO documents (id, organization_id, name, url, size) VALUES ('doc-a', 'org-a', 'Règlement.pdf', 'https://res.cloudinary.com/demo/raw/upload/reglement-a.pdf', '12 Ko')",
            "INSERT INTO justificatifs_absence (id, organization_id, eleve_id, parent_id, presence_id, motif, statut, depose_le, fichier_url) VALUES "
                + "('ja-a', 'org-a', 'u-eleve-a', 'u-parent', 'att-a1', 'Rendez-vous médical', 'EN_ATTENTE', '2026-03-02 12:00', 'https://res.cloudinary.com/demo/raw/upload/justificatif-a.pdf')",
            "INSERT INTO transactions (id, organization_id, amount, student_id, description, transaction_date) VALUES "
                + "('tr-a', 'org-a', 25000, 'u-eleve-a', 'Écolage de mars — espèces', '2026-03-03 11:00')",
            "INSERT INTO student_finances (id, organization_id, student_id, debt, paid, total_required) VALUES ('sf-a', 'org-a', 'u-eleve-a', 50000, 25000, 75000)",
            "INSERT INTO evaluations_cours (id, organization_id, course_id, coefficient, note_sur, date_evaluation, session, titre, type, verrouillee) VALUES "
                + "('ev-a', 'org-a', 'cours-a', 2.5, 20, '2026-03-10', 'NORMALE', 'Devoir 1', 'DEVOIR', false)",
            "INSERT INTO notes_evaluations (id, organization_id, course_id, evaluation_id, student_id, valeur) VALUES ('ne-a', 'org-a', 'cours-a', 'ev-a', 'u-eleve-a', 13.75)",
            "INSERT INTO reference_values (id, organization_id, list_code, code, label, active, sort_order, system_value, updated_at) VALUES "
                + "('rv-a', 'org-a', 'motifs_absence', 'MALADIE', 'Maladie', true, 1, false, '2026-01-05 08:00'),"
                + "('rv-sys', NULL, 'motifs_absence', 'AUTRE', 'Autre', true, 9, true, '2026-01-01 00:00')",
            "INSERT INTO organization_settings (organization_id, academic_year_start, language) VALUES ('org-a', '2025-10-01', 'fr'), ('org-b', '2025-10-01', 'fr')",
            "INSERT INTO statuts_frais_eleves (user_id, organization_id, statut, updated_at) VALUES ('u-eleve-a', 'org-a', 'EN_RETARD', '2026-03-03 11:00')",
            "INSERT INTO profils_eleves (id, organization_id, user_id, groupe_sanguin) VALUES ('pr-a', 'org-a', 'u-eleve-a', 'O+')",
            "INSERT INTO candidates (id, organization_id, email, eleve_id, academic_score, registration_fee_paid) VALUES ('cand-a', 'org-a', 'candidat.a@test.invalid', 'u-eleve-a', 14.5, true)",
            "INSERT INTO messages (id, organization_id, group_id, sender_id, content, timestamp, media_url) VALUES "
                + "('msg-a', 'org-a', 'gc-a1', 'u-admin-a', 'Bonjour à tous', '2026-03-01 07:30', NULL)",
            // tables qui RESTENT dans la base principale
            "INSERT INTO plans (id, name, price_monthly, price_yearly) VALUES ('plan-1', 'Essentiel', 15000.00, 150000.00)",
            "INSERT INTO subscriptions (id, organization_id, plan_id, start_date, status) VALUES ('sub-a', 'org-a', 'plan-1', '2026-01-05 08:00', 'SUSPENDED')",
            "INSERT INTO domaines_ecoles (id, organization_id, hostname, statut, echecs_consecutifs, created_at) VALUES "
                + "('dom-a', 'org-a', 'ecole-a.test.invalid', 'ACTIF', 0, '2026-01-05 08:00')",
            "INSERT INTO demandes_transfert (id, eleve_id, ecole_source_id, ecole_destination_id, statut, cree_le, cree_par) VALUES "
                + "('dt-1', 'u-eleve-a', 'org-a', 'org-b', 'REFUSEE', '2026-02-10 10:00+00', 'u-admin-a')",
            "INSERT INTO annonces_ecole (id, organization_id, titre, message, nature, destinataires, destinataires_touches, envoyee_le) VALUES "
                + "('an-a', 'org-a', 'Rentrée', 'Bonne rentrée', 'ANNONCE', 'TOUTE_ECOLE', 3, '2026-01-06 08:00')",
            "INSERT INTO audit_logs (id, organization_id, action_type, action_timestamp, created_at) VALUES ('aud-a', 'org-a', 'UPDATE', localtimestamp, localtimestamp)"
        };
        for (String s : sql) jdbc.execute(s);
    }

    // ── Outils de comparaison ───────────────────────────────────────────────────

    /** Chaque table : ses lignes sous forme texte, triées. */
    private Map<String, List<String>> photo() {
        Map<String, List<String>> p = new LinkedHashMap<>();
        for (String t : jdbc.queryForList("SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY 1", String.class)) {
            if (HORS_COMPARAISON.contains(t)) continue;
            p.put(t, jdbc.queryForList("SELECT x::text FROM public.\"" + t + "\" x ORDER BY 1", String.class));
        }
        return p;
    }

    private Map<String, Object> ecole(String id) {
        return jdbc.queryForMap("SELECT name, slug, type, status, created_at, suspended_at, settings::text AS settings FROM organizations WHERE id = ?", id);
    }

    private long lignesDeA() {
        PlanArchivage plan = RegistreTables.plan(CatalogueBase.lire(jdbc));
        long n = 0;
        for (PlanArchivage.TablePlan t : plan.tables()) {
            n += jdbc.queryForObject("SELECT count(*) FROM public.\"" + t.nom() + "\" t WHERE " + t.selection(), Long.class,
                    t.valeurs("org-a"));
        }
        return n;
    }

    private ArchivageEcole archiverA(ArchivageEcolesService s) {
        ArchivageEcole a = s.archiverMaintenant("org-a", SLUG_A, "Test d'aller-retour", "sa-1", "Super Admin (test)");
        return archivages.parId(a.getId()).orElseThrow();
    }

    // ── Tests ───────────────────────────────────────────────────────────────────

    @Test
    void allerRetourIdentiqueLigneALigne() throws IOException {
        Map<String, List<String>> avant = photo();
        Map<String, Object> ecoleAAvant = ecole("org-a");
        Map<String, Object> ecoleBAvant = ecole("org-b");
        List<String> auditAvant = jdbc.queryForList("SELECT x::text FROM audit_logs x", String.class);
        ArchivageEcolesService s = service();

        ArchivageEcole a = archiverA(s);
        assertThat(a.getEtat()).as(a.getErreur()).isEqualTo(EtatArchivage.ARCHIVEE);
        assertThat(a.getEtape()).isEqualTo(EtapeArchivage.TERMINEE);
        assertThat(a.getRestes()).isEmpty();
        assertThat(a.getConservationJusquAu()).isAfter(LocalDateTime.now().plusDays(29));
        assertThat(a.getCompteurs()).containsEntry("attendances", 2L).containsEntry("group_chat_participants", 3L)
                .containsEntry("organization_users", 1L).containsEntry("role_permissions", 1L).containsEntry("reference_values", 1L)
                .doesNotContainKeys("users", "audit_logs", "domaines_ecoles", "demandes_transfert", "subscriptions", "annonces_ecole");
        assertThat(a.getNombreMedias()).isEqualTo(3);   // avatar, document, justificatif (la photo du compte reste avec users)
        assertThat(a.getVersionSchema()).isEqualTo("24");
        assertThat(ecole("org-a").get("status")).isEqualTo("ARCHIVED");

        // plus aucune donnée métier de A ; les comptes, l'audit, le domaine, le transfert et l'abonnement restent
        assertThat(lignesDeA()).isZero();
        assertThat(jdbc.queryForList("SELECT id FROM organization_users ORDER BY id", String.class)).containsExactly("ou-1", "ou-2", "ou-4", "ou-5");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Long.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM domaines_ecoles WHERE organization_id = 'org-a'", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE id = 'aud-a'", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reference_values WHERE id = 'rv-sys'", Long.class)).isEqualTo(1);
        // l'école B n'a pas bougé
        Map<String, List<String>> archivee = photo();
        for (String t : avant.keySet()) {
            long parti = a.getCompteurs().getOrDefault(t, 0L);
            assertThat(archivee.get(t)).as(t).hasSize(avant.get(t).size() - (int) parti);
            assertThat(avant.get(t)).as(t).containsAll(archivee.get(t));
        }
        assertThat(ecole("org-b")).isEqualTo(ecoleBAvant);
        // fichier chiffré : aucune donnée en clair
        Path fichier = local.dossier().resolve(a.getId() + ".e221a");
        String brut = new String(Files.readAllBytes(fichier), StandardCharsets.ISO_8859_1);
        assertThat(brut).doesNotContain("parent.a@test.invalid").doesNotContain("Rendez-vous");
        assertThat(a.getEmpreinteSha256()).hasSize(64);

        ArchivageEcole r = s.restaurer("org-a", SLUG_A, "sa-1", "Super Admin (test)");
        r = archivages.parId(r.getId()).orElseThrow();
        assertThat(r.getEtat()).as(r.getErreur()).isEqualTo(EtatArchivage.RESTAUREE);
        assertThat(r.getCleEnveloppee()).isNull();
        assertThat(r.isFichierASupprimer()).isFalse();
        assertThat(fichier).doesNotExist();
        // tout revient à l'ordre : chaque ligne de chaque table, à l'identique
        assertThat(photo()).isEqualTo(avant);
        Map<String, Object> ecoleAApres = ecole("org-a");
        assertThat(ecoleAApres.get("status")).isEqualTo("ACTIVE");
        assertThat(ecoleAApres.get("suspended_at")).isNull();
        for (String k : List.of("name", "slug", "type", "created_at", "settings")) {
            assertThat(ecoleAApres.get(k)).as(k).isEqualTo(ecoleAAvant.get(k));
        }
        // audit : rien de perdu, des étapes ajoutées ; journal de l'archivage complet
        assertThat(jdbc.queryForList("SELECT x::text FROM audit_logs x", String.class)).containsAll(auditAvant);
        assertThat(jdbc.queryForList("SELECT evenement FROM archivages_ecoles_journal WHERE archivage_id = ? ORDER BY id",
                String.class, a.getId())).containsSubsequence("ARCHIVAGE_DEMANDE", "ARCHIVAGE_EXPORTE", "ARCHIVAGE_VERIFIE",
                "ARCHIVAGE_PURGE", "ARCHIVAGE_TERMINE", "RESTAURATION_DEMANDEE", "RESTAURATION_TERMINEE", "FICHIER_SUPPRIME");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE organization_id = 'plateforme' AND action_type = 'ARCHIVAGE_TERMINE'",
                Long.class)).isEqualTo(1);
    }

    @Test
    void conflitALaRestaurationRefuseSansRienEcrire() {
        ArchivageEcolesService s = service();
        ArchivageEcole a = archiverA(s);
        // entre-temps, l'adresse du parent archivé est reprise dans l'école B (parents.email est unique sur la plateforme)
        jdbc.update("INSERT INTO parents (id, organization_id, email, nom) VALUES ('par-b2', 'org-b', 'parent.a@test.invalid', 'Test')");
        s.restaurer("org-a", SLUG_A, "sa-1", "Super Admin (test)");
        ArchivageEcole refusee = archivages.parId(a.getId()).orElseThrow();
        assertThat(refusee.getEtat()).isEqualTo(EtatArchivage.ARCHIVEE);
        assertThat(refusee.getErreur()).startsWith("Restauration refusée : 1 conflit");
        assertThat(refusee.getConflits()).singleElement().satisfies(c -> {
            assertThat(c.get("table")).isEqualTo("parents");
            assertThat(String.valueOf(c.get("valeurs"))).contains("email=parent.a@test.invalid");
        });
        assertThat(lignesDeA()).isZero();                                   // rien n'a été écrit
        assertThat(ecole("org-a").get("status")).isEqualTo("ARCHIVED");
        assertThat(refusee.getCleEnveloppee()).isNotNull();                // archive intacte

        jdbc.update("DELETE FROM parents WHERE id = 'par-b2'");
        s.restaurer("org-a", SLUG_A, "sa-1", "Super Admin (test)");
        assertThat(archivages.parId(a.getId()).orElseThrow().getEtat()).isEqualTo(EtatArchivage.RESTAUREE);
        assertThat(lignesDeA()).isEqualTo(a.getNombreLignes());
    }

    @Test
    void relectureNonConformeEchecSansRienSupprimerEtEcoleRedevenueSuspendue() {
        Map<String, List<String>> avant = photo();
        StockageArchive corrompu = new Delegue(local) {
            @Override
            public String ecrire(String nom, Path fichier) throws IOException {
                byte[] b = Files.readAllBytes(fichier);
                b[b.length / 2] ^= 1;   // le stockage abîme le fichier
                Path abime = Files.write(fichier.resolveSibling("abime"), b);
                return super.ecrire(nom, abime);
            }
        };
        LocalDateTime debut = LocalDateTime.now().minusSeconds(1);
        ArchivageEcole a = archiverA(service(corrompu, Runnable::run));
        assertThat(a.getEtat()).isEqualTo(EtatArchivage.ECHEC);
        assertThat(a.getErreur()).contains("empreinte SHA-256");
        assertThat(photo()).isEqualTo(avant);                               // rien n'a été supprimé
        Map<String, Object> ecoleA = ecole("org-a");
        assertThat(ecoleA.get("status")).isEqualTo("SUSPENDED");
        assertThat(((java.sql.Timestamp) ecoleA.get("suspended_at")).toLocalDateTime()).isAfter(debut);   // règle D1
        assertThat(local.existe(a.getId() + ".e221a")).isFalse();           // fichier abandonné supprimé
        assertThat(a.getCleEnveloppee()).isNull();
        // un nouvel essai reste possible
        assertThat(archiverA(service()).getEtat()).isEqualTo(EtatArchivage.ARCHIVEE);
    }

    @Test
    void purgeInterrompueRepriseJusquAuBoutPuisRestaurationIdentique() {
        Map<String, List<String>> avant = photo();
        // coupure simulée pendant la purge : la base refuse de supprimer dans « classes »
        jdbc.execute("CREATE FUNCTION coupure() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'coupure simulée'; END $$");
        jdbc.execute("CREATE TRIGGER tr_coupure BEFORE DELETE ON classes FOR EACH ROW EXECUTE FUNCTION coupure()");
        ArchivageEcolesService s = service();
        ArchivageEcole a = archiverA(s);
        assertThat(a.getEtat()).isEqualTo(EtatArchivage.ARCHIVAGE_EN_COURS);
        assertThat(a.getEtape()).isEqualTo(EtapeArchivage.PURGE);
        assertThat(a.getTentatives()).isEqualTo(1);
        assertThat(a.getErreur()).contains("coupure simulée");
        assertThat(a.getPurge()).isNotEmpty().doesNotContainKey("classes");
        assertThat(lignesDeA()).isPositive();
        // l'école reste fermée (ARCHIVED) : impossible de la réactiver au milieu d'une purge
        assertThat(ecole("org-a").get("status")).isEqualTo("ARCHIVED");

        jdbc.execute("DROP TRIGGER tr_coupure ON classes");
        s.reprendreOperationsInterrompues();                                // redémarrage
        ArchivageEcole reprise = archivages.parId(a.getId()).orElseThrow();
        assertThat(reprise.getEtat()).as(reprise.getErreur()).isEqualTo(EtatArchivage.ARCHIVEE);
        assertThat(lignesDeA()).isZero();
        s.restaurer("org-a", SLUG_A, "sa-1", "Super Admin (test)");
        assertThat(photo()).isEqualTo(avant);
    }

    @Test
    void suppressionDefinitiveSeulementApresEcheanceEtConfirmation() {
        ArchivageEcolesService s = service();
        ArchivageEcole a = archiverA(s);
        assertThatThrownBy(() -> s.supprimerDefinitivement("org-a", true, "sa-1", "Super Admin (test)"))
                .isInstanceOf(ArchivageException.class).hasMessageContaining("après la fin de la conservation");
        s.tacheQuotidienne();
        assertThat(archivages.parId(a.getId()).orElseThrow().getEtat()).isEqualTo(EtatArchivage.ARCHIVEE);

        // 31 jours plus tard : la tâche ne fait que passer « à confirmer », rien n'est effacé
        jdbc.update("UPDATE archivages_ecoles SET conservation_jusqu_au = localtimestamp - interval '1 day' WHERE id = ?", a.getId());
        s.tacheQuotidienne();
        ArchivageEcole aConfirmer = archivages.parId(a.getId()).orElseThrow();
        assertThat(aConfirmer.getEtat()).isEqualTo(EtatArchivage.SUPPRESSION_A_CONFIRMER);
        assertThat(local.existe(a.getId() + ".e221a")).isTrue();
        assertThat(aConfirmer.getCleEnveloppee()).isNotNull();

        assertThatThrownBy(() -> s.supprimerDefinitivement("org-a", false, "sa-1", "Super Admin (test)"))
                .isInstanceOf(ArchivageException.class).hasMessageContaining("confirmer");
        s.supprimerDefinitivement("org-a", true, "sa-1", "Super Admin (test)");
        ArchivageEcole supprimee = archivages.parId(a.getId()).orElseThrow();
        assertThat(supprimee.getEtat()).isEqualTo(EtatArchivage.SUPPRIMEE_DEFINITIVEMENT);
        assertThat(supprimee.getCleEnveloppee()).isNull();
        assertThat(supprimee.isFichierASupprimer()).isFalse();
        assertThat(local.existe(a.getId() + ".e221a")).isFalse();
        // trace minimale : école, dates, auteur, empreinte
        assertThat(supprimee.getEcoleNom()).isEqualTo("École A (test archivage)");
        assertThat(supprimee.getSupprimeePar()).isEqualTo("sa-1");
        assertThat(supprimee.getEmpreinteSha256()).isEqualTo(a.getEmpreinteSha256());
        assertThatThrownBy(() -> archiverA(s)).isInstanceOf(ArchivageException.class).hasMessageContaining("supprimée définitivement");
        assertThatThrownBy(() -> s.restaurer("org-a", SLUG_A, "sa-1", "x")).isInstanceOf(ArchivageException.class);
    }

    @Test
    void archivageAutomatiqueAuSeuilSeulement() {
        ArchivageEcolesService s = service();
        assertThat(s.ecolesAuDelaDuSeuil(30)).isEqualTo(1);                 // A (40 j) ; pas C (10 j)
        s.tacheQuotidienne();
        ArchivageEcole a = archivages.dernierDeLEcole("org-a").orElseThrow();
        assertThat(a.getEtat()).isEqualTo(EtatArchivage.ARCHIVEE);
        assertThat(a.getDeclenchement()).isEqualTo(Declenchement.AUTOMATIQUE);
        assertThat(archivages.dernierDeLEcole("org-c")).isEmpty();
        assertThat(ecole("org-c").get("status")).isEqualTo("SUSPENDED");
        assertThat(s.ecolesAuDelaDuSeuil(7)).isEqualTo(1);                  // aperçu : C avec un seuil de 7 jours
    }

    @Test
    void reglagesBornesEtTraces() {
        ReglagesArchivageService reglages = new ReglagesArchivageService(jdbc, new JournalAudit(jdbc));
        assertThat(reglages.lire().reglages().seuilJours()).isEqualTo(30);
        assertThat(reglages.lire().reglages().conservationJours()).isEqualTo(30);
        assertThatThrownBy(() -> reglages.modifier(400, null, "sa-1", "x")).isInstanceOf(ArchivageException.class)
                .hasMessage("Le seuil d'archivage doit être compris entre 7 et 365 jours.");
        assertThatThrownBy(() -> reglages.modifier(null, 10, "sa-1", "x")).hasMessageContaining("entre 15 et 365 jours");
        assertThatThrownBy(() -> reglages.modifier(null, null, "sa-1", "x")).isInstanceOf(ArchivageException.class);
        ReglagesArchivageService.Valeurs v = reglages.modifier(60, 90, "sa-1", "Super Admin (test)");
        assertThat(v.reglages().seuilJours()).isEqualTo(60);
        assertThat(v.reglages().conservationJours()).isEqualTo(90);
        assertThat(v.modifiePar()).isEqualTo("sa-1");
        // pas d'effet rétroactif : l'échéance d'une archive est rangée à sa création
        ArchivageEcole a = archiverA(service());
        assertThat(a.getConservationJours()).isEqualTo(90);
        reglages.modifier(null, 15, "sa-1", "x");
        assertThat(archivages.parId(a.getId()).orElseThrow().getConservationJours()).isEqualTo(90);
    }

    @Test
    void refusClairEnProductionSansStockageCloudNiCle() {
        StockageArchiveLocal disqueDeProduction = new StockageArchiveLocal(dossier.resolve("prod"), true, false);
        assertThatThrownBy(() -> service(disqueDeProduction, Runnable::run)
                .archiverMaintenant("org-a", SLUG_A, "Test", "sa-1", "x"))
                .isInstanceOf(ArchivageException.class).hasMessageContaining("Archivage refusé en production")
                .extracting(e -> ((ArchivageException) e).nature()).isEqualTo(ArchivageException.Nature.INDISPONIBLE);
        assertThat(ecole("org-a").get("status")).isEqualTo("SUSPENDED");
        assertThat(archivages.dernierDeLEcole("org-a")).isEmpty();
        assertThat(lignesDeA()).isPositive();
    }

    @Test
    void doubleClicEtEcoleActiveSansEffet() {
        List<Runnable> enAttente = new ArrayList<>();
        ArchivageEcolesService s = service(local, enAttente::add);           // l'opération reste « en cours »
        s.archiverMaintenant("org-a", SLUG_A, "Premier clic", "sa-1", "x");
        assertThatThrownBy(() -> s.archiverMaintenant("org-a", SLUG_A, "Second clic", "sa-1", "x"))
                .isInstanceOf(ArchivageException.class).hasMessage("Un archivage est déjà en cours pour cette école.");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM archivages_ecoles", Long.class)).isEqualTo(1);
        assertThatThrownBy(() -> s.archiverMaintenant("org-b", "ecole-b-test", "École active", "sa-1", "x"))
                .isInstanceOf(ArchivageException.class).hasMessageContaining("Seule une école suspendue");
        assertThatThrownBy(() -> s.archiverMaintenant("org-c", "mauvais-slug", "Test", "sa-1", "x"))
                .isInstanceOf(ArchivageException.class).hasMessageContaining("Confirmation refusée");
        enAttente.forEach(Runnable::run);
        assertThat(archivages.dernierDeLEcole("org-a").orElseThrow().getEtat()).isEqualTo(EtatArchivage.ARCHIVEE);
    }

    @Test
    void migrationsRejouablesEtIndexDEcolePresents() {
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();   // rien à refaire
        assertThat(jdbc.queryForObject("SELECT max(version::int) FROM flyway_schema_history WHERE success", Integer.class)).isEqualTo(24);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname LIKE 'idx\\_%\\_ecole' "
                + "AND indexname <> 'idx_permissions_roles_ecole'", Long.class)).isEqualTo(60);
        assertThat(jdbc.queryForList("SELECT indexdef FROM pg_indexes WHERE indexname = 'idx_attendances_ecole'", String.class))
                .singleElement().asString().contains("(organization_id, student_id)");
        assertThat(jdbc.queryForObject("SELECT valeur FROM parametres_plateforme WHERE cle = 'ecoles.archivage.delai_suspension'",
                String.class)).isEqualTo("30 days");
    }

    /** Stockage qui délègue au stockage local (pour simuler une panne ou une altération). */
    private static class Delegue implements StockageArchive {
        private final StockageArchive d;

        Delegue(StockageArchive d) {
            this.d = d;
        }

        @Override
        public String nom() {
            return d.nom();
        }

        @Override
        public void verifierPret() {
            d.verifierPret();
        }

        @Override
        public String ecrire(String nom, Path fichier) throws IOException {
            return d.ecrire(nom, fichier);
        }

        @Override
        public InputStream lire(String emplacement) throws IOException {
            return d.lire(emplacement);
        }

        @Override
        public void supprimer(String emplacement) throws IOException {
            d.supprimer(emplacement);
        }

        @Override
        public boolean existe(String emplacement) throws IOException {
            return d.existe(emplacement);
        }
    }
}
