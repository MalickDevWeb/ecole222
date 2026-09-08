package ecole_221.sn.ecole.modules.email.infrastructure.adapter;

import ecole_221.sn.ecole.modules.email.domain.port.EmailServicePort;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Adapter pour le service email
 * Permet de connecter l'implémentation infrastructure au port domain
 */
@Component
public class EmailServiceAdapter {
    
    private final EmailServicePort emailServicePort;
    
    public EmailServiceAdapter(EmailServicePort emailServicePort) {
        this.emailServicePort = emailServicePort;
    }
    
    public boolean sendEmail(String to, String subject, String content) {
        return emailServicePort.sendEmail(to, subject, content);
    }
    
    public boolean sendEmailWithMetadata(String to, String subject, String content, Map<String, Object> metadata) {
        return emailServicePort.sendEmailWithMetadata(to, subject, content, metadata);
    }
    
    public boolean sendTemplateEmail(String to, String templateId, Map<String, Object> variables) {
        return emailServicePort.sendTemplateEmail(to, templateId, variables);
    }
    
    public boolean isAvailable() {
        return emailServicePort.isAvailable();
    }
    
    public String getProviderName() {
        return emailServicePort.getProviderName();
    }
}