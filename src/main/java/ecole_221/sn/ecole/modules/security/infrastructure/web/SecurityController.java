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
import java.util.concurrent.ConcurrentHashMap;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;

@RestController
public class SecurityController {
    private static final Logger logger = LoggerFactory.getLogger(SecurityController.class);

    private final SecurityUseCase securityUseCase;
    private final StudentUseCase studentUseCase;
    private final ActionLogger actionLogger;
    /** Dernier scan PAR ÉCOLE (avant : un seul champ partagé entre toutes les écoles). */
    private final Map<String, Map<String, Object>> derniersScans = new ConcurrentHashMap<>();

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
        // Profil du vigile CONNECTÉ (avant : profil « VIG-221 » pour tout le monde)
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof ecole_221.sn.ecole.modules.auth.domain.model.User u)) {
            return ResponseEntity.status(401).build();
        }
        Map<String, Object> profil = new java.util.HashMap<>(securityUseCase.getVigilProfile(u.getId()));
        if ("non_configuré".equals(profil.get("statut"))) {
            profil.put("nom", u.getNom());
            profil.put("prenom", u.getPrenom());
        }
        return ResponseEntity.ok(profil);
    }

    @PostMapping("/vigil/scan")
    public ResponseEntity<Map<String, Object>> scanVigilBadge(@RequestBody Map<String, String> body) {
        // Plus d'acceptation « à l'aveugle » des préfixes SN-/badge- ni d'assiduité / statut de frais inventés :
        // le badge est résolu par SecurityService.processBadgeScan vers un compte RÉEL membre de l'école courante.
        String payload = body == null || body.get("badgeId") == null ? "" : body.get("badgeId").trim();
        String identifiant = payload;
        Map<String, Object> result = new HashMap<>();
        boolean isValid;
        String message;
        String statutFrais = null;
        String owner = "Inconnu";

        boolean jetonRefuse = false;
        if (payload.startsWith("STU_SECURE:")) {
            String[] parts = payload.split(":");
            identifiant = parts.length > 1 ? parts[1] : "";
            if (parts.length != 4) {
                jetonRefuse = true;
            } else {
                try {
                    jetonRefuse = !studentUseCase.validateQrToken(parts[1], Long.parseLong(parts[2]), parts[3]);
                } catch (NumberFormatException ex) {
                    jetonRefuse = true;
                }
            }
        }

        if (payload.isEmpty()) {
            isValid = false;
            message = "Badge vide : aucun identifiant scanné.";
        } else if (jetonRefuse) {
            isValid = false;
            message = "Token QR expiré ou invalide.";
        } else {
            Map<String, Object> scan = securityUseCase.processBadgeScan(identifiant);
            isValid = Boolean.TRUE.equals(scan.get("accessGranted"));
            message = String.valueOf(scan.getOrDefault("message", ""));
            Object frais = scan.get("statutFrais");
            statutFrais = frais == null ? null : frais.toString();
            Object nom = scan.get("studentName");
            owner = nom == null || nom.toString().isBlank() ? "Inconnu" : nom.toString();
        }

        result.put("badgeOwner", owner);
        result.put("studentId", identifiant.isEmpty() ? null : identifiant);
        result.put("statut", isValid ? "Autorisé" : "Refusé");
        result.put("message", message);
        result.put("assiduite", null);          // aucune source d'assiduité fiable ici : pas de valeur inventée
        result.put("statutFrais", statutFrais); // null si inconnu ; « Non renseigné » si jamais saisi
        result.put("zone", null);               // aucun point de passage configuré n'est transmis par le scanner
        result.put("time", LocalDateTime.now().toString());

        actionLogger.logSecurityAction("VIGIL_SCAN", identifiant, "Portail",
            "Scan vigil: " + (isValid ? "AUTORISÉ" : "REFUSÉ") + " - " + message);

        derniersScans.put(ecoleCourante(), result);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/vigil/last-scan")
    public ResponseEntity<Map<String, Object>> getLastScan() {
        Map<String, Object> dernier = derniersScans.get(ecoleCourante());
        if (dernier == null) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("statut", "Refusé");
            empty.put("message", "Aucun scan récent");
            return ResponseEntity.ok(empty);
        }
        return ResponseEntity.ok(dernier);
    }

    private static String ecoleCourante() {
        return TenantContext.getCurrentOrganizationId().orElse("sans-ecole");
    }
}
