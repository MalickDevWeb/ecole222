package ecole_221.sn.ecole.modules.ai.domain.valueobject;

public record AIProvider(String provider) {
    public static final AIProvider GEMINI = new AIProvider("Gemini");
    public static final AIProvider MISTRAL = new AIProvider("Mistral");
    // APP-TL-24 : « OpenAI » retiré — aucune implémentation (AI_PROVIDER=openai empêchait le démarrage).
    public static final AIProvider MOCK = new AIProvider("Mock");
    
    public AIProvider {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("Le fournisseur IA ne peut pas être vide");
        }
    }
    
    public boolean isGemini() {
        return "Gemini".equals(provider);
    }
    
    public boolean isMistral() {
        return "Mistral".equals(provider);
    }
    
    public boolean isMock() {
        return "Mock".equals(provider);
    }
}