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
        
        String to = destinataire(body);
        String subject = texte(body, "subject");
        String content = texte(body, "content");
        
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
        
        String to = destinataire(body);
        String templateId = texte(body, "templateId");
        @SuppressWarnings("unchecked")
        Map<String, Object> variables = carte(body, "variables");
        
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
        
        String to = destinataire(body);
        String type = texte(body, "type");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = carte(body, "data");
        
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
        
        String to = destinataire(body);
        String reminderType = texte(body, "reminderType");
        @SuppressWarnings("unchecked")
        Map<String, Object> details = carte(body, "details");
        
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

    // Balayage de robustesse (2026-11-13) : un champ absent ou d'un autre type (nombre, liste…) provoquait une
    // ClassCastException ou une NullPointerException, donc une erreur 500. Refus 400 précis à la place.
    private static String destinataire(Map<String, Object> body) {
        String to = texte(body, "to");
        if (!to.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || to.length() > 254) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Adresse e-mail du destinataire invalide.");
        }
        return to;
    }

    private static String texte(Map<String, Object> body, String cle) {
        Object v = body == null ? null : body.get(cle);
        if (!(v instanceof String s) || s.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Champ « " + cle + " » obligatoire (texte).");
        }
        if (s.length() > 20_000) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Champ « " + cle + " » trop long.");
        }
        return s.trim();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> carte(Map<String, Object> body, String cle) {
        Object v = body == null ? null : body.get(cle);
        if (v == null) return Map.of();
        if (!(v instanceof Map<?, ?>)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Champ « " + cle + " » : un objet est attendu.");
        }
        return (Map<String, Object>) v;
    }
}
