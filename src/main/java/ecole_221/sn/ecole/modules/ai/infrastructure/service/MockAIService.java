package ecole_221.sn.ecole.modules.ai.infrastructure.service;

import ecole_221.sn.ecole.modules.ai.domain.port.AIServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service IA mock pour les tests et le développement
 * Simule une IA sans nécessiter de clé API
 */
@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAIService implements AIServicePort {
    
    private static final Logger logger = LoggerFactory.getLogger(MockAIService.class);
    
    @Override
    public String generateResponse(String message, Map<String, Object> context) {
        logger.info("Mock AI Service - Génération de réponse pour: {}", message);
        
        return generateMockResponse(message, context);
    }
    
    @Override
    public String generateResponseWithModel(String message, String model, Map<String, Object> context) {
        logger.info("Mock AI Service - Génération de réponse avec modèle {} pour: {}", model, message);
        
        return generateMockResponse(message, context);
    }
    
    @Override
    public boolean isAvailable() {
        return true;
    }
    
    @Override
    public String getProviderName() {
        return "Mock AI";
    }
    
    private String generateMockResponse(String message, Map<String, Object> context) {
        // Réponses mockées simples mais réalistes
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.contains("bonjour") || lowerMessage.contains("salut")) {
            return "Bonjour ! Je suis votre tuteur IA de l'École 221. Comment puis-je vous aider aujourd'hui ?";
        }
        
        if (lowerMessage.contains("aide") || lowerMessage.contains("question")) {
            return "Je suis là pour vous aider ! Posez-moi vos questions sur vos cours, devoirs ou tout autre sujet pédagogique. Je vous guiderai avec plaisir.";
        }
        
        if (lowerMessage.contains("quiz") || lowerMessage.contains("exercice")) {
            return "Je peux vous proposer des quiz et exercices pour vous entraîner. Dites-moi le sujet qui vous intéresse et je vous préparerai des questions adaptées.";
        }
        
        if (lowerMessage.contains("merci")) {
            return "Je vous en prie ! N'hésitez pas à revenir si vous avez d'autres questions. Bon courage dans vos études !";
        }
        
        // Réponse par défaut
        return "Je comprends votre question concernant : \"" + message + "\". En tant que tuteur IA de l'École 221, je peux vous aider à approfondir ce sujet. Voulez-vous que je vous propose des explications, des exemples ou des exercices pratiques ?";
    }
}