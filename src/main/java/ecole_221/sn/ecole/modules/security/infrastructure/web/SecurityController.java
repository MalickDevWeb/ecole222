package ecole_221.sn.ecole.modules.security.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;import org.slf4j.Logger;
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

    /** Terminal affecté au vigile qui scanne (APP-BE-21) : refus si désactivé ou révoqué, point de passage sinon. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.modules.security.application.service.TerminauxAccesService terminauxAcces;
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

    @Operation(summary = "État du poste de contrôle", description = "Porte et gardien affectés (« Non configuré » / « Non assigné » s'ils ne le sont pas) et nombre de scans du jour dans l'école.")
    @GetMapping("/security/checkpoint-status")
    public ResponseEntity<Map<String, Object>> getCheckpointStatus() {
        return ResponseEntity.ok(securityUseCase.getCheckpointStatus());
    }

    @Operation(summary = "Liste d'accès hors ligne (EF-10)", description = "Clé publique des badges, élèves actifs (statut des frais) et personnel de l'école : permet au terminal du vigile de décider sans réseau. Valable 24 h.")
    @GetMapping("/vigil/acces-hors-ligne")
    public ResponseEntity<Map<String, Object>> accesHorsLigne() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (terminauxAcces != null && auth != null && auth.getPrincipal() instanceof ecole_221.sn.ecole.modules.auth.domain.model.User vigile) {
            terminauxAcces.exigerTerminalUtilisable(vigile.getId());   // terminal désactivé : pas de liste
        }
        return ResponseEntity.ok(securityUseCase.listeAccesHorsLigne());
    }

    /** Scans faits hors ligne, transmis au retour du réseau (EF-10) : au plus 200, datés des 72 dernières heures. */
    @Operation(summary = "Transmettre les scans faits hors ligne (EF-10)", description = "Corps {scans: [{badgeId, scanneLe (ISO-8601), decision: Autorisé|Refusé}]} ; le serveur refait le contrôle à la minute du scan et signale les décisions contestées.")
    @PostMapping("/vigil/scans-hors-ligne")
    public ResponseEntity<Map<String, Object>> scansHorsLigne(@RequestBody(required = false) Map<String, Object> corps) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (terminauxAcces != null && auth != null && auth.getPrincipal() instanceof ecole_221.sn.ecole.modules.auth.domain.model.User vigile) {
            terminauxAcces.exigerTerminalUtilisable(vigile.getId());
        }
        Object brut = corps == null ? null : corps.get("scans");
        if (!(brut instanceof List<?> scans) || scans.isEmpty()) {
            throw ecole_221.sn.ecole.partage.ChampsRequete.refus("La liste « scans » est obligatoire.");
        }
        if (scans.size() > 200) {
            throw ecole_221.sn.ecole.partage.ChampsRequete.refus("Au plus 200 scans par envoi.");
        }
        java.time.Instant maintenant = java.time.Instant.now();
        List<Map<String, Object>> resultats = new java.util.ArrayList<>();
        int contestes = 0;
        for (Object o : scans) {
            Map<String, Object> r = new HashMap<>();
            if (!(o instanceof Map<?, ?> m) || !(m.get("badgeId") instanceof String badge) || badge.isBlank() || badge.length() > 500
                    || !(m.get("scanneLe") instanceof String quand) || !(m.get("decision") instanceof String decision)) {
                r.put("erreur", "Scan incomplet : badgeId, scanneLe et decision sont obligatoires.");
                resultats.add(r);
                continue;
            }
            java.time.Instant scanneLe;
            try {
                scanneLe = java.time.Instant.parse(quand);
            } catch (java.time.format.DateTimeParseException e) {
                r.put("erreur", "Date du scan invalide : " + quand + ".");
                resultats.add(r);
                continue;
            }
            if (scanneLe.isAfter(maintenant.plusSeconds(300)) || scanneLe.isBefore(maintenant.minus(java.time.Duration.ofHours(72)))) {
                r.put("erreur", "Date du scan hors des 72 dernières heures : " + quand + ".");
                resultats.add(r);
                continue;
            }
            Map<String, Object> log = securityUseCase.enregistrerScanHorsLigne(badge.trim(), scanneLe, "Autorisé".equals(decision));
            if (Boolean.TRUE.equals(log.get("conteste"))) contestes++;
            resultats.add(log);
        }
        return ResponseEntity.ok(Map.of("enregistres", resultats.stream().filter(x -> !x.containsKey("erreur")).count(),
                "contestes", contestes, "resultats", resultats));
    }

    @Operation(summary = "Journaliser un scan de badge", description = "Corps {badgeId} ou {studentId, checkpointId?}. 403 si le terminal du vigile est désactivé ou révoqué ; scan vide → 400.")
    @PostMapping("/security/scan-logs")
    public ResponseEntity<Map<String, Object>> scanBadge(@RequestBody Map<String, String> body) {
        // APP-BE-21 : terminal du vigile connecté — 403 s'il a été désactivé ou révoqué par l'administration
        var authLog = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (terminauxAcces != null && authLog != null
                && authLog.getPrincipal() instanceof ecole_221.sn.ecole.modules.auth.domain.model.User vigileLog) {
            terminauxAcces.exigerTerminalUtilisable(vigileLog.getId());
        }
        // APP-QA-04 : badge ou élève obligatoire (avant : un scan vide était journalisé comme « @gate-A »)
        if (body == null || ((body.get("badgeId") == null || body.get("badgeId").isBlank())
                && (body.get("studentId") == null || body.get("studentId").isBlank()))) {
            throw ecole_221.sn.ecole.partage.ChampsRequete.refus("Badge ou élève obligatoire : aucun identifiant scanné.");
        }
        if ((body.get("badgeId") != null && body.get("badgeId").length() > 500) || (body.get("studentId") != null && body.get("studentId").length() > 255)) {
            throw ecole_221.sn.ecole.partage.ChampsRequete.refus("Identifiant scanné trop long.");
        }
        // Accept badgeId directly, or construct from studentId+checkpointId for QR scan
        String badgeId = body.get("badgeId") != null && !body.get("badgeId").isBlank()
            ? body.get("badgeId")
            : body.getOrDefault("studentId", "") + "@" + body.getOrDefault("checkpointId", "gate-A");
        
        actionLogger.logSecurityAction("BADGE_SCAN", badgeId, "Security Checkpoint", 
            "Scan de badge: " + badgeId);
        
        return ResponseEntity.ok(securityUseCase.processBadgeScan(badgeId));
    }

    @Operation(summary = "Journal des passages", description = "Scans enregistrés dans l'école courante.")
    @GetMapping("/vigil/check-ins")
    public ResponseEntity<List<Map<String, Object>>> getVigilCheckIns() {
        return ResponseEntity.ok(securityUseCase.getVigilCheckIns());
    }

    @Operation(summary = "Profil du vigile connecté", description = "Profil enregistré du vigile ; à défaut, son nom et statut « non_configuré ».")
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

    @Operation(summary = "Scanner un badge ou un QR", description = "Corps {badgeId} (badge ou QR STU_SECURE signé). Résout le badge vers un élève réel de l'école ; renvoie Autorisé/Refusé, motif, statut des frais et terminal du vigile. 403 si ce terminal est désactivé ou révoqué.")
    @PostMapping("/vigil/scan")
    public ResponseEntity<Map<String, Object>> scanVigilBadge(@RequestBody Map<String, String> body) {
        // Plus d'acceptation « à l'aveugle » des préfixes SN-/badge- ni d'assiduité / statut de frais inventés :
        // le badge est résolu par SecurityService.processBadgeScan vers un compte RÉEL membre de l'école courante.
        // APP-BE-21 : terminal du vigile connecté — 403 s'il a été désactivé ou révoqué par l'administration
        String pointDePassage = null;
        var authScan = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (terminauxAcces != null && authScan != null
                && authScan.getPrincipal() instanceof ecole_221.sn.ecole.modules.auth.domain.model.User vigile) {
            pointDePassage = terminauxAcces.exigerTerminalUtilisable(vigile.getId()).orElse(null);
        }
        String payload = body == null || body.get("badgeId") == null ? "" : body.get("badgeId").trim();
        String identifiant = payload;
        Map<String, Object> result = new HashMap<>();
        boolean isValid;
        String message;
        String owner = "Inconnu";

        // 2026-11-13 : le badge QR dynamique (STU_SECURE:…) est vérifié par processBadgeScan, commun à toutes les
        // routes de scan ; on lui transmet donc le contenu scanné tel quel.
        if (payload.startsWith("STU_SECURE:")) {
            String[] parts = payload.split(":");
            identifiant = parts.length > 1 ? parts[1] : "";
        }

        if (payload.isEmpty()) {
            isValid = false;
            message = "Badge vide : aucun identifiant scanné.";
        } else {
            Map<String, Object> scan = securityUseCase.processBadgeScan(payload);
            isValid = Boolean.TRUE.equals(scan.get("accessGranted"));
            message = String.valueOf(scan.getOrDefault("message", ""));
            Object nom = scan.get("studentName");
            owner = nom == null || nom.toString().isBlank() ? "Inconnu" : nom.toString();
        }

        result.put("badgeOwner", owner);
        result.put("studentId", identifiant.isEmpty() ? null : identifiant);
        result.put("statut", isValid ? "Autorisé" : "Refusé");
        result.put("message", message);
        result.put("assiduite", null);          // aucune source d'assiduité fiable ici : pas de valeur inventée
        // APP-TL-22 : plus de statut de frais dans la réponse du scan (le vigile n'a pas à connaître la situation
        // financière d'une famille ; l'accès n'en dépend plus)
        result.put("zone", pointDePassage);     // terminal affecté au vigile ; null s'il n'en a aucun (jamais inventé)
        result.put("time", LocalDateTime.now().toString());

        actionLogger.logSecurityAction("VIGIL_SCAN", identifiant, pointDePassage != null ? pointDePassage : "Sans terminal",
            "Scan vigil: " + (isValid ? "AUTORISÉ" : "REFUSÉ") + " - " + message);

        derniersScans.put(ecoleCourante(), result);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Dernier scan de l'école", description = "Dernier résultat de scan de l'école courante (« Aucun scan récent » sinon).")
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
