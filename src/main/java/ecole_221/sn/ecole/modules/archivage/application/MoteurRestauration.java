package ecole_221.sn.ecole.modules.archivage.application;

import ecole_221.sn.ecole.modules.archivage.application.format.ChiffrementFlux;
import ecole_221.sn.ecole.modules.archivage.application.format.LectureArchive;
import ecole_221.sn.ecole.modules.archivage.application.format.TableArchivee;
import ecole_221.sn.ecole.modules.archivage.domain.exception.ArchivageException;
import ecole_221.sn.ecole.modules.archivage.domain.model.ArchivageEcole;
import ecole_221.sn.ecole.modules.archivage.domain.model.EtatArchivage;
import ecole_221.sn.ecole.modules.archivage.domain.port.ArchivagesEcolesPort;
import ecole_221.sn.ecole.modules.archivage.domain.port.CleDesArchives;
import ecole_221.sn.ecole.modules.archivage.domain.port.EcolesArchivablesPort;
import ecole_221.sn.ecole.modules.archivage.domain.port.StockageArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static ecole_221.sn.ecole.modules.archivage.application.RegistreTables.ident;

/**
 * Restauration d'une école archivée (décision du Tech Lead : « tout revient à l'ordre ») — EN UNE SEULE TRANSACTION.
 *
 * <ol>
 *   <li>Relecture : empreinte SHA-256 vérifiée AVANT le déchiffrement.</li>
 *   <li>Schéma : chargement PAR NOMS DE COLONNES dans des tables temporaires. Colonne ajoutée depuis l'archivage :
 *       sa valeur par défaut ; colonne ou table disparue, colonne obligatoire ajoutée sans défaut : refus explicite.</li>
 *   <li>Conflits (identifiants, e-mails, téléphones… repris entre-temps) : chaque index unique de la base est comparé
 *       aux lignes de l'archive ; le moindre conflit : refus qui LISTE les conflits, rien n'est écrit.</li>
 *   <li>Réinsertion, parents d'abord, comptes vérifiés ; l'école redevient ACTIVE, suspended_at effacé ; état
 *       RESTAUREE — tout dans la même transaction. Le fichier d'archive n'est supprimé qu'APRÈS la validation.</li>
 * </ol>
 * Un échec ou un refus ramène l'archive (intacte) à son état d'avant.
 */
public class MoteurRestauration {

    private static final Logger log = LoggerFactory.getLogger(MoteurRestauration.class);
    private static final int LOT = 500;
    private static final int CONFLITS_MAX = 100;

    private final DataSource source;
    private final TransactionsArchivage tx;
    private final ArchivagesEcolesPort archivages;
    private final EcolesArchivablesPort ecoles;
    private final StockageArchive stockage;
    private final CleDesArchives cles;
    private final JournalArchivage journal;
    private final FichiersArchive fichiers;
    private Supplier<LocalDateTime> horloge = LocalDateTime::now;

    public MoteurRestauration(DataSource source, TransactionsArchivage tx, ArchivagesEcolesPort archivages,
                              EcolesArchivablesPort ecoles, StockageArchive stockage, CleDesArchives cles,
                              JournalArchivage journal, FichiersArchive fichiers) {
        this.source = source;
        this.tx = tx;
        this.archivages = archivages;
        this.ecoles = ecoles;
        this.stockage = stockage;
        this.cles = cles;
        this.journal = journal;
        this.fichiers = fichiers;
    }

    void horloge(Supplier<LocalDateTime> h) {
        this.horloge = h;
    }

    public void executer(String archivageId) {
        ArchivageEcole a = archivages.parId(archivageId).orElse(null);
        if (a == null || a.getEtat() != EtatArchivage.RESTAURATION_EN_COURS) {
            return;
        }
        Path relu = null;
        byte[] cle = null;
        try {
            CleDesArchives.CleMaitresse cleMaitresse = cles.obtenir();
            if (a.getCleEnveloppee() == null) {
                throw ArchivageException.refus("Clé de l'archive effacée : archive illisible.");
            }
            if (a.getCleId() != null && !a.getCleId().equals(cleMaitresse.identifiant())) {
                throw ArchivageException.refus("Clé des archives différente de celle de l'archivage (ARCHIVES_CLE a changé).");
            }
            relu = fichiers.relireVerifie(stockage, a);           // empreinte AVANT déchiffrement
            cle = ChiffrementFlux.desenvelopper(cleMaitresse.octets(), a.getCleEnveloppee(), a.getId());
            CatalogueBase catalogue = CatalogueBase.lire(tx.jdbc());
            Path fichier = relu;
            byte[] cleDonnees = cle;
            Map<String, Long> inseres = tx.ecriture().execute(s -> {
                try {
                    return restaurer(a, fichier, cleDonnees, catalogue);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            long total = inseres == null ? 0 : inseres.values().stream().mapToLong(Long::longValue).sum();
            // trace écrite APRÈS la validation : une panne du journal ne peut pas annuler la restauration
            journal.noter(archivages.parId(a.getId()).orElse(a), "RESTAURATION_TERMINEE", a.getRestaurationDemandeePar(), null,
                    total + " lignes réinsérées dans " + (inseres == null ? 0 : inseres.size()) + " tables ; école de nouveau active.");
            log.info("Restauration {} : école {} rétablie ({} lignes)", a.getId(), a.getOrganizationId(), total);
            supprimerFichier(a);
        } catch (IOException | RuntimeException e) {
            echec(a, e);
        } finally {
            if (cle != null) Arrays.fill(cle, (byte) 0);
            if (relu != null) {
                try {
                    Files.deleteIfExists(relu);
                } catch (IOException ignore) {
                    // fichier temporaire : le système le nettoiera
                }
            }
        }
    }

    /** Dans la transaction : tables temporaires, contrôles, réinsertion, école ACTIVE, archive RESTAUREE. */
    private Map<String, Long> restaurer(ArchivageEcole a, Path fichier, byte[] cle, CatalogueBase catalogue) throws IOException {
        tx.reglerFormatsTexte();
        String statut = tx.jdbc().queryForList("SELECT status FROM organizations WHERE id = ? FOR UPDATE", String.class,
                a.getOrganizationId()).stream().findFirst().orElse(null);
        if (!"ARCHIVED".equals(statut)) {
            throw ArchivageException.conflit("Restauration impossible : l'école n'est plus archivée.");
        }
        Chargement chargement = new Chargement(catalogue);
        try (InputStream in = ChiffrementFlux.dechiffrer(new BufferedInputStream(Files.newInputStream(fichier)), cle)) {
            LectureArchive.lire(in, chargement);
        }
        if (!chargement.problemes.isEmpty()) {
            throw ArchivageException.refus("Restauration impossible, schéma incompatible : " + String.join(" ; ", chargement.problemes) + ".");
        }
        if (!chargement.lignes.equals(a.getCompteurs())) {
            throw ArchivageException.refus("Archive incomplète : comptes différents de ceux de l'archivage.");
        }
        List<Map<String, Object>> conflits = conflits(chargement, catalogue);
        if (!conflits.isEmpty()) {
            throw new ArchivageException(ArchivageException.Nature.CONFLIT,
                    "Restauration refusée : " + conflits.size() + " conflit(s) avec des données créées depuis l'archivage.",
                    Map.of("conflits", conflits));
        }
        // parents d'abord : l'archive est rangée enfants d'abord
        List<String> ordre = new ArrayList<>(chargement.temporaires.keySet());
        java.util.Collections.reverse(ordre);
        Map<String, Long> inseres = new LinkedHashMap<>();
        for (String table : ordre) {
            String colonnes = chargement.colonnes.get(table).stream().map(RegistreTables::ident).collect(Collectors.joining(", "));
            int n = tx.jdbc().update("INSERT INTO public." + ident(table) + " (" + colonnes + ") SELECT " + colonnes + " FROM "
                    + chargement.temporaires.get(table));
            if (n != chargement.lignes.get(table)) {
                throw ArchivageException.refus("Réinsertion incomplète dans " + table + " : rien n'est restauré.");
            }
            inseres.put(table, (long) n);
        }
        if (!ecoles.restaurer(a.getOrganizationId())) {
            throw ArchivageException.conflit("Restauration impossible : l'école a changé de statut.");
        }
        a.terminerRestauration(horloge.get());
        archivages.enregistrer(a);
        return inseres;
    }

    /** Charge chaque table de l'archive dans une table temporaire, par noms de colonnes, avec les types actuels. */
    private final class Chargement implements LectureArchive.Visiteur {
        private final CatalogueBase catalogue;
        final List<String> problemes = new ArrayList<>();
        final Map<String, String> temporaires = new LinkedHashMap<>();
        final Map<String, List<String>> colonnes = new LinkedHashMap<>();
        final Map<String, Long> lignes = new LinkedHashMap<>();
        private PreparedStatement insertion;
        private int enAttente;
        private long lues;
        private boolean ignorer;

        Chargement(CatalogueBase catalogue) {
            this.catalogue = catalogue;
        }

        @Override
        public void debutTable(TableArchivee t) throws IOException {
            lues = 0;
            ignorer = !compatible(t);
            if (ignorer) return;
            String temporaire = "archivage_r" + (temporaires.size() + 1);
            List<String> noms = t.nomsColonnes();
            String liste = noms.stream().map(RegistreTables::ident).collect(Collectors.joining(", "));
            tx.jdbc().execute("CREATE TEMP TABLE " + temporaire + " ON COMMIT DROP AS SELECT " + liste + " FROM public."
                    + ident(t.nom()) + " WITH NO DATA");
            String valeurs = noms.stream().map(c -> "CAST(? AS " + catalogue.colonne(t.nom(), c).type() + ")")
                    .collect(Collectors.joining(", "));
            try {
                Connection con = DataSourceUtils.getConnection(source);
                insertion = con.prepareStatement("INSERT INTO " + temporaire + " (" + liste + ") VALUES (" + valeurs + ")");
            } catch (SQLException e) {
                throw new IOException("Préparation de la restauration impossible", e);
            }
            temporaires.put(t.nom(), temporaire);
            colonnes.put(t.nom(), noms);
        }

        /** Colonnes de l'archive toutes présentes ; aucune colonne obligatoire sans défaut ajoutée depuis. */
        private boolean compatible(TableArchivee t) {
            if (!catalogue.existe(t.nom())) {
                problemes.add("table " + t.nom() + " disparue");
                return false;
            }
            Set<String> archivees = new HashSet<>(t.nomsColonnes());
            List<String> disparues = t.nomsColonnes().stream().filter(c -> catalogue.colonne(t.nom(), c) == null
                    || catalogue.colonne(t.nom(), c).generee()).toList();
            List<String> obligatoires = catalogue.colonnes().get(t.nom()).stream()
                    .filter(c -> c.obligatoire() && !c.aUnDefaut() && !c.generee() && !archivees.contains(c.nom()))
                    .map(CatalogueBase.Colonne::nom).toList();
            if (!disparues.isEmpty()) problemes.add("colonne(s) disparue(s) dans " + t.nom() + " : " + String.join(", ", disparues));
            if (!obligatoires.isEmpty()) problemes.add("colonne(s) obligatoire(s) sans défaut ajoutée(s) dans " + t.nom() + " : "
                    + String.join(", ", obligatoires));
            return disparues.isEmpty() && obligatoires.isEmpty();
        }

        @Override
        public void ligne(TableArchivee t, String[] valeurs) throws IOException {
            lues++;
            if (ignorer) return;
            try {
                for (int i = 0; i < valeurs.length; i++) insertion.setString(i + 1, valeurs[i]);
                insertion.addBatch();
                if (++enAttente >= LOT) envoyer(t);
            } catch (SQLException e) {
                throw valeurRefusee(t, e);
            }
        }

        private void envoyer(TableArchivee t) throws IOException {
            if (enAttente == 0) return;
            try {
                insertion.executeBatch();
                enAttente = 0;
            } catch (SQLException e) {
                throw valeurRefusee(t, e);
            }
        }

        private ArchivageException valeurRefusee(TableArchivee t, SQLException e) {
            return ArchivageException.refus("Restauration impossible : valeur refusée par le schéma actuel dans " + t.nom()
                    + " (" + MoteurArchivage.cause(e) + ").");
        }

        @Override
        public void finTable(TableArchivee t, long nombre, String empreinte) throws IOException {
            lignes.put(t.nom(), lues);
            if (ignorer) return;
            envoyer(t);
            try {
                insertion.close();
            } catch (SQLException e) {
                throw new IOException(e);
            }
            insertion = null;
        }
    }

    /** Conflits : index uniques (clé primaire comprise) et comptes disparus pour les appartenances. */
    private List<Map<String, Object>> conflits(Chargement c, CatalogueBase catalogue) {
        List<Map<String, Object>> conflits = new ArrayList<>();
        for (Map.Entry<String, String> e : c.temporaires.entrySet()) {
            String table = e.getKey();
            Set<String> archivees = new HashSet<>(c.colonnes.get(table));
            for (CatalogueBase.IndexUnique u : catalogue.indexUniques().getOrDefault(table, List.of())) {
                if (u.expression() || u.colonnes().isEmpty() || !archivees.containsAll(u.colonnes())) continue;
                String jointure = u.colonnes().stream().map(k -> "a." + ident(k) + " = b." + ident(k)).collect(Collectors.joining(" AND "));
                String valeurs = u.colonnes().stream().map(k -> "'" + k + "=' || coalesce(a." + ident(k) + "::text, 'null')")
                        .collect(Collectors.joining(" || ', ' || "));
                String cible = u.predicat() == null ? "public." + ident(table)
                        : "(SELECT * FROM public." + ident(table) + " WHERE " + u.predicat() + ")";
                for (String v : tx.jdbc().queryForList("SELECT " + valeurs + " FROM " + e.getValue() + " a JOIN " + cible
                        + " b ON " + jointure + " LIMIT 20", String.class)) {
                    conflits.add(conflit(table, u.nom(), v));
                    if (conflits.size() >= CONFLITS_MAX) return conflits;
                }
            }
            if (table.equals(RegistreTables.APPARTENANCES) && archivees.contains("user_id")) {
                for (String v : tx.jdbc().queryForList("SELECT a.user_id FROM " + e.getValue() + " a WHERE NOT EXISTS "
                        + "(SELECT 1 FROM public.users u WHERE u.id = a.user_id) LIMIT 20", String.class)) {
                    conflits.add(conflit(table, "compte supprimé depuis l'archivage", "user_id=" + v));
                    if (conflits.size() >= CONFLITS_MAX) return conflits;
                }
            }
        }
        return conflits;
    }

    private static Map<String, Object> conflit(String table, String contrainte, String valeurs) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("table", table);
        m.put("contrainte", contrainte);
        m.put("valeurs", valeurs);
        return m;
    }

    private void echec(ArchivageEcole a, Exception e) {
        String cause = MoteurArchivage.cause(e);
        List<Map<String, Object>> conflits = null;
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ArchivageException ae && ae.details().get("conflits") instanceof List<?> l) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> liste = (List<Map<String, Object>>) l;
                conflits = liste;
                break;
            }
        }
        try {
            ArchivageEcole courant = archivages.parId(a.getId()).orElse(a);
            if (courant.getEtat() != EtatArchivage.RESTAURATION_EN_COURS) return;
            courant.echouerRestauration(cause, conflits, horloge.get());
            archivages.enregistrer(courant);
            journal.noter(courant, "RESTAURATION_REFUSEE", courant.getRestaurationDemandeePar(), null,
                    "Rien n'a été écrit ; archive intacte. " + cause);
            log.warn("Restauration {} de l'école {} refusée : {}", courant.getId(), courant.getOrganizationId(), cause);
        } catch (RuntimeException ex) {
            log.error("Restauration {} : échec non enregistré ({})", a.getId(), ex.getMessage());
        }
    }

    /** Après validation seulement : le fichier quitte le stockage (nouvel essai chaque nuit en cas de panne). */
    private void supprimerFichier(ArchivageEcole a) {
        ArchivageEcole courant = archivages.parId(a.getId()).orElse(a);
        try {
            stockage.supprimer(courant.getEmplacement());
            courant.fichierSupprime(horloge.get());
            archivages.enregistrer(courant);
            journal.noter(courant, "FICHIER_SUPPRIME", null, null, "Fichier d'archive supprimé du stockage après la restauration.");
        } catch (IOException | RuntimeException ex) {
            log.warn("Restauration {} : fichier d'archive non supprimé ({}), nouvel essai chaque nuit", a.getId(), ex.getMessage());
        }
    }

}
