package ecole_221.sn.ecole.modules.email.infrastructure.web;

import ecole_221.sn.ecole.modules.email.application.port.usecase.EmailUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller pour les opérations d'envoi d'emails
 */
@RestController
@RequestMapping("/email")
@CrossOrigin(origins = "*")
@Tag(name = "Email", description = "Service d'envoi d'emails (Brevo, SMTP, etc.)")
public class EmailController {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailController.class);
    
    private final EmailUseCase emailUseCase;
    
    public EmailController(EmailUseCase emailUseCase) {
        this.emailUseCase = emailUseCase;
    }
    
    @PostMapping("/send")
    @Operation(summary = "Envoyer un email simple", description = "Envoie un email simple à un destinataire")
    public ResponseEntity<Map<String, Object>> sendEmail(@RequestBody Map<String, Object> body) {
        logger.info("POST /email/send - Envoi d'email simple");
        
        String to = (String) body.get("to");
        String subject = (String) body.get("subject");
        String content = (String) body.get("content");
        
        boolean success = emailUseCase.sendStudentNotification(to, subject, content);
        
        Map<String, Object> response = Map.of(
            "success", success,
            "message", success ? "Email envoyé avec succès" : "Échec de l'envoi d'email",
            "provider", emailUseCase.getEmailServiceInfo().get("provider"),
            "available", emailUseCase.isEmailServiceAvailable()
        );
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/send-template")
    @Operation(summary = "Envoyer un email template", description = "Envoie un email en utilisant un template Brevo")
    public ResponseEntity<Map<String, Object>> sendTemplateEmail(@RequestBody Map<String, Object> body) {
        logger.info("POST /email/send-template - Envoi d'email template");
        
        String to = (String) body.get("to");
        String templateId = (String) body.get("templateId");
        @SuppressWarnings("unchecked")
        Map<String, Object> variables = (Map<String, Object>) body.get("variables");
        
        boolean success = emailUseCase.sendTemplateEmail(to, templateId, variables);
        
        Map<String, Object> response = Map.of(
            "success", success,
            "message", success ? "Email template envoyé avec succès" : "Échec de l'envoi d'email template",
            "provider", emailUseCase.getEmailServiceInfo().get("provider"),
            "available", emailUseCase.isEmailServiceAvailable()
        );
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/send-confirmation")
    @Operation(summary = "Envoyer un email de confirmation", description = "Envoie un email de confirmation (inscription, paiement, etc.)")
    public ResponseEntity<Map<String, Object>> sendConfirmationEmail(@RequestBody Map<String, Object> body) {
        logger.info("POST /email/send-confirmation - Envoi d'email de confirmation");
        
        String to = (String) body.get("to");
        String type = (String) body.get("type");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        
        boolean success = emailUseCase.sendConfirmationEmail(to, type, data);
        
        Map<String, Object> response = Map.of(
            "success", success,
            "message", success ? "Email de confirmation envoyé avec succès" : "Échec de l'envoi d'email de confirmation",
            "provider", emailUseCase.getEmailServiceInfo().get("provider"),
            "available", emailUseCase.isEmailServiceAvailable()
        );
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/send-reminder")
    @Operation(summary = "Envoyer un email de rappel", description = "Envoie un email de rappel (devoir, examen, etc.)")
    public ResponseEntity<Map<String, Object>> sendReminderEmail(@RequestBody Map<String, Object> body) {
        logger.info("POST /email/send-reminder - Envoi d'email de rappel");
        
        String to = (String) body.get("to");
        String reminderType = (String) body.get("reminderType");
        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) body.get("details");
        
        boolean success = emailUseCase.sendReminderEmail(to, reminderType, details);
        
        Map<String, Object> response = Map.of(
            "success", success,
            "message", success ? "Email de rappel envoyé avec succès" : "Échec de l'envoi d'email de rappel",
            "provider", emailUseCase.getEmailServiceInfo().get("provider"),
            "available", emailUseCase.isEmailServiceAvailable()
        );
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/status")
    @Operation(summary = "Statut du service email", description = "Retourne les informations sur le service email configuré")
    public ResponseEntity<Map<String, Object>> getEmailStatus() {
        logger.info("GET /email/status - Vérification du statut du service email");
        
        Map<String, Object> status = emailUseCase.getEmailServiceInfo();
        status.put("available", emailUseCase.isEmailServiceAvailable());
        
        return ResponseEntity.ok(status);
    }
}