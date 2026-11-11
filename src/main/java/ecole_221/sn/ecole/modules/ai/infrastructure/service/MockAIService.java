package ecole_221.sn.ecole.modules.ai.infrastructure.service;

import ecole_221.sn.ecole.modules.ai.domain.port.AIServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Fournisseur d'IA ABSENT (aucun « ai.provider » configuré).
 *
 * <p>Avant (jusqu'au 2026-11-09) : faux tuteur qui se déclarait disponible et renvoyait des phrases toutes
 * faites (« Je comprends votre question… ») présentées comme une réponse d'IA. Constat Kasongo SUP-QC-20261109.
 * Désormais : déclaré indisponible et message explicite ; aucune réponse simulée.</p>
 */
@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAIService implements AIServicePort {

    private static final Logger logger = LoggerFactory.getLogger(MockAIService.class);
    static final String INDISPONIBLE = "Le tuteur IA n'est pas disponible : aucun fournisseur d'intelligence artificielle "
            + "n'est configuré sur ce serveur. Contactez l'administration de l'établissement.";

    @Override
    public String generateResponse(String message, Map<String, Object> context) {
        logger.warn("IA demandée alors qu'aucun fournisseur n'est configuré (ai.provider absent ou « mock »)");
        return INDISPONIBLE;
    }

    @Override
    public String generateResponseWithModel(String message, String model, Map<String, Object> context) {
        return generateResponse(message, context);
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String getProviderName() {
        return "Aucun (non configuré)";
    }
}
