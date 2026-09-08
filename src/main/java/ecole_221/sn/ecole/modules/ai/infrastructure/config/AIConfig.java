package ecole_221.sn.ecole.modules.ai.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration pour les services IA (Gemini, Mistral, Mock)
 * Permet de basculer facilement entre différents fournisseurs IA
 */
@Configuration
public class AIConfig {
    
    @Value("${ai.provider:mock}")
    private String provider;
    
    @Value("${ai.gemini.api-key:}")
    private String geminiApiKey;
    
    @Value("${ai.gemini.model:gemini-pro}")
    private String geminiModel;
    
    @Value("${ai.gemini.temperature:0.7}")
    private double geminiTemperature;
    
    @Value("${ai.gemini.max-tokens:1000}")
    private int geminiMaxTokens;
    
    @Value("${ai.mistral.api-key:}")
    private String mistralApiKey;
    
    @Value("${ai.mistral.model:mistral-large-latest}")
    private String mistralModel;
    
    @Value("${ai.mistral.temperature:0.7}")
    private double mistralTemperature;
    
    @Value("${ai.mistral.max-tokens:1000}")
    private int mistralMaxTokens;
    
    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "gemini")
    public AIServiceProperties geminiAIServiceProperties() {
        return new AIServiceProperties(
            "gemini",
            geminiApiKey,
            geminiModel,
            geminiTemperature,
            geminiMaxTokens
        );
    }
    
    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "mistral")
    public AIServiceProperties mistralAIServiceProperties() {
        return new AIServiceProperties(
            "mistral",
            mistralApiKey,
            mistralModel,
            mistralTemperature,
            mistralMaxTokens
        );
    }
    
    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "mock", matchIfMissing = true)
    public AIServiceProperties mockAIServiceProperties() {
        return new AIServiceProperties(
            "mock",
            "mock-key",
            "mock-model",
            0.7,
            1000
        );
    }
    
    public record AIServiceProperties(
        String provider,
        String apiKey,
        String model,
        double temperature,
        int maxTokens
    ) {
        public boolean isConfigured() {
            if ("mock".equals(provider)) {
                return true;
            }
            return apiKey != null && !apiKey.isEmpty() && 
                   !apiKey.equals("your-gemini-api-key") && 
                   !apiKey.equals("your-mistral-api-key");
        }
    }
}