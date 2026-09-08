package ecole_221.sn.ecole.modules.email.domain.model;

import ecole_221.sn.ecole.modules.email.domain.valueobject.EmailFormat;

import java.util.Map;

/**
 * Factory pour créer des instances de EmailRequest.
 * Permet d'accéder au constructeur privé de l'agrégat racine.
 */
public class EmailRequestFactory {
    
    /**
     * Crée une nouvelle requête d'email en format texte.
     */
    public static EmailRequest create(String to, String subject, String content) {
        return new EmailRequest(to, subject, content, EmailFormat.TEXT, Map.of());
    }
    
    /**
     * Crée une nouvelle requête d'email avec un format spécifique.
     */
    public static EmailRequest create(String to, String subject, String content, EmailFormat format) {
        return new EmailRequest(to, subject, content, format, Map.of());
    }
    
    /**
     * Crée une nouvelle requête d'email avec métadonnées.
     */
    public static EmailRequest create(String to, String subject, String content, EmailFormat format, Map<String, Object> metadata) {
        return new EmailRequest(to, subject, content, format, metadata);
    }
    
    /**
     * Crée une nouvelle requête d'email HTML.
     */
    public static EmailRequest createHtml(String to, String subject, String content) {
        return new EmailRequest(to, subject, content, EmailFormat.HTML, Map.of());
    }
    
    /**
     * Crée une nouvelle requête d'email HTML avec métadonnées.
     */
    public static EmailRequest createHtml(String to, String subject, String content, Map<String, Object> metadata) {
        return new EmailRequest(to, subject, content, EmailFormat.HTML, metadata);
    }
}