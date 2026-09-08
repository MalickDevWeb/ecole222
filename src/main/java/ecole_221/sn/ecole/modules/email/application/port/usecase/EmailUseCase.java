package ecole_221.sn.ecole.modules.email.application.port.usecase;

import java.util.Map;

/**
 * Use case pour les opérations d'envoi d'emails (Domain Layer)
 * Définit les cas d'utilisation métier pour les services d'emails
 */
public interface EmailUseCase {
    
    /**
     * Envoie une notification à un étudiant
     * @param studentEmail L'email de l'étudiant
     * @param subject Le sujet de la notification
     * @param content Le contenu de la notification
     * @return true si l'envoi a réussi
     */
    boolean sendStudentNotification(String studentEmail, String subject, String content);
    
    /**
     * Envoie un email de confirmation
     * @param to L'adresse email du destinataire
     * @param type Le type de confirmation (inscription, paiement, etc.)
     * @param data Les données à inclure
     * @return true si l'envoi a réussi
     */
    boolean sendConfirmationEmail(String to, String type, Map<String, Object> data);
    
    /**
     * Envoie un email de rappel
     * @param to L'adresse email du destinataire
     * @param reminderType Le type de rappel (devoir, examen, etc.)
     * @param details Les détails du rappel
     * @return true si l'envoi a réussi
     */
    boolean sendReminderEmail(String to, String reminderType, Map<String, Object> details);
    
    /**
     * Envoie un email en utilisant un template
     * @param to L'adresse email du destinataire
     * @param templateId L'identifiant du template
     * @param variables Les variables à injecter
     * @return true si l'envoi a réussi
     */
    boolean sendTemplateEmail(String to, String templateId, Map<String, Object> variables);
    
    /**
     * Vérifie si le service email est disponible
     * @return true si disponible
     */
    boolean isEmailServiceAvailable();
    
    /**
     * Obtient des informations sur le fournisseur email
     * @return Informations sur le provider
     */
    Map<String, Object> getEmailServiceInfo();
}