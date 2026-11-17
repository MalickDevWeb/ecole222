package ecole_221.sn.ecole.modules.archivage.application;

import ecole_221.sn.ecole.modules.archivage.domain.model.ArchivageEcole;
import ecole_221.sn.ecole.modules.archivage.domain.port.ArchivagesEcolesPort;
import ecole_221.sn.ecole.modules.audit.infrastructure.persistence.JournalAudit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Traces de chaque étape : journal d'audit (organization_id = « plateforme ») ET journal de l'archivage
 * (archivages_ecoles_journal, jamais purgé : l'audit n'est conservé que 30 minutes, V14). Une panne d'écriture d'une
 * trace est signalée sans interrompre l'opération.
 */
public class JournalArchivage {

    private static final Logger log = LoggerFactory.getLogger(JournalArchivage.class);
    public static final String SYSTEME = "système (archivage)";

    private final ArchivagesEcolesPort archivages;
    private final JournalAudit audit;

    public JournalArchivage(ArchivagesEcolesPort archivages, JournalAudit audit) {
        this.archivages = archivages;
        this.audit = audit;
    }

    /**
     * @param evenement   ARCHIVAGE_DEMANDE, ARCHIVAGE_EXPORTE, ARCHIVAGE_VERIFIE, ARCHIVAGE_PURGE, ARCHIVAGE_TERMINE,
     *                    ARCHIVAGE_ECHEC, CONSERVATION_ECHUE, RESTAURATION_DEMANDEE, RESTAURATION_TERMINEE,
     *                    RESTAURATION_REFUSEE, SUPPRESSION_DEFINITIVE…
     * @param acteurId    compte du super-admin, ou null pour la tâche planifiée
     */
    public void noter(ArchivageEcole a, String evenement, String acteurId, String acteurNom, String detail) {
        String acteur = acteurId == null ? SYSTEME : acteurNom == null ? acteurId : acteurNom + " (" + acteurId + ")";
        try {
            archivages.journaliser(a.getId(), a.getOrganizationId(), evenement, a.getEtat(), acteur, detail);
        } catch (RuntimeException e) {
            log.warn("Étape d'archivage non inscrite au journal de l'archivage ({} {}) : {}", evenement, a.getId(), e.getMessage());
        }
        if (audit != null) {
            audit.inscrire(new JournalAudit.Entree(JournalAudit.PLATEFORME, acteurId, acteurId == null ? SYSTEME : acteurNom,
                    acteurId == null ? null : "SUPER_ADMIN", evenement, "archivage/ecole", a.getOrganizationId(),
                    "Archivage " + a.getId() + " — école « " + a.getEcoleNom() + " » : " + (detail == null ? evenement : detail),
                    null, null, "archivage"));
        }
    }
}
