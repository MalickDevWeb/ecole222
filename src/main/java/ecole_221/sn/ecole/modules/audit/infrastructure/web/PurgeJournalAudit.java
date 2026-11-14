package ecole_221.sn.ecole.modules.audit.infrastructure.web;

import ecole_221.sn.ecole.modules.audit.application.service.ConservationJournalAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Purge de conservation du journal d'audit (2026-11-14) : toutes les minutes par défaut
 * ({@code ecole.audit.purge.intervalle}), désactivable ({@code ecole.audit.purge.active=false}).
 * Une panne est signalée dans les traces sans arrêter les purges suivantes.
 */
@Component
@ConditionalOnProperty(name = "ecole.audit.purge.active", havingValue = "true", matchIfMissing = true)
public class PurgeJournalAudit {

    private static final Logger log = LoggerFactory.getLogger(PurgeJournalAudit.class);
    private final ConservationJournalAuditService conservation;

    public PurgeJournalAudit(ConservationJournalAuditService conservation) {
        this.conservation = conservation;
    }

    @Scheduled(initialDelayString = "${ecole.audit.purge.delai-initial:PT1M}", fixedDelayString = "${ecole.audit.purge.intervalle:PT1M}")
    public void purger() {
        try {
            conservation.purger();
        } catch (RuntimeException e) {
            log.warn("Purge de conservation du journal d'audit impossible : {}", e.getMessage());
        }
    }
}
