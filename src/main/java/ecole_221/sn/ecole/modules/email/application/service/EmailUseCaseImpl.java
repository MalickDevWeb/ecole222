package ecole_221.sn.ecole.modules.email.application.service;

import ecole_221.sn.ecole.modules.email.application.port.usecase.EmailUseCase;
import ecole_221.sn.ecole.modules.email.domain.port.EmailServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Implémentation du use case pour les opérations d'emails
 * Coordonne les appels au service email infrastructure
 */
@Service
@org.springframework.context.annotation.Primary
public class EmailUseCaseImpl implements EmailUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailUseCaseImpl.class);
    
    private final EmailServicePort emailServicePort;
    
    @Autowired
    public EmailUseCaseImpl(EmailServicePort emailServicePort) {
        this.emailServicePort = emailServicePort;
        logger.info("EmailUseCaseImpl initialisé");
    }
    
    @Override
    public boolean sendStudentNotification(String studentEmail, String subject, String content) {
        logger.info("Envoi de notification à l'étudiant: {}", studentEmail);
        return emailServicePort.sendEmail(studentEmail, subject, content);
    }
    
    @Override
    public boolean sendConfirmationEmail(String to, String type, Map<String, Object> data) {
        logger.info("Envoi d'email de confirmation - Type: {}, Destinataire: {}", type, to);
        
        String subject = buildConfirmationSubject(type);
        String content = buildConfirmationContent(type, data);
        
        return emailServicePort.sendEmailWithMetadata(to, subject, content, data);
    }
    
    @Override
    public boolean sendReminderEmail(String to, String reminderType, Map<String, Object> details) {
        logger.info("Envoi d'email de rappel - Type: {}, Destinataire: {}", reminderType, to);
        
        String subject = buildReminderSubject(reminderType);
        String content = buildReminderContent(reminderType, details);
        
        return emailServicePort.sendEmailWithMetadata(to, subject, content, details);
    }
    
    @Override
    public boolean sendTemplateEmail(String to, String templateId, Map<String, Object> variables) {
        logger.info("Envoi d'email template - Template: {}, Destinataire: {}", templateId, to);
        return emailServicePort.sendTemplateEmail(to, templateId, variables);
    }
    
    @Override
    public boolean isEmailServiceAvailable() {
        return emailServicePort.isAvailable();
    }
    
    @Override
    public Map<String, Object> getEmailServiceInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("provider", emailServicePort.getProviderName());
        info.put("available", emailServicePort.isAvailable());
        return info;
    }
    
    private String buildConfirmationSubject(String type) {
        return switch (type.toLowerCase()) {
            case "inscription" -> "Confirmation d'inscription - École 221";
            case "paiement" -> "Confirmation de paiement - École 221";
            case "inscription_cours" -> "Confirmation d'inscription au cours - École 221";
            default -> "Confirmation - École 221";
        };
    }
    
    private String buildConfirmationContent(String type, Map<String, Object> data) {
        StringBuilder content = new StringBuilder();
        content.append("<h1>").append(buildConfirmationSubject(type)).append("</h1>");
        content.append("<p>Bonjour,</p>");
        content.append("<p>Votre ").append(type).append(" a été traitée avec succès.</p>");
        
        if (data != null && !data.isEmpty()) {
            content.append("<h3>Détails:</h3>");
            content.append("<ul>");
            data.forEach((key, value) -> {
                content.append("<li><strong>").append(key).append(":</strong> ").append(value).append("</li>");
            });
            content.append("</ul>");
        }
        
        content.append("<p>Cordialement,<br>L'équipe École 221</p>");
        return content.toString();
    }
    
    private String buildReminderSubject(String reminderType) {
        return switch (reminderType.toLowerCase()) {
            case "devoir" -> "Rappel de devoir - École 221";
            case "examen" -> "Rappel d'examen - École 221";
            case "cours" -> "Rappel de cours - École 221";
            case "paiement" -> "Rappel de paiement - École 221";
            default -> "Rappel - École 221";
        };
    }
    
    private String buildReminderContent(String reminderType, Map<String, Object> details) {
        StringBuilder content = new StringBuilder();
        content.append("<h1>").append(buildReminderSubject(reminderType)).append("</h1>");
        content.append("<p>Bonjour,</p>");
        content.append("<p>Ceci est un rappel concernant: ").append(reminderType).append("</p>");
        
        if (details != null && !details.isEmpty()) {
            content.append("<h3>Détails:</h3>");
            content.append("<ul>");
            details.forEach((key, value) -> {
                content.append("<li><strong>").append(key).append(":</strong> ").append(value).append("</li>");
            });
            content.append("</ul>");
        }
        
        content.append("<p>Veillez à compléter cette action dans les délais impartis.</p>");
        content.append("<p>Cordialement,<br>L'équipe École 221</p>");
        return content.toString();
    }
}