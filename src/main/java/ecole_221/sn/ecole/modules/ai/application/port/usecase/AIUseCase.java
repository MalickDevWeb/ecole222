package ecole_221.sn.ecole.modules.ai.application.port.usecase;

import java.util.Map;

/**
 * Use case pour les opérations IA (Domain Layer)
 * Définit les cas d'utilisation métier pour les services IA
 */
public interface AIUseCase {
    
    /**
     * Génère une réponse pour le tuteur IA
     * @param message Le message de l'étudiant
     * @return La réponse générée
     */
    String generateTutorResponse(String message);
    
    /**
     * Génère une réponse pour le tuteur IA (alias)
     * @param message Le message de l'étudiant
     * @return La réponse générée
     */
    String getTutorResponse(String message);
    
    /**
     * Génère une réponse avec contexte
     * @param message Le message de l'étudiant
     * @param context Le contexte (cours, matière, etc.)
     * @return La réponse générée
     */
    String generateTutorResponseWithContext(String message, Map<String, Object> context);
    
    /**
     * Génère du contenu pédagogique
     * @param topic Le sujet
     * @param type Le type de contenu (quiz, résumé, exercice, etc.)
     * @return Le contenu généré
     */
    String generateEducationalContent(String topic, String type);
    
    /**
     * Vérifie si le service IA est disponible
     * @return true si disponible
     */
    boolean isAIServiceAvailable();
    
    /**
     * Obtient des informations sur le fournisseur IA
     * @return Informations sur le provider
     */
    Map<String, Object> getAIServiceInfo();
}