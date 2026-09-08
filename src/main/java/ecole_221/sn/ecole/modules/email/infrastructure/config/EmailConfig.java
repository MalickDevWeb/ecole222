package ecole_221.sn.ecole.modules.email.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Configuration pour les services d'envoi d'emails (Brevo, SMTP, etc.)
 * Permet de basculer facilement entre différents fournisseurs d'emails
 */
@Configuration
@ConditionalOnProperty(name = "email.provider", havingValue = "brevo", matchIfMissing = true)
public class EmailConfig {
    
    @Value("${email.brevo.api-key:}")
    private String brevoApiKey;
    
    @Value("${email.brevo.sender-email:noreply@ecole221.sn}")
    private String senderEmail;
    
    @Value("${email.brevo.sender-name:École 221}")
    private String senderName;
    
    @Value("${email.smtp.host:smtp-relay.brevo.com}")
    private String smtpHost;
    
    @Value("${email.smtp.port:587}")
    private int smtpPort;
    
    @Value("${email.smtp.username:}")
    private String smtpUsername;
    
    @Value("${email.smtp.password:}")
    private String smtpPassword;
    
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(smtpHost);
        mailSender.setPort(smtpPort);
        mailSender.setUsername(smtpUsername);
        mailSender.setPassword(smtpPassword);
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", "*");
        props.put("mail.debug", "false");
        
        return mailSender;
    }
    
    @Bean
    public EmailServiceProperties emailServiceProperties() {
        return new EmailServiceProperties(
            "brevo",
            brevoApiKey,
            senderEmail,
            senderName,
            smtpHost,
            smtpPort,
            smtpUsername,
            smtpPassword
        );
    }
    
    public record EmailServiceProperties(
        String provider,
        String apiKey,
        String senderEmail,
        String senderName,
        String smtpHost,
        int smtpPort,
        String smtpUsername,
        String smtpPassword
    ) {
        public boolean isConfigured() {
            return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("your-brevo-api-key");
        }
        
        public boolean isSmtpConfigured() {
            return smtpUsername != null && !smtpUsername.isEmpty() && 
                   smtpPassword != null && !smtpPassword.isEmpty();
        }
    }
}