package ecole_221.sn.ecole.modules.ai.domain.valueobject;

public record AIModel(String model) {
    public static final AIModel GEMINI_PRO = new AIModel("gemini-pro");
    public static final AIModel GEMINI_FLASH = new AIModel("gemini-flash");
    public static final AIModel MISTRAL_MEDIUM = new AIModel("mistral-medium");
    public static final AIModel MISTRAL_SMALL = new AIModel("mistral-small");
    
    public AIModel {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Le modèle IA ne peut pas être vide");
        }
    }
    
    public boolean isGemini() {
        return model.startsWith("gemini-");
    }
    
    public boolean isMistral() {
        return model.startsWith("mistral-");
    }
    
    public boolean isPro() {
        return model.contains("pro");
    }
    
    public boolean isFlash() {
        return model.contains("flash");
    }
}