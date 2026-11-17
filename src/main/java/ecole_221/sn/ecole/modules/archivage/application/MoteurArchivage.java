package ecole_221.sn.ecole.modules.archivage.application;

import ecole_221.sn.ecole.modules.archivage.application.PlanArchivage.TablePlan;
import ecole_221.sn.ecole.modules.archivage.application.format.ChiffrementFlux;
import ecole_221.sn.ecole.modules.archivage.application.format.EcritureArchive;
import ecole_221.sn.ecole.modules.archivage.application.format.LectureArchive;
import ecole_221.sn.ecole.modules.archivage.application.format.TableArchivee;
import ecole_221.sn.ecole.modules.archivage.domain.exception.ArchivageException;
import ecole_221.sn.ecole.modules.archivage.domain.model.ArchivageEcole;
import ecole_221.sn.ecole.modules.archivage.domain.model.EtapeArchivage;
import ecole_221.sn.ecole.modules.archivage.domain.model.EtatArchivage;
import ecole_221.sn.ecole.modules.archivage.domain.port.ArchivagesEcolesPort;
import ecole_221.sn.ecole.modules.archivage.domain.port.CleDesArchives;
import ecole_221.sn.ecole.modules.archivage.domain.port.EcolesArchivablesPort;
import ecole_221.sn.ecole.modules.archivage.domain.port.StockageArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static ecole_221.sn.ecole.modules.archivage.application.RegistreTables.ident;

/**
 * Moteur d'archivage d'une école (étude d'archivage, §§ 4.3 et 4.4 ; décisions du Tech Lead du 16/11).
 *
 * <ol>
 *   <li>EXPORT : instantané REPEATABLE READ en lecture seule ; chaque table qui part est écrite en JSON (noms de
 *       colonnes, valeurs texte PostgreSQL), compressée GZIP, chiffrée AES-256-GCM, puis déposée dans le stockage ;
 *       nombre de lignes comparé au COUNT(*) du même instantané.</li>
 *   <li>VÉRIFICATION : RELECTURE depuis le stockage, empreinte SHA-256 du fichier, déchiffrement, empreinte et nombre
 *       de lignes de chaque table. Le moindre écart : échec, rien n'est supprimé, l'école redevient suspendue.</li>
 *   <li>PURGE (point de non-retour) : enfants d'abord, par lots, en supprimant UNIQUEMENT les lignes présentes dans
 *       l'archive vérifiée (par clé primaire) et seulement tant que l'école est ARCHIVED. Une coupure se reprend :
 *       les tables déjà purgées sont notées, supprimer deux fois la même clé ne fait rien.</li>
 *   <li>FIN : lignes restantes comptées (écrites après le gel, jamais supprimées sans archive), échéance de
 *       conservation calculée et rangée, état ARCHIVEE.</li>
 * </ol>
 */
public class MoteurArchivage {

    private static final Logger log = LoggerFactory.getLogger(MoteurArchivage.class);
    private static final String GARDE = "EXISTS (SELECT 1 FROM public.organizations o WHERE o.id = ? AND o.status = 'ARCHIVED')";
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TransactionsArchivage tx;
    private final ArchivagesEcolesPort archivages;
    private final EcolesArchivablesPort ecoles;
    private final StockageArchive stockage;
    private final CleDesArchives cles;
    private final JournalArchivage journal;
    private final ReglagesArchivageService reglages;
    private final FichiersArchive fichiers;
    private final int tailleLot;
    private Supplier<LocalDateTime> horloge = LocalDateTime::now;

    public MoteurArchivage(TransactionsArchivage tx, ArchivagesEcolesPort archivages, EcolesArchivablesPort ecoles,
                           StockageArchive stockage, CleDesArchives cles, JournalArchivage journal,
                           ReglagesArchivageService reglages, FichiersArchive fichiers, int tailleLot) {
        this.tx = tx;
        this.archivages = archivages;
        this.ecoles = ecoles;
        this.stockage = stockage;
        this.cles = cles;
        this.journal = journal;
        this.reglages = reglages;
        this.fichiers = fichiers;
        this.tailleLot = Math.max(1, tailleLot);
    }

    void horloge(Supplier<LocalDateTime> h) {
        this.horloge = h;
    }

    private LocalDateTime maintenant() {
        return horloge.get();
    }

    /** Exécute l'archivage jusqu'au bout, ou le reprend là où il s'est arrêté. */
    public void executer(String archivageId) {
        ArchivageEcole a = archivages.parId(archivageId).orElse(null);
        if (a == null || a.getEtat() != EtatArchivage.ARCHIVAGE_EN_COURS) {
            return;
        }
        try {
            if (a.getEtape().avantPurge()) {
                exporter(a);
                verifier(a);
            }
            purger(a);
            terminer(a);
        } catch (IOException | RuntimeException e) {
            gererEchec(a, e);
        }
    }

    // ── 1. Export ───────────────────────────────────────────────────────────────

    void exporter(ArchivageEcole a) throws IOException {
        a.avancer(EtapeArchivage.EXPORT, maintenant());
        archivages.enregistrer(a);
        PlanArchivage plan = RegistreTables.plan(CatalogueBase.lire(tx.jdbc()));
        CleDesArchives.CleMaitresse cleMaitresse = cles.obtenir();
        byte[] cleDonnees = ChiffrementFlux.nouvelleCle();
        Path fichier = fichiers.temporaire("archive-");
        MessageDigest md = FichiersArchive.sha256();
        Map<String, Long> compteurs = new LinkedHashMap<>();
        Set<Map<String, String>> medias = new LinkedHashSet<>();
        String[] version = {null};
        try {
            try (OutputStream sortie = new DigestOutputStream(new BufferedOutputStream(Files.newOutputStream(fichier)), md);
                 EcritureArchive ecriture = new EcritureArchive(ChiffrementFlux.chiffrer(sortie, cleDonnees))) {
                tx.instantane().executeWithoutResult(s -> {
                    tx.reglerFormatsTexte();
                    version[0] = versionSchema();
                    try {
                        ecrireTables(ecriture, a, plan, version[0], compteurs, medias);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
            long taille = Files.size(fichier);
            String empreinte = HexFormat.of().formatHex(md.digest());
            String emplacement = stockage.ecrire(a.getId(), fichier);
            long total = compteurs.values().stream().mapToLong(Long::longValue).sum();
            a.enregistrerFichier(stockage.nom(), emplacement, empreinte, taille, version[0],
                    ChiffrementFlux.envelopper(cleMaitresse.octets(), cleDonnees, a.getId()), cleMaitresse.identifiant(),
                    compteurs, total, medias.size(), maintenant());
            archivages.enregistrer(a);
            journal.noter(a, "ARCHIVAGE_EXPORTE", null, null, total + " lignes de " + compteurs.size() + " tables, "
                    + medias.size() + " adresses de médias ; " + taille + " octets chiffrés (" + stockage.nom() + ").");
        } finally {
            Arrays.fill(cleDonnees, (byte) 0);
            Files.deleteIfExists(fichier);
        }
    }

    private String versionSchema() {
        Boolean flyway = tx.jdbc().queryForObject("SELECT to_regclass('public.flyway_schema_history') IS NOT NULL", Boolean.class);
        if (!Boolean.TRUE.equals(flyway)) return null;
        List<String> v = tx.jdbc().queryForList("SELECT version FROM flyway_schema_history WHERE success AND version IS NOT NULL "
                + "ORDER BY installed_rank DESC LIMIT 1", String.class);
        return v.isEmpty() ? null : v.get(0);
    }

    private void ecrireTables(EcritureArchive ecriture, ArchivageEcole a, PlanArchivage plan, String version,
                              Map<String, Long> compteurs, Set<Map<String, String>> medias) throws IOException {
        Map<String, Object> entete = new LinkedHashMap<>();
        entete.put("archivageId", a.getId());
        Map<String, Object> ecole = new LinkedHashMap<>();
        ecole.put("id", a.getOrganizationId());
        ecole.put("nom", a.getEcoleNom());
        ecole.put("slug", a.getEcoleSlug());
        entete.put("ecole", ecole);
        entete.put("creeLe", maintenant().toString());
        entete.put("versionSchema", version);
        entete.put("declenchement", a.getDeclenchement().name());
        entete.put("tablesGardees", plan.gardees());
        ecriture.entete(entete);
        for (TablePlan t : plan.tables()) {
            Object[] parametres = t.valeurs(a.getOrganizationId());
            Long attendu = tx.jdbc().queryForObject("SELECT count(*) FROM public." + ident(t.nom()) + " t WHERE " + t.selection(),
                    Long.class, parametres);
            ecriture.debutTable(t.description());
            int largeur = t.colonnes().size();
            List<Integer> colonnesMedias = t.colonnesMedias().stream().map(c -> t.description().indexDe(c)).toList();
            String sql = "SELECT " + t.colonnes().stream().map(c -> "t." + ident(c.nom()) + "::text").collect(Collectors.joining(", "))
                    + " FROM public." + ident(t.nom()) + " t WHERE " + t.selection()
                    + (t.cle().isEmpty() ? "" : " ORDER BY " + t.cle().stream().map(k -> "t." + ident(k)).collect(Collectors.joining(", ")));
            tx.jdbc().query(con -> {
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setFetchSize(500);
                for (int i = 0; i < parametres.length; i++) ps.setObject(i + 1, parametres[i]);
                return ps;
            }, (RowCallbackHandler) rs -> {
                String[] valeurs = new String[largeur];
                for (int i = 0; i < largeur; i++) valeurs[i] = rs.getString(i + 1);
                try {
                    ecriture.ligne(valeurs);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                for (int i : colonnesMedias) {
                    String v = valeurs[i];
                    if (v != null && (v.startsWith("http://") || v.startsWith("https://"))) {
                        Map<String, String> m = new LinkedHashMap<>();
                        m.put("table", t.nom());
                        m.put("colonne", t.colonnes().get(i).nom());
                        m.put("url", v);
                        medias.add(m);
                    }
                }
            });
            EcritureArchive.ResumeTable resume = ecriture.finTable();
            if (attendu == null || resume.lignes() != attendu) {
                throw ArchivageException.refus("Export incohérent pour la table " + t.nom() + ".");
            }
            compteurs.put(t.nom(), resume.lignes());
        }
        Map<String, Object> resume = new LinkedHashMap<>();
        resume.put("tables", compteurs.size());
        resume.put("lignes", compteurs.values().stream().mapToLong(Long::longValue).sum());
        resume.put("medias", medias.size());
        ecriture.fin(medias, resume);
    }

    // ── 2. Vérification ─────────────────────────────────────────────────────────

    void verifier(ArchivageEcole a) throws IOException {
        a.avancer(EtapeArchivage.VERIFICATION, maintenant());
        archivages.enregistrer(a);
        Map<String, Long> relus = new LinkedHashMap<>();
        lire(a, new LectureArchive.Visiteur() {
            @Override
            public void entete(Map<String, Object> entete) {
                verifierEntete(a, entete);
            }

            @Override
            public void ligne(TableArchivee table, String[] valeurs) {
            }

            @Override
            public void finTable(TableArchivee table, long lignes, String empreinte) {
                relus.put(table.nom(), lignes);
            }
        });
        if (!relus.equals(a.getCompteurs())) {
            throw ArchivageException.refus("Archive relue incomplète : comptes différents de l'instantané.");
        }
        journal.noter(a, "ARCHIVAGE_VERIFIE", null, null, "Relecture conforme (empreinte SHA-256, déchiffrement, "
                + relus.size() + " tables comptées). Début de la purge.");
    }

    static void verifierEntete(ArchivageEcole a, Map<String, Object> entete) {
        Object ecole = entete.get("ecole");
        if (!a.getId().equals(entete.get("archivageId"))
                || !(ecole instanceof Map<?, ?> m) || !a.getOrganizationId().equals(m.get("id"))) {
            throw ArchivageException.refus("Ce fichier n'est pas l'archive de cette école.");
        }
    }

    /** Relit l'archive depuis le stockage (empreinte vérifiée), la déchiffre et la parcourt. */
    void lire(ArchivageEcole a, LectureArchive.Visiteur visiteur) throws IOException {
        CleDesArchives.CleMaitresse cleMaitresse = cles.obtenir();
        if (a.getCleEnveloppee() == null) {
            throw ArchivageException.refus("Clé de l'archive effacée : archive illisible.");
        }
        if (a.getCleId() != null && !a.getCleId().equals(cleMaitresse.identifiant())) {
            throw ArchivageException.refus("Clé des archives différente de celle de l'archivage (ARCHIVES_CLE a changé).");
        }
        byte[] cle = ChiffrementFlux.desenvelopper(cleMaitresse.octets(), a.getCleEnveloppee(), a.getId());
        Path relu = fichiers.relireVerifie(stockage, a);
        try (InputStream in = ChiffrementFlux.dechiffrer(new BufferedInputStream(Files.newInputStream(relu)), cle)) {
            LectureArchive.lire(in, visiteur);
        } finally {
            Arrays.fill(cle, (byte) 0);
            Files.deleteIfExists(relu);
        }
    }

    // ── 3. Purge ────────────────────────────────────────────────────────────────

    void purger(ArchivageEcole a) throws IOException {
        if (a.getEtape() != EtapeArchivage.PURGE) {
            String statut = ecoles.lire(a.getOrganizationId()).map(EcolesArchivablesPort.EcoleArchivable::statut).orElse(null);
            if (!"ARCHIVED".equals(statut)) {
                throw ArchivageException.conflit("L'école a changé de statut pendant l'archivage : rien n'a été supprimé.");
            }
            a.commencerPurge(maintenant());
            archivages.enregistrer(a);
        }
        Map<String, TablePlan> plan = new LinkedHashMap<>();
        for (TablePlan t : RegistreTables.plan(CatalogueBase.lire(tx.jdbc())).tables()) plan.put(t.nom(), t);
        long[] total = {0};
        lire(a, new LectureArchive.Visiteur() {
            private PurgeTable courante;

            @Override
            public void debutTable(TableArchivee table) {
                courante = a.tablePurgee(table.nom()) || !plan.containsKey(table.nom()) ? null
                        : new PurgeTable(plan.get(table.nom()), table, a.getOrganizationId());
            }

            @Override
            public void ligne(TableArchivee table, String[] valeurs) {
                if (courante != null) courante.ajouter(valeurs);
            }

            @Override
            public void finTable(TableArchivee table, long lignes, String empreinte) {
                if (courante == null) return;
                long supprimees = courante.terminer();
                total[0] += supprimees;
                a.noterTablePurgee(table.nom(), supprimees, maintenant());
                archivages.enregistrer(a);
                courante = null;
            }
        });
        journal.noter(a, "ARCHIVAGE_PURGE", null, null, total[0] + " lignes retirées de la base (présentes dans l'archive vérifiée).");
    }

    /** Suppression par lots des lignes d'une table, d'après les lignes lues dans l'archive. */
    private final class PurgeTable {
        private final TablePlan plan;
        private final String organizationId;
        private final List<String[]> lot = new ArrayList<>();
        private final int[] positionsCle;
        private final boolean parCle;
        private final String sql;
        private long supprimees;

        PurgeTable(TablePlan plan, TableArchivee archivee, String organizationId) {
            this.plan = plan;
            this.organizationId = organizationId;
            String table = "public." + ident(plan.nom());
            List<String> cle = archivee.cle().isEmpty() ? archivee.nomsColonnes() : archivee.cle();
            this.positionsCle = cle.stream().mapToInt(archivee::indexDe).toArray();
            this.parCle = archivee.cle().size() == 1 && plan.cle().equals(archivee.cle());
            if (parCle) {
                String k = archivee.cle().get(0);
                this.sql = "DELETE FROM " + table + " t WHERE " + plan.selection() + " AND t." + ident(k) + " = ANY(CAST(? AS "
                        + plan.typesCle().get(k) + "[])) AND " + GARDE;
            } else {
                // clé composée ou absente : ligne par ligne, comparaison texte de la clé (ou de toute la ligne)
                String egalites = cle.stream().map(c -> "t." + ident(c) + "::text IS NOT DISTINCT FROM ?")
                        .collect(Collectors.joining(" AND "));
                this.sql = "DELETE FROM " + table + " WHERE ctid = (SELECT t.ctid FROM " + table + " t WHERE "
                        + plan.selection() + " AND " + egalites + " LIMIT 1) AND " + GARDE;
            }
        }

        void ajouter(String[] valeurs) {
            lot.add(valeurs);
            if (lot.size() >= tailleLot) vider();
        }

        long terminer() {
            vider();
            return supprimees;
        }

        private void vider() {
            if (lot.isEmpty()) return;
            Object[] selection = plan.valeurs(organizationId);
            Integer n = tx.ecriture().execute(s -> {
                tx.reglerFormatsTexte();
                return tx.jdbc().execute((java.sql.Connection con) -> {
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    if (parCle) {
                        int i = 1;
                        for (Object o : selection) ps.setObject(i++, o);
                        ps.setArray(i++, con.createArrayOf("text", lot.stream().map(v -> v[positionsCle[0]]).toArray()));
                        ps.setString(i, organizationId);
                        return ps.executeUpdate();
                    }
                    for (String[] v : lot) {
                        int i = 1;
                        for (Object o : selection) ps.setObject(i++, o);
                        for (int p : positionsCle) ps.setString(i++, v[p]);
                        ps.setString(i, organizationId);
                        ps.addBatch();
                    }
                    int somme = 0;
                    for (int r : ps.executeBatch()) somme += Math.max(r, 0);
                    return somme;
                }
                });
            });
            supprimees += n == null ? 0 : n;
            lot.clear();
        }
    }

    // ── 4. Fin ──────────────────────────────────────────────────────────────────

    void terminer(ArchivageEcole a) {
        Map<String, Long> restes = new LinkedHashMap<>();
        for (TablePlan t : RegistreTables.plan(CatalogueBase.lire(tx.jdbc())).tables()) {
            Long n = tx.jdbc().queryForObject("SELECT count(*) FROM public." + ident(t.nom()) + " t WHERE " + t.selection(),
                    Long.class, t.valeurs(a.getOrganizationId()));
            if (n != null && n > 0) restes.put(t.nom(), n);
        }
        int conservation = reglages.lire().reglages().conservationJours();
        a.terminerArchivage(conservation, restes, maintenant());
        archivages.enregistrer(a);
        journal.noter(a, "ARCHIVAGE_TERMINE", null, null, "École archivée ; conservation jusqu'au "
                + a.getConservationJusquAu().format(JOUR) + " (" + conservation + " jours)."
                + (restes.isEmpty() ? "" : " Lignes écrites après le gel, restées en base : " + restes + "."));
        if (!restes.isEmpty()) {
            log.warn("Archivage {} : lignes écrites après le gel, restées en base : {}", a.getId(), restes);
        }
    }

    // ── Échec ───────────────────────────────────────────────────────────────────

    private void gererEchec(ArchivageEcole a, Exception e) {
        String cause = cause(e);
        try {
            ArchivageEcole courant = archivages.parId(a.getId()).orElse(a);
            if (courant.getEtat() != EtatArchivage.ARCHIVAGE_EN_COURS) return;
            if (courant.getEtape() != null && courant.getEtape().avantPurge()) {
                courant.echouerAvantPurge(cause, maintenant());
                archivages.enregistrer(courant);
                if (courant.renduSuspendue() && !ecoles.rendreSuspendue(courant.getOrganizationId())) {
                    log.warn("Archivage {} : statut de l'école changé entre-temps, laissé tel quel", courant.getId());
                }
                journal.noter(courant, "ARCHIVAGE_ECHEC", null, null, "Rien n'a été supprimé. " + cause);
                supprimerFichierAbandonne(courant);
                log.warn("Archivage {} de l'école {} échoué avant la purge : {}", courant.getId(), courant.getOrganizationId(), cause);
            } else {
                courant.noterIncidentPurge(cause, maintenant());
                archivages.enregistrer(courant);
                journal.noter(courant, "ARCHIVAGE_INCIDENT", null, null, "Purge interrompue, reprise prévue : " + cause);
                log.warn("Archivage {} : purge interrompue ({}), reprise prévue", courant.getId(), cause);
            }
        } catch (RuntimeException ex) {
            log.error("Archivage {} : échec non enregistré ({}) après « {} »", a.getId(), ex.getMessage(), cause);
        }
    }

    private void supprimerFichierAbandonne(ArchivageEcole a) {
        if (!a.isFichierASupprimer() || a.getEmplacement() == null) return;
        try {
            stockage.supprimer(a.getEmplacement());
            a.fichierSupprime(maintenant());
            archivages.enregistrer(a);
        } catch (IOException | RuntimeException ex) {
            log.warn("Archivage {} : fichier abandonné non supprimé ({}), nouvel essai chaque nuit", a.getId(), ex.getMessage());
        }
    }

    /**
     * Message court. Pour une erreur technique : la classe et la PREMIÈRE ligne du message de la cause racine (le
     * « Detail » de PostgreSQL, qui peut citer des valeurs comme une adresse e-mail, n'est jamais repris).
     */
    static String cause(Throwable e) {
        Throwable racine = e;
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ArchivageException ae) return ae.getMessage();
            racine = t;
        }
        String m = racine.getMessage() == null ? "" : racine.getMessage().split("\\R", 2)[0];
        if (m.length() > 300) m = m.substring(0, 300);
        return "Erreur technique (" + racine.getClass().getSimpleName() + ")" + (m.isBlank() ? "." : " : " + m);
    }
}
