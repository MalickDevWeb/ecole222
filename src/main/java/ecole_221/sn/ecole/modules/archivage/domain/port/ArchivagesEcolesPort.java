package ecole_221.sn.ecole.modules.archivage.domain.port;

import ecole_221.sn.ecole.modules.archivage.domain.model.ArchivageEcole;
import ecole_221.sn.ecole.modules.archivage.domain.model.EtatArchivage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Port de persistance du suivi des archivages (table archivages_ecoles et son journal, V24). */
public interface ArchivagesEcolesPort {

    /** Insère un nouvel archivage ; refuse (CONFLIT) si une opération vivante existe déjà pour l'école. */
    void inserer(ArchivageEcole archivage);

    void enregistrer(ArchivageEcole archivage);

    /**
     * Enregistre seulement si l'archivage est encore dans l'état {@code etatAttendu} en base (deux super-admins ou un
     * double clic : une seule transition passe). Renvoie false sinon (rien n'est écrit).
     */
    boolean enregistrerSi(ArchivageEcole archivage, EtatArchivage etatAttendu);

    Optional<ArchivageEcole> parId(String id);

    /** Archivage vivant (en cours, archivé, à confirmer, en restauration) de l'école, s'il existe. */
    Optional<ArchivageEcole> vivantDeLEcole(String organizationId);

    /** Dernier archivage de l'école, quel que soit son état. */
    Optional<ArchivageEcole> dernierDeLEcole(String organizationId);

    List<ArchivageEcole> lister(EtatArchivage etat, String organizationId);

    List<ArchivageEcole> parEtat(EtatArchivage etat);

    List<ArchivageEcole> echeancesAtteintes(LocalDateTime maintenant);

    List<ArchivageEcole> fichiersASupprimer();

    void journaliser(String archivageId, String organizationId, String evenement, EtatArchivage etat, String acteur,
                     String detail);

    List<Map<String, Object>> journal(String archivageId);
}
