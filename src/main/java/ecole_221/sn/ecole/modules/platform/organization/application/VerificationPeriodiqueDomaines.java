package ecole_221.sn.ecole.modules.platform.organization.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Revérification périodique des domaines propres déclarés (toutes les 6 heures par défaut,
 * {@code ecole.domaines.verification.intervalle}) ; désactivable par {@code ecole.domaines.verification.active=false}.
 * Après 3 échecs consécutifs un domaine passe DESACTIVE ; il redevient ACTIF dès qu'il pointe de nouveau.
 */
@Component
@ConditionalOnProperty(name = "ecole.domaines.verification.active", havingValue = "true", matchIfMissing = true)
public class VerificationPeriodiqueDomaines {

    private static final Logger log = LoggerFactory.getLogger(VerificationPeriodiqueDomaines.class);
    private final DomainesEcoleService service;

    public VerificationPeriodiqueDomaines(DomainesEcoleService service) {
        this.service = service;
    }

    @Scheduled(initialDelayString = "${ecole.domaines.verification.delai-initial:PT10M}",
            fixedDelayString = "${ecole.domaines.verification.intervalle:PT6H}")
    public void verifier() {
        int n = service.verifierTout();
        if (n > 0) {
            log.info("[DOMAINES] Revérification périodique : {} domaine(s) vérifié(s)", n);
        }
    }
}
