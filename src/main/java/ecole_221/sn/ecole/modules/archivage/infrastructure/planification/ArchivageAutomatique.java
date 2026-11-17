package ecole_221.sn.ecole.modules.archivage.infrastructure.planification;

import ecole_221.sn.ecole.modules.archivage.application.ArchivageEcolesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tâche quotidienne de l'archivage (décision D2 du Tech Lead), la nuit par défaut ({@code ecole.archivage.automatique.cron},
 * 2 h 30, avant la purge de la corbeille de 3 h 30) ; désactivable ({@code ecole.archivage.automatique.active=false}).
 * Elle archive les écoles suspendues depuis le seuil et passe « à confirmer » les archives dont la conservation est
 * échue. Elle ne supprime jamais une archive.
 */
@Component
@ConditionalOnProperty(name = "ecole.archivage.automatique.active", havingValue = "true", matchIfMissing = true)
public class ArchivageAutomatique {

    private static final Logger log = LoggerFactory.getLogger(ArchivageAutomatique.class);
    private final ArchivageEcolesService archivage;

    public ArchivageAutomatique(ArchivageEcolesService archivage) {
        this.archivage = archivage;
    }

    @Scheduled(cron = "${ecole.archivage.automatique.cron:0 30 2 * * *}")
    public void executer() {
        try {
            archivage.tacheQuotidienne();
        } catch (RuntimeException e) {
            log.warn("Tâche quotidienne d'archivage impossible : {}", e.getMessage());
        }
    }
}
