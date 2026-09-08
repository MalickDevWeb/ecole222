package ecole_221.sn.ecole.modules.ai.infrastructure.service;

import ecole_221.sn.ecole.modules.ai.domain.port.AIServicePort;
import ecole_221.sn.ecole.modules.ai.infrastructure.config.AIConfig.AIServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Implémentation du service IA avec Google Gemini
 * Peut être facilement remplacé par une autre implémentation (OpenAI, Claude, etc.)
 */
@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "gemini")
public class GeminiAIService implements AIServicePort {
    
    private static final Logger logger = LoggerFactory.getLogger(GeminiAIService.class);
    
    private final AIServiceProperties properties;
    
    @Autowired
    public GeminiAIService(AIServiceProperties properties) {
        this.properties = properties;
        logger.info("GeminiAIService initialisé avec provider: {}", properties.provider());
    }
    
    @Override
    public String generateResponse(String message, Map<String, Object> context) {
        return generateResponseWithModel(message, properties.model(), context);
    }
    
    @Override
    public String generateResponseWithModel(String message, String model, Map<String, Object> context) {
        if (!isAvailable()) {
            logger.warn("Gemini service non configuré, utilisation du fallback");
            return generateFallbackResponse(message, context);
        }
        
        try {
            logger.info("Génération de réponse avec Gemini pour: {}", message);
            
            // Pour l'instant, utiliser l'API REST Gemini directement
            return generateWithRestAPI(message, model, context);
            
        } catch (Exception e) {
            logger.error("Erreur lors de la génération avec Gemini: {}", e.getMessage());
            return generateFallbackResponse(message, context);
        }
    }
    
    @Override
    public boolean isAvailable() {
        return properties.isConfigured();
    }
    
    @Override
    public String getProviderName() {
        return "Gemini";
    }
    
    private String generateWithRestAPI(String message, String model, Map<String, Object> context) {
        // Pour l'instant, retourner le fallback car l'API REST nécessite une clé API valide
        logger.info("API REST Gemini non configurée, utilisation du fallback");
        return generateFallbackResponse(message, context);
    }
    
    private String generateFallbackResponse(String message, Map<String, Object> context) {
        logger.info("Utilisation de la réponse fallback pour: {}", message);
        
        // Réponse fallback simple qui simule une IA
        return "Je suis votre assistant IA (mode développement). Je suis actuellement en mode simulation car le service Gemini n'est pas configuré. Votre message : \"" + message + "\" sera traité une fois le service IA activé avec une clé API valide.";
    }
}