package ecole_221.sn.ecole.modules.corbeille.infrastructure.web;

import ecole_221.sn.ecole.modules.corbeille.application.CorbeilleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Purge automatique de la corbeille : chaque nuit à 3 h 30 par défaut ({@code ecole.corbeille.purge.cron}),
 * désactivable ({@code ecole.corbeille.purge.active=false}). Supprime définitivement ce qui est dans la corbeille
 * depuis plus de {@code ecole.corbeille.conservation-jours} jours (30 par défaut), chaque purge étant journalisée.
 */
@Component
@ConditionalOnProperty(name = "ecole.corbeille.purge.active", havingValue = "true", matchIfMissing = true)
public class PurgeCorbeille {

    private static final Logger log = LoggerFactory.getLogger(PurgeCorbeille.class);
    private final CorbeilleService corbeille;

    public PurgeCorbeille(CorbeilleService corbeille) {
        this.corbeille = corbeille;
    }

    @Scheduled(cron = "${ecole.corbeille.purge.cron:0 30 3 * * *}")
    public void purger() {
        try {
            int n = corbeille.purgerExpires();
            if (n > 0) log.info("Corbeille : {} définitivement après {} jours", n == 1 ? "1 élément supprimé" : n + " éléments supprimés",
                    corbeille.conservationJours());
        } catch (RuntimeException e) {
            log.warn("Purge de la corbeille impossible : {}", e.getMessage());
        }
    }
}
