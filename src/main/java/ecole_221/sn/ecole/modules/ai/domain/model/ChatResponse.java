package ecole_221.sn.ecole.modules.ai.domain.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Modèle de réponse pour les conversations IA
 */
public record ChatResponse(
    String text,
    String model,
    String provider,
    LocalDateTime timestamp,
    Map<String, Object> metadata
) {
    public ChatResponse(String text, String model, String provider) {
        this(text, model, provider, LocalDateTime.now(), Map.of());
    }
    
    public ChatResponse(String text, String model) {
        this(text, model, "unknown", LocalDateTime.now(), Map.of());
    }
}