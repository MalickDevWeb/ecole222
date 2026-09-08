package ecole_221.sn.ecole.modules.audit.infrastructure.web;

import ecole_221.sn.ecole.modules.audit.application.port.dto.AuditLogDto;
import ecole_221.sn.ecole.modules.audit.application.port.dto.SecurityEventDto;
import ecole_221.sn.ecole.modules.audit.application.port.usecase.AuditUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/audit")
@CrossOrigin(origins = "*")
@Tag(name = "Audit", description = "Journal d'audit et événements de sécurité")
public class AuditController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditController.class);
    private final AuditUseCase auditUseCase;
    
    public AuditController(AuditUseCase auditUseCase) {
        this.auditUseCase = auditUseCase;
    }
    
    // ==================== ENDPOINTS LOGS D'AUDIT ====================
    
    @GetMapping("/logs")
    @Operation(summary = "Liste des logs d'audit", description = "Retourne tous les logs d'audit")
    public ResponseEntity<List<AuditLogDto>> getAllAuditLogs() {
        logger.info("GET /audit/logs - Récupération logs d'audit");
        return ResponseEntity.ok(auditUseCase.getAllAuditLogs());
    }
    
    @GetMapping("/logs/recent")
    @Operation(summary = "Logs d'audit récents", description = "Retourne les logs d'audit récents")
    public ResponseEntity<List<AuditLogDto>> getRecentAuditLogs(@RequestParam(defaultValue = "50") int limit) {
        logger.info("GET /audit/logs/recent - Récupération logs d'audit récents");
        return ResponseEntity.ok(auditUseCase.getRecentAuditLogs(limit));
    }
    
    @GetMapping("/logs/actor/{actorId}")
    @Operation(summary = "Logs par acteur", description = "Retourne les logs d'audit par acteur")
    public ResponseEntity<List<AuditLogDto>> getAuditLogsByActor(@PathVariable String actorId) {
        logger.info("GET /audit/logs/actor/{} - Récupération logs par acteur", actorId);
        return ResponseEntity.ok(auditUseCase.getAuditLogsByActor(actorId));
    }
    
    @GetMapping("/logs/module/{module}")
    @Operation(summary = "Logs par module", description = "Retourne les logs d'audit par module")
    public ResponseEntity<List<AuditLogDto>> getAuditLogsByModule(@PathVariable String module) {
        logger.info("GET /audit/logs/module/{} - Récupération logs par module", module);
        return ResponseEntity.ok(auditUseCase.getAuditLogsByModule(module));
    }
    
    @GetMapping("/logs/entity/{entityType}")
    @Operation(summary = "Logs par type d'entité", description = "Retourne les logs d'audit par type d'entité")
    public ResponseEntity<List<AuditLogDto>> getAuditLogsByEntityType(@PathVariable String entityType) {
        logger.info("GET /audit/logs/entity/{} - Récupération logs par type d'entité", entityType);
        return ResponseEntity.ok(auditUseCase.getAuditLogsByEntityType(entityType));
    }
    
    @PostMapping("/logs")
    @Operation(summary = "Logger action", description = "Enregistre une nouvelle action dans le journal d'audit")
    public ResponseEntity<AuditLogDto> logAction(@RequestBody AuditLogDto auditLog) {
        logger.info("POST /audit/logs - Log action");
        return ResponseEntity.ok(auditUseCase.logAction(auditLog));
    }
    
    // ==================== ENDPOINTS ÉVÉNEMENTS DE SÉCURITÉ ====================
    
    @GetMapping("/security-events")
    @Operation(summary = "Liste des événements sécurité", description = "Retourne tous les événements de sécurité")
    public ResponseEntity<List<SecurityEventDto>> getAllSecurityEvents() {
        logger.info("GET /audit/security-events - Récupération événements sécurité");
        return ResponseEntity.ok(auditUseCase.getAllSecurityEvents());
    }
    
    @GetMapping("/security-events/recent")
    @Operation(summary = "Événements sécurité récents", description = "Retourne les événements de sécurité récents")
    public ResponseEntity<List<SecurityEventDto>> getRecentSecurityEvents(@RequestParam(defaultValue = "20") int limit) {
        logger.info("GET /audit/security-events/recent - Récupération événements sécurité récents");
        return ResponseEntity.ok(auditUseCase.getRecentSecurityEvents(limit));
    }
    
    @GetMapping("/security-events/severity/{severity}")
    @Operation(summary = "Événements par sévérité", description = "Retourne les événements par niveau de sévérité")
    public ResponseEntity<List<SecurityEventDto>> getSecurityEventsBySeverity(@PathVariable String severity) {
        logger.info("GET /audit/security-events/severity/{} - Récupération événements par sévérité", severity);
        return ResponseEntity.ok(auditUseCase.getSecurityEventsBySeverity(severity));
    }
    
    @GetMapping("/security-events/checkpoint/{checkpointId}")
    @Operation(summary = "Événements par point de contrôle", description = "Retourne les événements par point de contrôle")
    public ResponseEntity<List<SecurityEventDto>> getSecurityEventsByCheckpoint(@PathVariable String checkpointId) {
        logger.info("GET /audit/security-events/checkpoint/{} - Récupération événements par checkpoint", checkpointId);
        return ResponseEntity.ok(auditUseCase.getSecurityEventsByCheckpoint(checkpointId));
    }
    
    @GetMapping("/security-events/pending-investigations")
    @Operation(summary = "Enquêtes en attente", description = "Retourne les événements nécessitant une enquête")
    public ResponseEntity<List<SecurityEventDto>> getPendingInvestigations() {
        logger.info("GET /audit/security-events/pending-investigations - Récupération enquêtes en attente");
        return ResponseEntity.ok(auditUseCase.getPendingInvestigations());
    }
    
    @PostMapping("/security-events")
    @Operation(summary = "Créer événement sécurité", description = "Crée un nouvel événement de sécurité")
    public ResponseEntity<SecurityEventDto> createSecurityEvent(@RequestBody SecurityEventDto event) {
        logger.info("POST /audit/security-events - Création événement sécurité");
        return ResponseEntity.ok(auditUseCase.createSecurityEvent(event));
    }
    
    @PutMapping("/security-events/{id}")
    @Operation(summary = "Mettre à jour événement sécurité", description = "Met à jour un événement de sécurité")
    public ResponseEntity<SecurityEventDto> updateSecurityEvent(@PathVariable String id, 
                                                             @RequestBody SecurityEventDto event) {
        logger.info("PUT /audit/security-events/{} - Mise à jour événement sécurité", id);
        return ResponseEntity.ok(auditUseCase.updateSecurityEvent(id, event));
    }
    
    @PatchMapping("/security-events/{id}/resolve")
    @Operation(summary = "Résoudre événement sécurité", description = "Marque un événement comme résolu")
    public ResponseEntity<Map<String, Object>> resolveSecurityEvent(@PathVariable String id, 
                                                                   @RequestBody Map<String, String> body) {
        logger.info("PATCH /audit/security-events/{}/resolve - Résolution événement sécurité", id);
        auditUseCase.resolveSecurityEvent(id, body.get("resolutionNotes"));
        return ResponseEntity.ok(Map.of("success", true));
    }
    
    // ==================== ENDPOINTS ANALYTIQUES ====================
    
    @GetMapping("/statistics")
    @Operation(summary = "Statistiques d'audit", description = "Retourne les statistiques d'audit")
    public ResponseEntity<Map<String, Object>> getAuditStatistics() {
        logger.info("GET /audit/statistics - Récupération statistiques d'audit");
        return ResponseEntity.ok(auditUseCase.getAuditStatistics());
    }
    
    @GetMapping("/security-statistics")
    @Operation(summary = "Statistiques sécurité", description = "Retourne les statistiques de sécurité")
    public ResponseEntity<Map<String, Object>> getSecurityStatistics() {
        logger.info("GET /audit/security-statistics - Récupération statistiques sécurité");
        return ResponseEntity.ok(auditUseCase.getSecurityStatistics());
    }
    
    @GetMapping("/security-trends")
    @Operation(summary = "Tendances sécurité", description = "Retourne les tendances de sécurité")
    public ResponseEntity<List<Map<String, Object>>> getSecurityTrends() {
        logger.info("GET /audit/security-trends - Récupération tendances sécurité");
        return ResponseEntity.ok(auditUseCase.getSecurityTrends());
    }
    
    // ==================== ENDPOINTS HISTORIQUE ====================
    
    @GetMapping("/trail/{entityType}/{entityId}")
    @Operation(summary = "Historique d'entité", description = "Retourne l'historique d'une entité")
    public ResponseEntity<List<AuditLogDto>> getEntityHistory(@PathVariable String entityType, 
                                                             @PathVariable String entityId) {
        logger.info("GET /audit/trail/{}/{} - Récupération historique entité", entityType, entityId);
        return ResponseEntity.ok(auditUseCase.getEntityHistory(entityType, entityId));
    }
    
    @GetMapping("/security-history/{personId}")
    @Operation(summary = "Historique sécurité personne", description = "Retourne l'historique de sécurité d'une personne")
    public ResponseEntity<List<SecurityEventDto>> getPersonSecurityHistory(@PathVariable String personId) {
        logger.info("GET /audit/security-history/{} - Récupération historique sécurité personne", personId);
        return ResponseEntity.ok(auditUseCase.getPersonSecurityHistory(personId));
    }
}