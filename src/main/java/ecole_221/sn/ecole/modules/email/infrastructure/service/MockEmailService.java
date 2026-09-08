package ecole_221.sn.ecole.modules.email.infrastructure.service;

import ecole_221.sn.ecole.modules.email.domain.port.EmailServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service email mock pour les tests et le développement
 * Simule l'envoi d'emails sans nécessiter de configuration SMTP/API
 */
@Service
@ConditionalOnProperty(name = "email.provider", havingValue = "mock")
public class MockEmailService implements EmailServicePort {
    
    private static final Logger logger = LoggerFactory.getLogger(MockEmailService.class);
    
    @Override
    public boolean sendEmail(String to, String subject, String content) {
        return sendEmailWithMetadata(to, subject, content, Map.of());
    }
    
    @Override
    public boolean sendEmailWithMetadata(String to, String subject, String content, Map<String, Object> metadata) {
        logger.info("Mock Email Service - Envoi d'email à: {} | Sujet: {}", to, subject);
        logger.info("Contenu: {}", content.substring(0, Math.min(100, content.length())));
        logger.info("Métadonnées: {}", metadata);
        
        // Simulation d'envoi réussi
        return true;
    }
    
    @Override
    public boolean sendTemplateEmail(String to, String templateId, Map<String, Object> variables) {
        logger.info("Mock Email Service - Envoi d'email template {} à: {}", templateId, to);
        logger.info("Variables: {}", variables);
        
        // Simulation d'envoi réussi
        return true;
    }
    
    @Override
    public boolean isAvailable() {
        return true;
    }
    
    @Override
    public String getProviderName() {
        return "Mock Email";
    }
}