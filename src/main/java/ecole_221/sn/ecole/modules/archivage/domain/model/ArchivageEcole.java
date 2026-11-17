package ecole_221.sn.ecole.modules.archivage.domain.model;

import ecole_221.sn.ecole.modules.archivage.domain.exception.ArchivageException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Agrégat « archivage d'une école » (décisions du Tech Lead du 16/11) : porte la machine à états et les règles
 * (statut de départ admis, point de non-retour, échéance de conservation non rétroactive, suppression toujours
 * confirmée). Aucune dépendance technique : la persistance est faite par un port.
 */
public class ArchivageEcole {

    /** Statuts d'école admis pour lancer un archivage : suspendue, ou archivée par l'ancienne route (données en place). */
    public static final Set<String> STATUTS_DE_DEPART = Set.of("SUSPENDED", "ARCHIVED");
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final String id;
    private final String organizationId;
    private final String ecoleNom;
    private final String ecoleSlug;
    private final Declenchement declenchement;
    private final String statutInitial;
    private final LocalDateTime suspendueLe;
    private final String motif;
    private final String demandePar;
    private final String demandeParNom;
    private final LocalDateTime demandeLe;

    private EtatArchivage etat;
    private EtapeArchivage etape;
    private EtatArchivage etatAvantRestauration;
    private LocalDateTime archiveeLe;
    private Integer conservationJours;
    private LocalDateTime conservationJusquAu;
    private LocalDateTime aConfirmerDepuis;
    private String restaurationDemandeePar;
    private LocalDateTime restaurationDemandeeLe;
    private LocalDateTime restaureeLe;
    private LocalDateTime supprimeeLe;
    private String supprimeePar;
    private String stockage;
    private String emplacement;
    private boolean fichierASupprimer;
    private String empreinteSha256;
    private Long tailleOctets;
    private String versionSchema;
    private String cleEnveloppee;
    private String cleId;
    private Map<String, Long> compteurs = new LinkedHashMap<>();
    private Long nombreLignes;
    private Integer nombreMedias;
    private Map<String, Long> purge = new LinkedHashMap<>();
    private Map<String, Long> restes = new LinkedHashMap<>();
    private List<Map<String, Object>> conflits = List.of();
    private String erreur;
    private int tentatives;
    private LocalDateTime majLe;

    private ArchivageEcole(String id, String organizationId, String ecoleNom, String ecoleSlug, Declenchement declenchement,
                           String statutInitial, LocalDateTime suspendueLe, String motif, String demandePar,
                           String demandeParNom, LocalDateTime demandeLe) {
        this.id = Objects.requireNonNull(id);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.ecoleNom = ecoleNom;
        this.ecoleSlug = ecoleSlug;
        this.declenchement = Objects.requireNonNull(declenchement);
        this.statutInitial = Objects.requireNonNull(statutInitial);
        this.suspendueLe = suspendueLe;
        this.motif = motif;
        this.demandePar = demandePar;
        this.demandeParNom = demandeParNom;
        this.demandeLe = Objects.requireNonNull(demandeLe);
    }

    // ── Création ────────────────────────────────────────────────────────────────

    /**
     * Début d'un archivage : l'école doit être SUSPENDED (ou ARCHIVED par l'ancienne route, données encore en place).
     * L'état devient ARCHIVAGE_EN_COURS, étape GEL.
     */
    public static ArchivageEcole demarrer(String organizationId, String ecoleNom, String ecoleSlug, String statutEcole,
                                          LocalDateTime suspendueLe, Declenchement declenchement, String motif,
                                          String acteurId, String acteurNom, LocalDateTime maintenant) {
        if (statutEcole == null || !STATUTS_DE_DEPART.contains(statutEcole)) {
            throw ArchivageException.conflit("Seule une école suspendue peut être archivée"
                    + (statutEcole == null ? "." : " (statut actuel : " + statutEcole + ")."));
        }
        ArchivageEcole a = new ArchivageEcole("arc-" + UUID.randomUUID(), organizationId, ecoleNom, ecoleSlug,
                declenchement, statutEcole, suspendueLe, motif, acteurId, acteurNom, maintenant);
        a.etat = EtatArchivage.ARCHIVAGE_EN_COURS;
        a.etape = EtapeArchivage.GEL;
        a.majLe = maintenant;
        return a;
    }

    /** Reconstitution depuis la persistance (aucune règle rejouée). */
    public static ArchivageEcole reconstituer(String id, String organizationId, String ecoleNom, String ecoleSlug,
                                              Declenchement declenchement, String statutInitial, LocalDateTime suspendueLe,
                                              String motif, String demandePar, String demandeParNom, LocalDateTime demandeLe,
                                              EtatArchivage etat, EtapeArchivage etape, EtatArchivage etatAvantRestauration) {
        ArchivageEcole a = new ArchivageEcole(id, organizationId, ecoleNom, ecoleSlug, declenchement, statutInitial,
                suspendueLe, motif, demandePar, demandeParNom, demandeLe);
        a.etat = Objects.requireNonNull(etat);
        a.etape = etape;
        a.etatAvantRestauration = etatAvantRestauration;
        return a;
    }

    // ── Archivage ───────────────────────────────────────────────────────────────

    private void exigerEnCours() {
        if (etat != EtatArchivage.ARCHIVAGE_EN_COURS) {
            throw ArchivageException.conflit("Aucun archivage en cours pour cette école (état : " + etat.libelle() + ").");
        }
    }

    public void avancer(EtapeArchivage suivante, LocalDateTime maintenant) {
        exigerEnCours();
        if (etape == EtapeArchivage.PURGE && suivante.avantPurge()) {
            throw ArchivageException.conflit("La purge a commencé : l'archivage ne peut que se terminer.");
        }
        this.etape = suivante;
        this.majLe = maintenant;
    }

    /** Fichier écrit dans le stockage (pas encore relu) : emplacement, empreinte, clé enveloppée, comptes. */
    public void enregistrerFichier(String stockage, String emplacement, String empreinteSha256, long tailleOctets,
                                   String versionSchema, String cleEnveloppee, String cleId, Map<String, Long> compteurs,
                                   long nombreLignes, int nombreMedias, LocalDateTime maintenant) {
        exigerEnCours();
        if (!etape.avantPurge()) {
            throw ArchivageException.conflit("Le fichier d'archive ne peut plus être remplacé : la purge a commencé.");
        }
        this.stockage = stockage;
        this.emplacement = emplacement;
        this.empreinteSha256 = empreinteSha256;
        this.tailleOctets = tailleOctets;
        this.versionSchema = versionSchema;
        this.cleEnveloppee = cleEnveloppee;
        this.cleId = cleId;
        this.compteurs = new LinkedHashMap<>(compteurs);
        this.nombreLignes = nombreLignes;
        this.nombreMedias = nombreMedias;
        this.majLe = maintenant;
    }

    /** Point de non-retour : seulement après une vérification réussie de l'archive relue. */
    public void commencerPurge(LocalDateTime maintenant) {
        exigerEnCours();
        if (etape != EtapeArchivage.VERIFICATION && etape != EtapeArchivage.PURGE) {
            throw ArchivageException.conflit("La purge exige une archive relue et vérifiée.");
        }
        if (emplacement == null || empreinteSha256 == null) {
            throw ArchivageException.conflit("La purge exige une archive enregistrée.");
        }
        this.etape = EtapeArchivage.PURGE;
        this.majLe = maintenant;
    }

    public boolean tablePurgee(String table) {
        return purge.containsKey(table);
    }

    /** Statut à rendre à l'école si l'archivage échoue avant la purge (SUSPENDED, ou ARCHIVED de l'ancienne route). */
    public boolean renduSuspendue() {
        return "SUSPENDED".equals(statutInitial);
    }

    public void noterTablePurgee(String table, long lignesSupprimees, LocalDateTime maintenant) {
        exigerEnCours();
        if (etape != EtapeArchivage.PURGE) {
            throw ArchivageException.conflit("La purge n'a pas commencé.");
        }
        this.purge.merge(table, lignesSupprimees, Long::sum);
        this.majLe = maintenant;
    }

    /**
     * Échec AVANT la purge : rien n'a été supprimé, l'école retrouve son statut d'avant. Renvoie ce statut. Après le
     * début de la purge, un échec n'est jamais « rendu » : l'archivage reste en cours et reprend.
     */
    public String echouerAvantPurge(String cause, LocalDateTime maintenant) {
        exigerEnCours();
        if (!etape.avantPurge()) {
            throw ArchivageException.conflit("La purge a commencé : l'archivage doit être repris, pas annulé.");
        }
        this.etat = EtatArchivage.ECHEC;
        this.erreur = borne(cause);
        this.cleEnveloppee = null;                  // un fichier déjà déposé devient illisible
        this.fichierASupprimer = emplacement != null;
        this.majLe = maintenant;
        return statutInitial;
    }

    /** Incident pendant la purge : noté, l'archivage reste en cours (reprise automatique ou manuelle). */
    public void noterIncidentPurge(String cause, LocalDateTime maintenant) {
        exigerEnCours();
        this.erreur = borne(cause);
        this.tentatives++;
        this.majLe = maintenant;
    }

    /** Purge terminée : l'archive fait foi ; l'échéance de conservation est calculée maintenant et rangée. */
    public void terminerArchivage(int joursDeConservation, Map<String, Long> lignesRestantes, LocalDateTime maintenant) {
        exigerEnCours();
        if (etape != EtapeArchivage.PURGE) {
            throw ArchivageException.conflit("L'archivage ne peut se terminer qu'après la purge.");
        }
        ReglagesArchivage.verifierConservation(joursDeConservation);
        this.etat = EtatArchivage.ARCHIVEE;
        this.etape = EtapeArchivage.TERMINEE;
        this.archiveeLe = maintenant;
        this.conservationJours = joursDeConservation;
        this.conservationJusquAu = maintenant.plusDays(joursDeConservation);
        this.restes = new LinkedHashMap<>(lignesRestantes);
        this.erreur = null;
        this.majLe = maintenant;
    }

    // ── Conservation ────────────────────────────────────────────────────────────

    public boolean echeanceAtteinte(LocalDateTime maintenant) {
        return etat == EtatArchivage.ARCHIVEE && conservationJusquAu != null && !maintenant.isBefore(conservationJusquAu);
    }

    /** Fin de conservation : la tâche planifiée passe l'archive « à confirmer ». Elle n'efface jamais rien. */
    public void passerASupprimer(LocalDateTime maintenant) {
        if (!echeanceAtteinte(maintenant)) {
            throw ArchivageException.conflit("La conservation de cette archive n'est pas échue.");
        }
        this.etat = EtatArchivage.SUPPRESSION_A_CONFIRMER;
        this.aConfirmerDepuis = maintenant;
        this.majLe = maintenant;
    }

    // ── Restauration ────────────────────────────────────────────────────────────

    public void demarrerRestauration(String acteurId, LocalDateTime maintenant) {
        if (etat != EtatArchivage.ARCHIVEE && etat != EtatArchivage.SUPPRESSION_A_CONFIRMER) {
            throw ArchivageException.conflit("Restauration impossible : l'archive est " + etat.libelle().toLowerCase() + ".");
        }
        this.etatAvantRestauration = etat;
        this.etat = EtatArchivage.RESTAURATION_EN_COURS;
        this.restaurationDemandeePar = acteurId;
        this.restaurationDemandeeLe = maintenant;
        this.conflits = List.of();
        this.erreur = null;
        this.majLe = maintenant;
    }

    /** Échec ou refus de la restauration : rien n'a été écrit, l'archive (intacte) retrouve son état. */
    public void echouerRestauration(String cause, List<Map<String, Object>> conflitsTrouves, LocalDateTime maintenant) {
        if (etat != EtatArchivage.RESTAURATION_EN_COURS) {
            throw ArchivageException.conflit("Aucune restauration en cours pour cette école.");
        }
        this.etat = etatAvantRestauration == null ? EtatArchivage.ARCHIVEE : etatAvantRestauration;
        this.etatAvantRestauration = null;
        this.erreur = borne(cause);
        this.conflits = conflitsTrouves == null ? List.of() : List.copyOf(conflitsTrouves);
        this.majLe = maintenant;
    }

    /** Restauration réussie : la clé de l'archive est effacée, le fichier reste à supprimer du stockage. */
    public void terminerRestauration(LocalDateTime maintenant) {
        if (etat != EtatArchivage.RESTAURATION_EN_COURS) {
            throw ArchivageException.conflit("Aucune restauration en cours pour cette école.");
        }
        this.etat = EtatArchivage.RESTAUREE;
        this.etatAvantRestauration = null;
        this.restaureeLe = maintenant;
        this.cleEnveloppee = null;
        this.fichierASupprimer = true;
        this.conflits = List.of();
        this.erreur = null;
        this.majLe = maintenant;
    }

    // ── Suppression définitive ──────────────────────────────────────────────────

    /**
     * Suppression définitive (D4) : seulement « à confirmer » et sur confirmation explicite du super-admin. La clé de
     * l'archive est effacée (archive illisible même si une copie subsiste) ; la trace minimale reste.
     */
    public void supprimerDefinitivement(boolean confirmation, String acteurId, LocalDateTime maintenant) {
        if (!confirmation) {
            throw ArchivageException.invalide("Confirmez la suppression définitive : {\"confirmer\": true}.");
        }
        if (etat == EtatArchivage.ARCHIVEE) {
            throw ArchivageException.conflit("Suppression possible seulement après la fin de la conservation, le "
                    + (conservationJusquAu == null ? "?" : conservationJusquAu.format(JOUR)) + ".");
        }
        if (etat != EtatArchivage.SUPPRESSION_A_CONFIRMER) {
            throw ArchivageException.conflit("Suppression définitive impossible : l'archive est "
                    + etat.libelle().toLowerCase() + ".");
        }
        this.etat = EtatArchivage.SUPPRIMEE_DEFINITIVEMENT;
        this.supprimeeLe = maintenant;
        this.supprimeePar = acteurId;
        this.cleEnveloppee = null;
        this.fichierASupprimer = true;
        this.conflits = List.of();
        this.restes = new LinkedHashMap<>();
        this.erreur = null;
        this.majLe = maintenant;
    }

    public void fichierSupprime(LocalDateTime maintenant) {
        this.fichierASupprimer = false;
        this.majLe = maintenant;
    }

    public void noterErreur(String cause, LocalDateTime maintenant) {
        this.erreur = borne(cause);
        this.majLe = maintenant;
    }

    private static String borne(String s) {
        return s == null ? null : (s.length() <= 2000 ? s : s.substring(0, 2000));
    }

    // ── Hydratation (adaptateur de persistance) ─────────────────────────────────

    public void hydrater(LocalDateTime archiveeLe, Integer conservationJours, LocalDateTime conservationJusquAu,
                         LocalDateTime aConfirmerDepuis, String restaurationDemandeePar, LocalDateTime restaurationDemandeeLe,
                         LocalDateTime restaureeLe, LocalDateTime supprimeeLe, String supprimeePar, String stockage,
                         String emplacement, boolean fichierASupprimer, String empreinteSha256, Long tailleOctets,
                         String versionSchema, String cleEnveloppee, String cleId, Map<String, Long> compteurs,
                         Long nombreLignes, Integer nombreMedias, Map<String, Long> purge, Map<String, Long> restes,
                         List<Map<String, Object>> conflits, String erreur, int tentatives, LocalDateTime majLe) {
        this.archiveeLe = archiveeLe;
        this.conservationJours = conservationJours;
        this.conservationJusquAu = conservationJusquAu;
        this.aConfirmerDepuis = aConfirmerDepuis;
        this.restaurationDemandeePar = restaurationDemandeePar;
        this.restaurationDemandeeLe = restaurationDemandeeLe;
        this.restaureeLe = restaureeLe;
        this.supprimeeLe = supprimeeLe;
        this.supprimeePar = supprimeePar;
        this.stockage = stockage;
        this.emplacement = emplacement;
        this.fichierASupprimer = fichierASupprimer;
        this.empreinteSha256 = empreinteSha256;
        this.tailleOctets = tailleOctets;
        this.versionSchema = versionSchema;
        this.cleEnveloppee = cleEnveloppee;
        this.cleId = cleId;
        this.compteurs = compteurs == null ? new LinkedHashMap<>() : new LinkedHashMap<>(compteurs);
        this.nombreLignes = nombreLignes;
        this.nombreMedias = nombreMedias;
        this.purge = purge == null ? new LinkedHashMap<>() : new LinkedHashMap<>(purge);
        this.restes = restes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(restes);
        this.conflits = conflits == null ? List.of() : List.copyOf(conflits);
        this.erreur = erreur;
        this.tentatives = tentatives;
        this.majLe = majLe;
    }

    // ── Lecture ─────────────────────────────────────────────────────────────────

    public String getId() { return id; }
    public String getOrganizationId() { return organizationId; }
    public String getEcoleNom() { return ecoleNom; }
    public String getEcoleSlug() { return ecoleSlug; }
    public Declenchement getDeclenchement() { return declenchement; }
    public String getStatutInitial() { return statutInitial; }
    public LocalDateTime getSuspendueLe() { return suspendueLe; }
    public String getMotif() { return motif; }
    public String getDemandePar() { return demandePar; }
    public String getDemandeParNom() { return demandeParNom; }
    public LocalDateTime getDemandeLe() { return demandeLe; }
    public EtatArchivage getEtat() { return etat; }
    public EtapeArchivage getEtape() { return etape; }
    public EtatArchivage getEtatAvantRestauration() { return etatAvantRestauration; }
    public LocalDateTime getArchiveeLe() { return archiveeLe; }
    public Integer getConservationJours() { return conservationJours; }
    public LocalDateTime getConservationJusquAu() { return conservationJusquAu; }
    public LocalDateTime getAConfirmerDepuis() { return aConfirmerDepuis; }
    public String getRestaurationDemandeePar() { return restaurationDemandeePar; }
    public LocalDateTime getRestaurationDemandeeLe() { return restaurationDemandeeLe; }
    public LocalDateTime getRestaureeLe() { return restaureeLe; }
    public LocalDateTime getSupprimeeLe() { return supprimeeLe; }
    public String getSupprimeePar() { return supprimeePar; }
    public String getStockage() { return stockage; }
    public String getEmplacement() { return emplacement; }
    public boolean isFichierASupprimer() { return fichierASupprimer; }
    public String getEmpreinteSha256() { return empreinteSha256; }
    public Long getTailleOctets() { return tailleOctets; }
    public String getVersionSchema() { return versionSchema; }
    public String getCleEnveloppee() { return cleEnveloppee; }
    public String getCleId() { return cleId; }
    public Map<String, Long> getCompteurs() { return java.util.Collections.unmodifiableMap(compteurs); }
    public Long getNombreLignes() { return nombreLignes; }
    public Integer getNombreMedias() { return nombreMedias; }
    public Map<String, Long> getPurge() { return java.util.Collections.unmodifiableMap(purge); }
    public Map<String, Long> getRestes() { return java.util.Collections.unmodifiableMap(restes); }
    public List<Map<String, Object>> getConflits() { return conflits; }
    public String getErreur() { return erreur; }
    public int getTentatives() { return tentatives; }
    public LocalDateTime getMajLe() { return majLe; }
}
