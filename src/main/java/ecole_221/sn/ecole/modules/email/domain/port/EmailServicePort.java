package ecole_221.sn.ecole.modules.email.domain.port;

import java.util.Map;

/**
 * Port d'interface pour les services d'envoi d'emails (abstraction pour changer de fournisseur)
 * Permet de changer facilement entre Brevo, SendGrid, Mailgun, SMTP, etc.
 */
public interface EmailServicePort {
    
    /**
     * Envoie un email simple
     * @param to L'adresse email du destinataire
     * @param subject Le sujet de l'email
     * @param content Le contenu de l'email (HTML ou texte)
     * @return true si l'envoi a réussi
     */
    boolean sendEmail(String to, String subject, String content);
    
    /**
     * Envoie un email avec des métadonnées
     * @param to L'adresse email du destinataire
     * @param subject Le sujet de l'email
     * @param content Le contenu de l'email
     * @param metadata Les métadonnées optionnelles (template, variables, etc.)
     * @return true si l'envoi a réussi
     */
    boolean sendEmailWithMetadata(String to, String subject, String content, Map<String, Object> metadata);
    
    /**
     * Envoie un email en utilisant un template
     * @param to L'adresse email du destinataire
     * @param templateId L'identifiant du template à utiliser
     * @param variables Les variables à injecter dans le template
     * @return true si l'envoi a réussi
     */
    boolean sendTemplateEmail(String to, String templateId, Map<String, Object> variables);
    
    /**
     * Vérifie si le service email est disponible
     * @return true si le service est opérationnel
     */
    boolean isAvailable();
    
    /**
     * Obtient le nom du fournisseur email configuré
     * @return Le nom du fournisseur (ex: "Brevo", "SendGrid", "SMTP", "Mock")
     */
    String getProviderName();
}