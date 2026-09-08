package ecole_221.sn.ecole.modules.ai.domain.model;

import ecole_221.sn.ecole.modules.ai.domain.valueobject.AIModel;
import ecole_221.sn.ecole.modules.ai.domain.valueobject.AIProvider;

import java.time.LocalDateTime;
import java.util.Map;

public class ChatResponseFactory {
    
    public static ChatResponse create(String text, AIModel model, AIProvider provider) {
        return new ChatResponse(text, model.model(), provider.provider());
    }
    
    public static ChatResponse createWithMetadata(String text, AIModel model, AIProvider provider, 
                                                   Map<String, Object> metadata) {
        return new ChatResponse(text, model.model(), provider.provider(), LocalDateTime.now(), metadata);
    }
    
    public static ChatResponse createSimple(String text, AIModel model) {
        return new ChatResponse(text, model.model());
    }
    
    public static ChatResponse reconstitute(String text, String model, String provider, 
                                           LocalDateTime timestamp, Map<String, Object> metadata) {
        return new ChatResponse(text, model, provider, timestamp, metadata);
    }
}