package ecole_221.sn.ecole.modules.security.infrastructure.web;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ecole_221.sn.ecole.modules.security.application.port.dto.VigilProfileDto;
import ecole_221.sn.ecole.modules.security.application.port.usecase.SecurityUseCase;
import ecole_221.sn.ecole.modules.student.application.port.usecase.StudentUseCase;
import ecole_221.sn.ecole.infrastructure.logging.ActionLogger;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class SecurityController {
    private static final Logger logger = LoggerFactory.getLogger(SecurityController.class);

    private final SecurityUseCase securityUseCase;
    private final StudentUseCase studentUseCase;
    private final ActionLogger actionLogger;
    private volatile Map<String, Object> lastScan = null;

    public SecurityController(SecurityUseCase securityUseCase, StudentUseCase studentUseCase, ActionLogger actionLogger) {
        this.securityUseCase = securityUseCase;
        this.studentUseCase = studentUseCase;
        this.actionLogger = actionLogger;
    }

    // ==================== ENDPOINTS PROFILS DE VIGILANCE ====================

    @GetMapping("/security/vigil-profiles")
    public ResponseEntity<List<VigilProfileDto>> getAllVigilProfiles() {
        logger.info("GET /security/vigil-profiles - Récupération profils de vigilance");
        return ResponseEntity.ok(securityUseCase.getAllVigilProfiles());
    }

    @GetMapping("/security/vigil-profiles/{id}")
    public ResponseEntity<VigilProfileDto> getVigilProfileById(@PathVariable String id) {
        logger.info("GET /security/vigil-profiles/{} - Récupération profil de vigilance", id);
        VigilProfileDto profile = securityUseCase.getVigilProfileById(id);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/security/vigil-profiles/user/{userId}")
    public ResponseEntity<List<VigilProfileDto>> getVigilProfilesByUserId(@PathVariable String userId) {
        logger.info("GET /security/vigil-profiles/user/{} - Récupération profils par utilisateur", userId);
        return ResponseEntity.ok(securityUseCase.getVigilProfilesByUserId(userId));
    }

    @GetMapping("/security/vigil-profiles/role/{role}")
    public ResponseEntity<List<VigilProfileDto>> getVigilProfilesByRole(@PathVariable String role) {
        logger.info("GET /security/vigil-profiles/role/{} - Récupération profils par rôle", role);
        return ResponseEntity.ok(securityUseCase.getVigilProfilesByRole(role));
    }

    @PostMapping("/security/vigil-profiles")
    public ResponseEntity<VigilProfileDto> createVigilProfile(@RequestBody VigilProfileDto profile) {
        logger.info("POST /security/vigil-profiles - Création profil de vigilance");
        return ResponseEntity.ok(securityUseCase.createVigilProfile(profile));
    }

    @PutMapping("/security/vigil-profiles/{id}")
    public ResponseEntity<VigilProfileDto> updateVigilProfile(@PathVariable String id, @RequestBody VigilProfileDto profile) {
        logger.info("PUT /security/vigil-profiles/{} - Mise à jour profil de vigilance", id);
        return ResponseEntity.ok(securityUseCase.updateVigilProfile(id, profile));
    }

    @DeleteMapping("/security/vigil-profiles/{id}")
    public ResponseEntity<Void> deleteVigilProfile(@PathVariable String id) {
        logger.info("DELETE /security/vigil-profiles/{} - Suppression profil de vigilance", id);
        securityUseCase.deleteVigilProfile(id);
        return ResponseEntity.ok().build();
    }

    // ==================== ENDPOINTS OPÉRATIONS DE SÉCURITÉ ====================

    @GetMapping("/security/checkpoint-status")
    public ResponseEntity<Map<String, Object>> getCheckpointStatus() {
        return ResponseEntity.ok(securityUseCase.getCheckpointStatus());
    }

    @PostMapping("/security/scan-logs")
    public ResponseEntity<Map<String, Object>> scanBadge(@RequestBody Map<String, String> body) {
        // Accept badgeId directly, or construct from studentId+checkpointId for QR scan
        String badgeId = body.containsKey("badgeId")
            ? body.get("badgeId")
            : body.getOrDefault("studentId", "") + "@" + body.getOrDefault("checkpointId", "gate-A");
        
        actionLogger.logSecurityAction("BADGE_SCAN", badgeId, "Security Checkpoint", 
            "Scan de badge: " + badgeId);
        
        return ResponseEntity.ok(securityUseCase.processBadgeScan(badgeId));
    }

    @GetMapping("/vigil/check-ins")
    public ResponseEntity<List<Map<String, Object>>> getVigilCheckIns() {
        return ResponseEntity.ok(securityUseCase.getVigilCheckIns());
    }

    @GetMapping("/vigil/profile")
    public ResponseEntity<Map<String, Object>> getVigilProfile() {
        return ResponseEntity.ok(securityUseCase.getVigilProfile("VIG-221"));
    }

    @PostMapping("/vigil/scan")
    public ResponseEntity<Map<String, Object>> scanVigilBadge(@RequestBody Map<String, String> body) {
        String payload = body.getOrDefault("badgeId", "");
        
        boolean isValid = false;
        String matricule = "Inconnu";
        String message = "Format de badge invalide ou non reconnu.";
        
        if (payload.startsWith("STU_SECURE:")) {
            String[] parts = payload.split(":");
            if (parts.length == 4) {
                matricule = parts[1];
                try {
                    long minuteEpoch = Long.parseLong(parts[2]);
                    isValid = studentUseCase.validateQrToken(matricule, minuteEpoch, parts[3]);
                    message = isValid
                        ? "Badge étudiant vérifié avec succès. Jeton cryptographique valide."
                        : "Token QR expiré ou invalide.";
                } catch (NumberFormatException ex) {
                    message = "Minute du badge invalide.";
                }
            }
        } else if (payload.startsWith("SN-")) {
            matricule = payload;
            isValid = true;
            message = "Saisie manuelle du matricule validée.";
        } else if (payload.startsWith("badge-")) {
            matricule = payload;
            isValid = true;
            message = "Ancien format de badge accepté.";
        }

        Map<String, Object> result = new HashMap<>();
        result.put("badgeOwner", isValid ? "Étudiant " + matricule : "Inconnu");
        result.put("studentId", matricule);
        result.put("statut", isValid ? "Autorisé" : "Refusé");
        result.put("message", message);
        result.put("assiduite", "95%");
        result.put("statutFrais", isValid ? "À Jour" : "Inconnu");
        result.put("zone", "Portail Principal");
        result.put("time", LocalDateTime.now().toString());
        
        actionLogger.logSecurityAction("VIGIL_SCAN", matricule, "Portail Principal", 
            "Scan vigil: " + (isValid ? "AUTORISÉ" : "REFUSÉ") + " - " + message);
        
        lastScan = result;
        if (isValid) {
            securityUseCase.processBadgeScan(matricule);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/vigil/last-scan")
    public ResponseEntity<Map<String, Object>> getLastScan() {
        if (lastScan == null) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("statut", "Refusé");
            empty.put("message", "Aucun scan récent");
            return ResponseEntity.ok(empty);
        }
        return ResponseEntity.ok(lastScan);
    }
}
