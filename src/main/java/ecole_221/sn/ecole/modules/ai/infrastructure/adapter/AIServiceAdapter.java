package ecole_221.sn.ecole.modules.ai.infrastructure.adapter;

import ecole_221.sn.ecole.modules.ai.domain.port.AIServicePort;
import org.springframework.stereotype.Component;

/**
 * Adapter pour le service IA
 * Permet de connecter l'implémentation infrastructure au port domain
 */
@Component
public class AIServiceAdapter {
    
    private final AIServicePort aiServicePort;
    
    public AIServiceAdapter(AIServicePort aiServicePort) {
        this.aiServicePort = aiServicePort;
    }
    
    public String generateResponse(String message) {
        return aiServicePort.generateResponse(message, java.util.Map.of());
    }
    
    public String generateResponse(String message, java.util.Map<String, Object> context) {
        return aiServicePort.generateResponse(message, context);
    }
    
    public String generateResponseWithModel(String message, String model, java.util.Map<String, Object> context) {
        return aiServicePort.generateResponseWithModel(message, model, context);
    }
    
    public boolean isAvailable() {
        return aiServicePort.isAvailable();
    }
    
    public String getProviderName() {
        return aiServicePort.getProviderName();
    }
}