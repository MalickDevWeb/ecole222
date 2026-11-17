package ecole_221.sn.ecole.modules.archivage.infrastructure.planification;

import ecole_221.sn.ecole.modules.archivage.application.ArchivageEcolesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Reprise après coupure (redéploiement, panne) : au démarrage, un archivage interrompu reprend à son étape ; une
 * restauration interrompue (annulée par la base) rend l'archive à son état d'avant. Désactivable
 * ({@code ecole.archivage.reprise.active=false}, tests).
 */
@Component
@ConditionalOnProperty(name = "ecole.archivage.reprise.active", havingValue = "true", matchIfMissing = true)
public class RepriseArchivages {

    private static final Logger log = LoggerFactory.getLogger(RepriseArchivages.class);
    private final ArchivageEcolesService archivage;

    public RepriseArchivages(ArchivageEcolesService archivage) {
        this.archivage = archivage;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void reprendre() {
        try {
            archivage.reprendreOperationsInterrompues();
        } catch (RuntimeException e) {
            log.warn("Reprise des archivages interrompus impossible : {}", e.getMessage());
        }
    }
}
