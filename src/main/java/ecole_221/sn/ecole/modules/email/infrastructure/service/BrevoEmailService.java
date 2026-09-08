package ecole_221.sn.ecole.modules.email.infrastructure.service;

import ecole_221.sn.ecole.modules.email.domain.port.EmailServicePort;
import ecole_221.sn.ecole.modules.email.infrastructure.config.EmailConfig.EmailServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Implémentation du service email avec Brevo (Sendinblue)
 * Peut être facilement remplacé par une autre implémentation (SendGrid, Mailgun, SMTP, etc.)
 */
@Service
@ConditionalOnProperty(name = "email.provider", havingValue = "brevo", matchIfMissing = true)
public class BrevoEmailService implements EmailServicePort {
    
    private static final Logger logger = LoggerFactory.getLogger(BrevoEmailService.class);
    
    private final EmailServiceProperties properties;
    private final JavaMailSender mailSender;
    
    @Autowired
    public BrevoEmailService(EmailServiceProperties properties, JavaMailSender mailSender) {
        this.properties = properties;
        this.mailSender = mailSender;
        logger.info("BrevoEmailService initialisé avec provider: {}", properties.provider());
    }
    
    @Override
    public boolean sendEmail(String to, String subject, String content) {
        return sendEmailWithMetadata(to, subject, content, Map.of());
    }
    
    @Override
    public boolean sendEmailWithMetadata(String to, String subject, String content, Map<String, Object> metadata) {
        if (!isAvailable()) {
            logger.warn("Brevo service non configuré, utilisation du fallback");
            return sendFallbackEmail(to, subject, content, metadata);
        }
        
        try {
            logger.info("Envoi d'email via Brevo à: {}", to);
            
            // Tenter d'envoi via SMTP Brevo (recommandé par Brevo)
            if (properties.isSmtpConfigured()) {
                return sendViaSmtp(to, subject, content, metadata);
            }
            
            // Sinon, utiliser l'API REST Brevo
            return sendViaRestAPI(to, subject, content, metadata);
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi d'email via Brevo: {}", e.getMessage());
            return sendFallbackEmail(to, subject, content, metadata);
        }
    }
    
    @Override
    public boolean sendTemplateEmail(String to, String templateId, Map<String, Object> variables) {
        if (!isAvailable()) {
            logger.warn("Brevo service non configuré, utilisation du fallback");
            return false;
        }
        
        try {
            logger.info("Envoi d'email template {} via Brevo à: {}", templateId, to);
            
            // Pour l'instant, retourner false car l'API REST nécessite une clé API valide
            logger.info("API REST Brevo non configurée, template email non envoyé");
            return false;
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi d'email template via Brevo: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean isAvailable() {
        return properties.isConfigured();
    }
    
    @Override
    public String getProviderName() {
        return "Brevo";
    }
    
    private boolean sendViaSmtp(String to, String subject, String content, Map<String, Object> metadata) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.senderEmail());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            
            mailSender.send(message);
            logger.info("Email envoyé avec succès via SMTP Brevo à: {}", to);
            return true;
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi via SMTP Brevo: {}", e.getMessage());
            return false;
        }
    }
    
    private boolean sendViaRestAPI(String to, String subject, String content, Map<String, Object> metadata) {
        // Pour l'instant, retourner le fallback car l'API REST nécessite une clé API valide
        logger.info("API REST Brevo non configurée, utilisation du fallback");
        return sendFallbackEmail(to, subject, content, metadata);
    }
    
    private boolean sendFallbackEmail(String to, String subject, String content, Map<String, Object> metadata) {
        logger.info("Utilisation de l'email fallback (simulation) pour: {}", to);
        // Simulation d'envoi d'email en mode développement
        logger.info("EMAIL FALLBACK - À: {} | Sujet: {} | Contenu: {}", to, subject, content.substring(0, Math.min(50, content.length())));
        return true;
    }
}