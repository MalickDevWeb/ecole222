package ecole_221.sn.ecole.modules.ai.infrastructure.service;

import ecole_221.sn.ecole.modules.ai.domain.port.AIServicePort;
import ecole_221.sn.ecole.modules.ai.infrastructure.config.AIConfig.AIServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Implémentation du service IA avec Mistral AI
 * Peut être facilement remplacé par une autre implémentation (Gemini, OpenAI, etc.)
 */
@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "mistral")
public class MistralAIService implements AIServicePort {
    
    private static final Logger logger = LoggerFactory.getLogger(MistralAIService.class);
    
    private final AIServiceProperties properties;
    private final WebClient webClient;
    
    @Autowired
    public MistralAIService(AIServiceProperties properties, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClient = webClientBuilder.build();
        logger.info("MistralAIService initialisé avec provider: {}", properties.provider());
    }
    
    @Override
    public String generateResponse(String message, Map<String, Object> context) {
        return generateResponseWithModel(message, properties.model(), context);
    }
    
    @Override
    public String generateResponseWithModel(String message, String model, Map<String, Object> context) {
        if (!isAvailable()) {
            logger.warn("Mistral service non configuré, utilisation du fallback");
            return generateFallbackResponse(message, context);
        }
        
        try {
            logger.info("Génération de réponse avec Mistral pour: {}", message);
            
            // Utiliser l'API REST Mistral
            return generateWithMistralAPI(message, model, context);
            
        } catch (Exception e) {
            logger.error("Erreur lors de la génération avec Mistral: {}", e.getMessage());
            return generateFallbackResponse(message, context);
        }
    }
    
    @Override
    public boolean isAvailable() {
        return properties.isConfigured();
    }
    
    @Override
    public String getProviderName() {
        return "Mistral";
    }
    
    private String generateWithMistralAPI(String message, String model, Map<String, Object> context) {
        try {
            String url = "https://api.mistral.ai/v1/chat/completions";
            
            Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", java.util.List.of(
                    Map.of(
                        "role", "user",
                        "content", message
                    )
                ),
                "temperature", 0.7,
                "max_tokens", 1000
            );
            
            String response = webClient.post()
                    .uri(url)
                    .headers(headers -> {
                        headers.set("Authorization", "Bearer " + properties.apiKey());
                        headers.set("Content-Type", "application/json");
                        headers.set("Accept", "application/json");
                    })
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            logger.info("Réponse Mistral générée avec succès");
            return extractContentFromResponse(response);
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'appel API Mistral: {}", e.getMessage());
            return generateFallbackResponse(message, context);
        }
    }
    
    private String extractContentFromResponse(String response) {
        try {
            // Parser la réponse JSON pour extraire le contenu
            // Pour l'instant, retourner la réponse brute ou une version simplifiée
            if (response.contains("\"content\"")) {
                int contentStart = response.indexOf("\"content\"") + 11;
                int contentEnd = response.indexOf("\"", contentStart);
                if (contentEnd > contentStart) {
                    return response.substring(contentStart, contentEnd)
                            .replace("\\n", "\n")
                            .replace("\\\"", "\"");
                }
            }
            return response;
        } catch (Exception e) {
            logger.error("Erreur lors de l'extraction du contenu: {}", e.getMessage());
            return response;
        }
    }
    
    private String generateFallbackResponse(String message, Map<String, Object> context) {
        logger.info("Utilisation de la réponse fallback pour: {}", message);
        
        // Réponse fallback simple qui simule une IA
        return "Je suis votre assistant IA (mode développement). Je suis actuellement en mode simulation car le service Mistral n'est pas configuré. Votre message : \"" + message + "\" sera traité une fois le service IA activé avec une clé API valide.";
    }
}