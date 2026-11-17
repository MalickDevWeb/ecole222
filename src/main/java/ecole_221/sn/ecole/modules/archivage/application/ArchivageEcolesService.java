package ecole_221.sn.ecole.modules.archivage.application;

import ecole_221.sn.ecole.modules.archivage.domain.exception.ArchivageException;
import ecole_221.sn.ecole.modules.archivage.domain.model.ArchivageEcole;
import ecole_221.sn.ecole.modules.archivage.domain.model.Declenchement;
import ecole_221.sn.ecole.modules.archivage.domain.model.EtatArchivage;
import ecole_221.sn.ecole.modules.archivage.domain.port.ArchivagesEcolesPort;
import ecole_221.sn.ecole.modules.archivage.domain.port.CleDesArchives;
import ecole_221.sn.ecole.modules.archivage.domain.port.EcolesArchivablesPort;
import ecole_221.sn.ecole.modules.archivage.domain.port.EcolesArchivablesPort.EcoleArchivable;
import ecole_221.sn.ecole.modules.archivage.domain.port.StockageArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Cas d'usage de l'archivage des écoles (décisions du Tech Lead du 16/11), réservés au SUPER_ADMIN :
 * archiver maintenant, restaurer, confirmer la suppression définitive, lister ; plus la tâche quotidienne (archivage au
 * seuil, fin de conservation → « suppression à confirmer », jamais d'effacement) et la reprise après redémarrage.
 *
 * <p>Les opérations longues (export, purge, restauration) s'exécutent en arrière-plan, une à la fois ; la requête HTTP
 * ne fait que les accepter (202) après les contrôles et le gel de l'école.</p>
 */
public class ArchivageEcolesService {

    private static final Logger log = LoggerFactory.getLogger(ArchivageEcolesService.class);
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ArchivagesEcolesPort archivages;
    private final EcolesArchivablesPort ecoles;
    private final StockageArchive stockage;
    private final CleDesArchives cles;
    private final ReglagesArchivageService reglages;
    private final JournalArchivage journal;
    private final MoteurArchivage moteur;
    private final MoteurRestauration restauration;
    private final TransactionsArchivage tx;
    private final Executor executeur;
    private final Set<String> enCours = ConcurrentHashMap.newKeySet();
    private Supplier<LocalDateTime> horloge = LocalDateTime::now;

    public ArchivageEcolesService(ArchivagesEcolesPort archivages, EcolesArchivablesPort ecoles, StockageArchive stockage,
                                  CleDesArchives cles, ReglagesArchivageService reglages, JournalArchivage journal,
                                  MoteurArchivage moteur, MoteurRestauration restauration, TransactionsArchivage tx,
                                  Executor executeur) {
        this.archivages = archivages;
        this.ecoles = ecoles;
        this.stockage = stockage;
        this.cles = cles;
        this.reglages = reglages;
        this.journal = journal;
        this.moteur = moteur;
        this.restauration = restauration;
        this.tx = tx;
        this.executeur = executeur;
    }

    void horloge(Supplier<LocalDateTime> h) {
        this.horloge = h;
    }

    private LocalDateTime maintenant() {
        return horloge.get();
    }

    /** Stockage et clé utilisables ici ; sinon refus clair (503), jamais de repli. */
    public void verifierPret() {
        stockage.verifierPret();
        cles.obtenir();
    }

    // ── Lecture ─────────────────────────────────────────────────────────────────

    public List<ArchivageEcole> lister(String etat, String organizationId) {
        EtatArchivage filtre = null;
        if (etat != null && !etat.isBlank()) {
            try {
                filtre = EtatArchivage.valueOf(etat.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw ArchivageException.invalide("État inconnu : " + etat + ".");
            }
        }
        return archivages.lister(filtre, organizationId == null || organizationId.isBlank() ? null : organizationId.trim());
    }

    public ArchivageEcole detail(String archivageId) {
        return archivages.parId(archivageId).orElseThrow(() -> ArchivageException.introuvable("Archive introuvable."));
    }

    public List<Map<String, Object>> journal(String archivageId) {
        return archivages.journal(archivageId);
    }

    // ── Archiver maintenant ─────────────────────────────────────────────────────

    public ArchivageEcole archiverMaintenant(String organizationId, String confirmationSlug, String motif,
                                            String acteurId, String acteurNom) {
        EcoleArchivable ecole = ecole(organizationId);
        confirmer(ecole, confirmationSlug);
        verifierPret();
        ArchivageEcole a = demarrer(ecole, Declenchement.MANUEL, motif, acteurId, acteurNom);
        lancer(a.getId(), () -> moteur.executer(a.getId()));
        return a;
    }

    private EcoleArchivable ecole(String organizationId) {
        return ecoles.lire(organizationId).orElseThrow(() -> ArchivageException.introuvable("École introuvable."));
    }

    private static void confirmer(EcoleArchivable ecole, String confirmationSlug) {
        if (confirmationSlug == null || !confirmationSlug.trim().equals(ecole.slug())) {
            throw ArchivageException.refus("Confirmation refusée : saisissez l'identifiant de l'école (" + ecole.slug() + ").");
        }
    }

    /** Gel : ligne d'archivage créée (une seule vivante par école) et école SUSPENDED → ARCHIVED, ensemble. */
    ArchivageEcole demarrer(EcoleArchivable ecole, Declenchement declenchement, String motif, String acteurId, String acteurNom) {
        archivages.vivantDeLEcole(ecole.id()).ifPresent(v -> {
            throw ArchivageException.conflit(v.getEtat() == EtatArchivage.ARCHIVAGE_EN_COURS
                    ? "Un archivage est déjà en cours pour cette école."
                    : "Cette école a déjà une archive (" + v.getEtat().libelle().toLowerCase(Locale.ROOT) + ").");
        });
        if ("ARCHIVED".equals(ecole.statut()) && archivages.dernierDeLEcole(ecole.id())
                .filter(d -> d.getEtat() == EtatArchivage.SUPPRIMEE_DEFINITIVEMENT).isPresent()) {
            throw ArchivageException.conflit("Cette école a été supprimée définitivement.");
        }
        ArchivageEcole a = ArchivageEcole.demarrer(ecole.id(), ecole.nom(), ecole.slug(), ecole.statut(), ecole.suspendueLe(),
                declenchement, motif, acteurId, acteurNom, maintenant());
        tx.ecriture().executeWithoutResult(s -> {
            archivages.inserer(a);
            if ("SUSPENDED".equals(ecole.statut()) && !ecoles.geler(ecole.id())) {
                throw ArchivageException.conflit("L'école a changé de statut : archivage annulé.");
            }
        });
        journal.noter(a, "ARCHIVAGE_DEMANDE", acteurId, acteurNom, (declenchement == Declenchement.AUTOMATIQUE
                ? "Archivage automatique (suspendue depuis le " + (ecole.suspendueLe() == null ? "?" : ecole.suspendueLe().format(JOUR)) + ")"
                : "Archiver maintenant") + (motif == null ? "." : " — motif : " + motif));
        return a;
    }

    private void lancer(String id, Runnable operation) {
        if (!enCours.add(id)) return;
        try {
            executeur.execute(() -> {
                try {
                    operation.run();
                } catch (RuntimeException e) {
                    log.error("Opération d'archivage {} interrompue : {}", id, e.getMessage());
                } finally {
                    enCours.remove(id);
                }
            });
        } catch (RuntimeException e) {
            enCours.remove(id);
            throw e;
        }
    }

    // ── Restaurer ───────────────────────────────────────────────────────────────

    public ArchivageEcole restaurer(String organizationId, String confirmationSlug, String acteurId, String acteurNom) {
        EcoleArchivable ecole = ecole(organizationId);
        confirmer(ecole, confirmationSlug);
        ArchivageEcole a = archivages.vivantDeLEcole(ecole.id())
                .orElseThrow(() -> ArchivageException.conflit("Aucune archive à restaurer pour cette école."));
        verifierPret();
        EtatArchivage avant = a.getEtat();
        a.demarrerRestauration(acteurId, maintenant());
        if (!archivages.enregistrerSi(a, avant)) {
            throw ArchivageException.conflit("L'archive a changé d'état entre-temps : rechargez la liste.");
        }
        journal.noter(a, "RESTAURATION_DEMANDEE", acteurId, acteurNom, "Restauration demandée.");
        lancer(a.getId(), () -> restauration.executer(a.getId()));
        return a;
    }

    // ── Suppression définitive ──────────────────────────────────────────────────

    public ArchivageEcole supprimerDefinitivement(String organizationId, boolean confirmation, String acteurId, String acteurNom) {
        ecole(organizationId);
        ArchivageEcole a = archivages.vivantDeLEcole(organizationId)
                .orElseThrow(() -> ArchivageException.conflit("Aucune archive à supprimer pour cette école."));
        EtatArchivage avant = a.getEtat();
        a.supprimerDefinitivement(confirmation, acteurId, maintenant());
        if (!archivages.enregistrerSi(a, avant)) {
            throw ArchivageException.conflit("L'archive a changé d'état entre-temps : rechargez la liste.");
        }
        journal.noter(a, "SUPPRESSION_DEFINITIVE", acteurId, acteurNom, "Suppression définitive confirmée : clé de l'archive "
                + "effacée, fichier retiré du stockage. Trace conservée (école, dates, auteur, empreinte "
                + a.getEmpreinteSha256() + ").");
        lancer("fichier:" + a.getId(), () -> supprimerFichier(a.getId()));
        return a;
    }

    void supprimerFichier(String archivageId) {
        ArchivageEcole a = archivages.parId(archivageId).orElse(null);
        if (a == null || !a.isFichierASupprimer() || a.getEmplacement() == null) return;
        try {
            stockage.supprimer(a.getEmplacement());
            a.fichierSupprime(maintenant());
            archivages.enregistrer(a);
            journal.noter(a, "FICHIER_SUPPRIME", null, null, "Fichier d'archive supprimé du stockage.");
        } catch (IOException | RuntimeException e) {
            a.noterErreur("Fichier non supprimé du stockage : nouvel essai chaque nuit.", maintenant());
            archivages.enregistrer(a);
            log.warn("Archive {} : fichier non supprimé ({})", archivageId, e.getMessage());
        }
    }

    // ── Tâche quotidienne et reprise ────────────────────────────────────────────

    /**
     * 1. Fin de conservation : ARCHIVEE → SUPPRESSION_A_CONFIRMER (rien n'est effacé, D4).
     * 2. Fichiers restés dans le stockage après une restauration ou une suppression : nouvel essai.
     * 3. Archivages interrompus : reprise.
     * 4. Écoles suspendues depuis le seuil : archivage automatique (si le stockage et la clé sont prêts).
     */
    public void tacheQuotidienne() {
        LocalDateTime maintenant = maintenant();
        for (ArchivageEcole a : archivages.echeancesAtteintes(maintenant)) {
            try {
                a.passerASupprimer(maintenant);
                if (archivages.enregistrerSi(a, EtatArchivage.ARCHIVEE)) {
                    journal.noter(a, "CONSERVATION_ECHUE", null, null, "Fin de conservation : suppression définitive à "
                            + "confirmer par le super-admin. Rien n'est effacé automatiquement.");
                }
            } catch (RuntimeException e) {
                log.warn("Fin de conservation de l'archive {} non traitée : {}", a.getId(), e.getMessage());
            }
        }
        for (ArchivageEcole a : archivages.fichiersASupprimer()) {
            lancer("fichier:" + a.getId(), () -> supprimerFichier(a.getId()));
        }
        reprendreArchivages();
        try {
            verifierPret();
        } catch (ArchivageException e) {
            log.warn("Archivage automatique suspendu : {}", e.getMessage());
            return;
        }
        int seuil = reglages.lire().reglages().seuilJours();
        for (EcoleArchivable ecole : ecoles.suspenduesAvant(maintenant.minusDays(seuil))) {
            try {
                ArchivageEcole a = demarrer(ecole, Declenchement.AUTOMATIQUE, "Suspendue depuis plus de " + seuil + " jours", null, null);
                lancer(a.getId(), () -> moteur.executer(a.getId()));
            } catch (RuntimeException e) {
                log.warn("Archivage automatique de l'école {} non lancé : {}", ecole.id(), MoteurArchivage.cause(e));
            }
        }
    }

    private void reprendreArchivages() {
        for (ArchivageEcole a : archivages.parEtat(EtatArchivage.ARCHIVAGE_EN_COURS)) {
            lancer(a.getId(), () -> moteur.executer(a.getId()));
        }
    }

    /**
     * Au démarrage : un archivage interrompu reprend (export refait s'il n'avait pas atteint la purge, purge continuée
     * sinon) ; une restauration interrompue a été annulée par la base (une seule transaction) : l'archive revient à son
     * état d'avant.
     */
    public void reprendreOperationsInterrompues() {
        reprendreArchivages();
        for (ArchivageEcole a : archivages.parEtat(EtatArchivage.RESTAURATION_EN_COURS)) {
            if (enCours.contains(a.getId())) continue;
            try {
                a.echouerRestauration("Restauration interrompue par un redémarrage : relancez-la.", null, maintenant());
                if (archivages.enregistrerSi(a, EtatArchivage.RESTAURATION_EN_COURS)) {
                    journal.noter(a, "RESTAURATION_INTERROMPUE", null, null, "Rien n'a été écrit ; archive intacte.");
                }
            } catch (RuntimeException e) {
                log.warn("Restauration interrompue {} non remise en état : {}", a.getId(), e.getMessage());
            }
        }
    }

    /** Écoles qui seraient archivées à la prochaine tâche avec le seuil donné (aperçu des réglages). */
    public int ecolesAuDelaDuSeuil(int seuilJours) {
        return ecoles.suspenduesAvant(maintenant().minusDays(seuilJours)).size();
    }

    public String nomStockage() {
        return stockage.nom();
    }

    /** Arrêt de l'application : l'opération en cours est interrompue et reprendra au prochain démarrage. */
    public void fermer() {
        if (executeur instanceof java.util.concurrent.ExecutorService e) {
            e.shutdownNow();
        }
    }
}
