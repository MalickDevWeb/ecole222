package ecole_221.sn.ecole.modules.ai.domain.port;

import java.util.Map;

/**
 * Port d'interface pour les services IA (abstraction pour changer de fournisseur)
 * Permet de changer facilement entre Gemini, OpenAI, Claude, etc.
 */
public interface AIServicePort {
    
    /**
     * Génère une réponse à partir d'un message utilisateur
     * @param message Le message de l'utilisateur
     * @param context Le contexte optionnel (historique, métadonnées, etc.)
     * @return La réponse générée par l'IA
     */
    String generateResponse(String message, Map<String, Object> context);
    
    /**
     * Génère une réponse avec un modèle spécifique
     * @param message Le message de l'utilisateur
     * @param model Le modèle à utiliser (ex: "gemini-pro", "gemini-flash")
     * @param context Le contexte optionnel
     * @return La réponse générée
     */
    String generateResponseWithModel(String message, String model, Map<String, Object> context);
    
    /**
     * Vérifie si le service IA est disponible
     * @return true si le service est opérationnel
     */
    boolean isAvailable();
    
    /**
     * Obtient le nom du fournisseur IA configuré
     * @return Le nom du fournisseur (ex: "Gemini", "OpenAI", "Mock")
     */
    String getProviderName();
}