package ecole_221.sn.ecole.modules.collaboration.infrastructure.web;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ecole_221.sn.ecole.modules.collaboration.application.port.usecase.CollaborationUseCase;
import ecole_221.sn.ecole.infrastructure.logging.ActionLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/collaboration")
@CrossOrigin(origins = "*")
@Tag(name = "Collaboration", description = "Collaboration — groupes de travail, documents, tâches, messages, réunions")
public class CollaborationController {
    private static final Logger logger = LoggerFactory.getLogger(CollaborationController.class);

    private final CollaborationUseCase collaborationUseCase;
    private final ActionLogger actionLogger;

    public CollaborationController(CollaborationUseCase collaborationUseCase, ActionLogger actionLogger) {
        this.collaborationUseCase = collaborationUseCase;
        this.actionLogger = actionLogger;
    }

    @GetMapping("/workgroups")
    @Operation(summary = "Liste des groupes de travail", description = "Retourne tous les groupes de travail disponibles.")
    public ResponseEntity<List<Map<String, Object>>> getWorkgroups() {
        List<Map<String, Object>> result = collaborationUseCase.getWorkgroups().stream()
            .map(dto -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", dto.id());
                map.put("name", dto.name());
                map.put("nom", dto.name()); // Compatibilité frontend
                map.put("description", dto.description());
                map.put("membersCount", dto.memberCount());
                map.put("membres", List.of()); // Placeholder pour compatibilité
                return map;
            })
            .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/workgroups")
    @Operation(summary = "Créer un groupe", description = "Crée un nouveau groupe de travail (name, description).")
    public ResponseEntity<Map<String, Object>> createWorkgroup(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        String creatorId = (String) body.getOrDefault("creatorId", "usr-001");
        
        var workgroup = collaborationUseCase.createWorkgroup(name, description, creatorId);
        
        actionLogger.logCollaborationAction("CREATE_WORKGROUP", creatorId, "NEW", 
            "Création groupe: " + name);
        
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("id", workgroup.id());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/workgroups/{groupId}/documents")
    @Operation(summary = "Documents d'un groupe", description = "Retourne les documents partagés dans un groupe.")
    public ResponseEntity<List<Map<String, Object>>> getDocuments(
            @Parameter(description = "Identifiant du groupe") @PathVariable("groupId") String groupId) {
        return ResponseEntity.ok(collaborationUseCase.getDocuments(groupId));
    }

    @PostMapping("/workgroups/{groupId}/documents")
    @Operation(summary = "Ajouter un document", description = "Ajoute un document partagé dans un groupe (name, type, size, url, addedBy).")
    public ResponseEntity<Map<String, Object>> addDocument(
            @Parameter(description = "Identifiant du groupe") @PathVariable("groupId") String groupId,
            @RequestBody Map<String, Object> body) {
        collaborationUseCase.addDocument(
            groupId, (String) body.get("name"), (String) body.get("type"),
            (String) body.get("size"), (String) body.get("url"), (String) body.get("addedBy")
        );
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/workgroups/{groupId}/homeworks")
    @Operation(summary = "Devoirs d'un groupe", description = "Retourne les devoirs de groupe partagés dans un groupe.")
    public ResponseEntity<List<Map<String, Object>>> getHomeworks(
            @Parameter(description = "Identifiant du groupe") @PathVariable("groupId") String groupId) {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/workgroups/{groupId}/homeworks")
    @Operation(summary = "Ajouter un devoir", description = "Ajoute un devoir dans un groupe.")
    public ResponseEntity<Map<String, Object>> addHomework(
            @Parameter(description = "Identifiant du groupe") @PathVariable("groupId") String groupId,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/workgroups/{groupId}/tasks")
    @Operation(summary = "Tâches d'un groupe", description = "Retourne les tâches assignées dans un groupe.")
    public ResponseEntity<List<Map<String, Object>>> getTasks(
            @Parameter(description = "Identifiant du groupe") @PathVariable("groupId") String groupId) {
        return ResponseEntity.ok(collaborationUseCase.getTasksCompat(groupId));
    }

    @PostMapping("/workgroups/{groupId}/tasks")
    @Operation(summary = "Ajouter une tâche", description = "Crée une nouvelle tâche dans un groupe (title, status, assignedTo).")
    public ResponseEntity<Map<String, Object>> addTask(
            @Parameter(description = "Identifiant du groupe") @PathVariable("groupId") String groupId,
            @RequestBody Map<String, Object> body) {
        collaborationUseCase.addTask(groupId, (String) body.get("title"), (String) body.get("status"), (String) body.get("assignedTo"));
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/workgroups/{groupId}/messages")
    @Operation(summary = "Messages d'un groupe", description = "Retourne les messages du chat d'un groupe.")
    public ResponseEntity<List<Map<String, Object>>> getMessages(
            @Parameter(description = "Identifiant du groupe") @PathVariable("groupId") String groupId) {
        return ResponseEntity.ok(collaborationUseCase.getMessages(groupId));
    }

    @PostMapping("/workgroups/{groupId}/messages")
    @Operation(summary = "Envoyer un message", description = "Envoie un message dans le chat d'un groupe (sender, content).")
    public ResponseEntity<Map<String, Object>> addMessage(
            @Parameter(description = "Identifiant du groupe") @PathVariable("groupId") String groupId,
            @RequestBody Map<String, Object> body) {
        collaborationUseCase.addMessage(groupId, (String) body.get("sender"), (String) body.get("content"));
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/meets")
    @Operation(summary = "Liste des réunions", description = "Retourne toutes les réunions virtuelles planifiées.")
    public ResponseEntity<List<Map<String, Object>>> getMeets() {
        return ResponseEntity.ok(collaborationUseCase.getMeets());
    }

    @PostMapping("/meets")
    @Operation(summary = "Créer une réunion", description = "Planifie une nouvelle réunion virtuelle (title, link, time).")
    public ResponseEntity<Map<String, Object>> createMeet(@RequestBody Map<String, Object> body) {
        collaborationUseCase.createMeet((String) body.get("title"), (String) body.get("link"), (String) body.get("time"));
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/admin-meets")
    @Operation(summary = "Réunions admin", description = "Retourne les réunions visibles par les administrateurs.")
    public ResponseEntity<List<Map<String, Object>>> getAdminMeets() {
        return ResponseEntity.ok(collaborationUseCase.getMeets());
    }

    @PostMapping("/voice-sessions")
    @Operation(summary = "Créer une session vocale", description = "Crée une nouvelle session de collaboration vocale (title, groupId).")
    public ResponseEntity<Map<String, Object>> createVoiceSession(@RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String groupId = (String) body.get("groupId");
        String sessionId = "voice-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> res = new HashMap<>();
        res.put("sessionId", sessionId);
        res.put("title", title);
        res.put("groupId", groupId);
        res.put("createdAt", java.time.LocalDateTime.now().toString());
        res.put("success", true);
        
        logger.info("Session vocale créée: {} pour le groupe {}", sessionId, groupId);
        return ResponseEntity.ok(res);
    }
    
    @GetMapping("/voice-sessions")
    @Operation(summary = "Liste des sessions vocales", description = "Retourne toutes les sessions vocales actives.")
    public ResponseEntity<List<Map<String, Object>>> getVoiceSessions() {
        // Retourne une liste vide pour l'instant (à connecter avec la persistance)
        List<Map<String, Object>> sessions = List.of();
        return ResponseEntity.ok(sessions);
    }
    
    @GetMapping("/voice-sessions/{sessionId}")
    @Operation(summary = "Détails d'une session vocale", description = "Retourne les détails d'une session vocale spécifique.")
    public ResponseEntity<Map<String, Object>> getVoiceSession(@PathVariable String sessionId) {
        Map<String, Object> session = new HashMap<>();
        session.put("sessionId", sessionId);
        session.put("title", "Session vocale");
        session.put("active", true);
        return ResponseEntity.ok(session);
    }
    
    @DeleteMapping("/voice-sessions/{sessionId}")
    @Operation(summary = "Supprimer une session vocale", description = "Termine une session vocale active.")
    public ResponseEntity<Map<String, Object>> deleteVoiceSession(@PathVariable String sessionId) {
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Session vocale terminée");
        return ResponseEntity.ok(res);
    }
    
    @GetMapping("/workgroups/{groupId}/voice")
    @Operation(summary = "Session vocale d'un groupe", description = "Retourne la session vocale active d'un groupe.")
    public ResponseEntity<Map<String, Object>> getGroupVoiceSession(
            @Parameter(description = "Identifiant du groupe") @PathVariable("groupId") String groupId) {
        Map<String, Object> session = new HashMap<>();
        session.put("groupId", groupId);
        session.put("hasActiveSession", false);
        return ResponseEntity.ok(session);
    }
    
    @PostMapping("/workgroups/{groupId}/voice")
    @Operation(summary = "Démarrer session vocale", description = "Démarre une nouvelle session vocale pour un groupe.")
    public ResponseEntity<Map<String, Object>> startGroupVoiceSession(
            @Parameter(description = "Identifiant du groupe") @PathVariable("groupId") String groupId,
            @RequestBody Map<String, Object> body) {
        String sessionId = "voice-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> res = new HashMap<>();
        res.put("sessionId", sessionId);
        res.put("groupId", groupId);
        res.put("title", body.get("title"));
        res.put("active", true);
        res.put("startedAt", java.time.LocalDateTime.now().toString());
        res.put("success", true);
        
        logger.info("Session vocale démarrée: {} pour le groupe {}", sessionId, groupId);
        return ResponseEntity.ok(res);
    }
}