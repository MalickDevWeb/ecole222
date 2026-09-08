package ecole_221.sn.ecole.modules.ai.application.service;

import ecole_221.sn.ecole.modules.ai.application.port.usecase.AIUseCase;
import ecole_221.sn.ecole.modules.ai.infrastructure.adapter.AIServiceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service application pour les opérations IA
 * Implémente les use cases métier en utilisant l'adapter infrastructure
 */
@Service
public class AIService implements AIUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    
    private final AIServiceAdapter aiServiceAdapter;
    
    public AIService(AIServiceAdapter aiServiceAdapter) {
        this.aiServiceAdapter = aiServiceAdapter;
    }
    
    @Override
    public String generateTutorResponse(String message) {
        logger.info("Génération de réponse tuteur pour: {}", message);
        
        // Ajouter le contexte du tuteur IA
        Map<String, Object> context = Map.of(
            "role", "tutor",
            "institution", "École 221",
            "language", "french"
        );
        
        return aiServiceAdapter.generateResponse(message, context);
    }
    
    @Override
    public String getTutorResponse(String message) {
        // Alias pour generateTutorResponse
        return generateTutorResponse(message);
    }
    
    @Override
    public String generateTutorResponseWithContext(String message, Map<String, Object> context) {
        logger.info("Génération de réponse tuteur avec contexte pour: {}", message);
        
        // Fusionner le contexte tuteur avec le contexte fourni
        Map<String, Object> tutorContext = Map.of(
            "role", "tutor",
            "institution", "École 221",
            "language", "french"
        );
        
        // Note: pour simplifier, on utilise le contexte fourni seulement
        // Dans une implémentation complète, on fusionnerait les deux contextes
        return aiServiceAdapter.generateResponse(message, context);
    }
    
    @Override
    public String generateEducationalContent(String topic, String type) {
        logger.info("Génération de contenu éducatif - Type: {}, Sujet: {}", type, topic);
        
        String prompt = switch (type.toLowerCase()) {
            case "quiz" -> "Génère un quiz sur le sujet: " + topic + " avec 5 questions à choix multiples et leurs réponses.";
            case "summary" -> "Génère un résumé structuré sur le sujet: " + topic + " avec les points clés.";
            case "exercise" -> "Génère 3 exercices pratiques sur le sujet: " + topic + " avec leurs solutions.";
            case "explanation" -> "Explique le concept: " + topic + " de manière simple et pédagogique.";
            default -> "Génère du contenu éducatif sur le sujet: " + topic;
        };
        
        Map<String, Object> context = Map.of(
            "type", type,
            "topic", topic,
            "language", "french"
        );
        
        return aiServiceAdapter.generateResponse(prompt, context);
    }
    
    @Override
    public boolean isAIServiceAvailable() {
        return aiServiceAdapter.isAvailable();
    }
    
    @Override
    public Map<String, Object> getAIServiceInfo() {
        return Map.of(
            "provider", aiServiceAdapter.getProviderName(),
            "available", aiServiceAdapter.isAvailable(),
            "timestamp", java.time.LocalDateTime.now()
        );
    }
}