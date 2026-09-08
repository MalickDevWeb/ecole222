package ecole_221.sn.ecole.modules.admin.infrastructure.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ecole_221.sn.ecole.modules.admin.application.port.usecase.AdminUseCase;
import ecole_221.sn.ecole.modules.admin.domain.valueobject.StatutFrais;
import ecole_221.sn.ecole.infrastructure.logging.ActionLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
@Tag(name = "Admin", description = "Administration — portails, statuts etudiants, logs temps reel")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private final AdminUseCase adminUseCase;
    private final ActionLogger actionLogger;

    public AdminController(AdminUseCase adminUseCase, ActionLogger actionLogger) {
        this.adminUseCase = adminUseCase;
        this.actionLogger = actionLogger;
    }

    @PostMapping("/gates/assign")
    @Operation(summary = "Assigner un portail", description = "Assigne un gardien/vigil a un portail d'acces. Accepte: gate + guard (ou vigilId).")
    public ResponseEntity<Map<String, Object>> assignGate(@RequestBody Map<String, String> body) {
        logger.info("POST /admin/gates/assign - Assignation portail");
        String gate = body.get("gate");
        String guard = body.getOrDefault("guard", body.getOrDefault("vigilId", null));
        adminUseCase.assignGate(gate, guard);
        
        actionLogger.logAdminAction("ASSIGN_GATE", "ADMIN", gate + " -> " + guard, 
            "Portail " + gate + " assigné au vigil " + guard);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("gate", gate);
        response.put("assignedTo", guard);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/students/{id}/status")
    @Operation(summary = "Modifier statut etudiant", description = "Met a jour le statut de frais d'un etudiant (En Règle, En Retard).")
    public ResponseEntity<Map<String, Object>> updateStudentStatus(
            @Parameter(description = "Identifiant de l'etudiant") @PathVariable("id") String studentId,
            @RequestBody Map<String, String> body) {
        logger.info("PATCH /admin/students/{}/status - Mise a jour statut: {}", studentId, body.get("statutFrais"));
        StatutFrais status = StatutFrais.fromString(body.get("statutFrais"));
        adminUseCase.updateStudentStatus(studentId, status);
        
        actionLogger.logAdminAction("UPDATE_STUDENT_STATUS", "ADMIN", studentId, 
            "Statut frais changé à " + status.getDisplayName() + " pour étudiant " + studentId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("status", status.getDisplayName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/realtime-logs")
    @Operation(summary = "Logs temps reel", description = "Retourne les derniers logs d'activite de la plateforme.")
    public ResponseEntity<List<String>> getRealtimeLogs() {
        logger.info("GET /admin/realtime-logs - Recuperation des logs temps reel");
        return ResponseEntity.ok(adminUseCase.getRealtimeLogs());
    }

    @PostMapping("/candidatures/settings")
    @Operation(summary = "Mettre a jour parametres candidature", description = "Active ou desactive le systeme de candidature.")
    public ResponseEntity<Map<String, Object>> updateCandidaturesSettings(@RequestBody Map<String, Boolean> body) {
        logger.info("POST /admin/candidatures/settings - Mise a jour parametres");
        boolean enabled = body.getOrDefault("enabled", false);
        adminUseCase.updateCandidaturesSettings(enabled);
        return ResponseEntity.ok(Map.of("success", true, "candidaturesEnabled", enabled));
    }

    @GetMapping("/stats")
    @Operation(summary = "Statistiques globales", description = "Retourne les statistiques de la plateforme.")
    public ResponseEntity<Map<String, Object>> getStats() {
        logger.info("GET /admin/stats - Recuperation des statistiques");
        Map<String, Object> stats = new HashMap<>();
        stats.put("studentsCount", 150);
        stats.put("professorsCount", 25);
        stats.put("coursesCount", 30);
        stats.put("classesCount", 8);
        stats.put("presentProfessors", "18/25");
        stats.put("salleOccupation", "75%");
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users")
    @Operation(summary = "Liste des utilisateurs", description = "Retourne tous les utilisateurs (etudiants, professeurs, promotions).")
    public ResponseEntity<Map<String, Object>> getUsers() {
        logger.info("GET /admin/users - Recuperation des utilisateurs");
        Map<String, Object> response = new HashMap<>();
        
        // Étudiants mock
        response.put("students", List.of(
            Map.of("id", "usr-001", "name", "Mamadou Sall", "matricule", "2026-GC-001", "promotion_id", "L1-GL", "average", 15.5, "gpa", 3.5, "mood", "stable", "statutFrais", "En Règle"),
            Map.of("id", "usr-005", "name", "Fatou Diop", "matricule", "2026-GC-002", "promotion_id", "L1-GL", "average", 14.2, "gpa", 3.2, "mood", "stable", "statutFrais", "En Règle")
        ));
        
        // Professeurs mock
        response.put("professors", List.of(
            Map.of("id", "usr-002", "name", "Moussa Diallo", "email", "professeur221@gmail.com"),
            Map.of("id", "usr-006", "name", "Aminata Sy", "email", "aminata.sy@ecole221.sn")
        ));
        
        // Promotions mock
        response.put("promotions", List.of(
            Map.of("id", "L1-GL", "name", "L1 Génie Civil", "filiere", "Génie Civil", "faculte", "Sciences et Technologie"),
            Map.of("id", "M1-GL", "name", "M1 Génie Civil", "filiere", "Génie Civil", "faculte", "Sciences et Technologie")
        ));
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/students")
    @Operation(summary = "Ajouter étudiant", description = "Crée un nouvel étudiant.")
    public ResponseEntity<Map<String, Object>> addStudent(@RequestBody Map<String, String> body) {
        logger.info("POST /admin/students - Ajout étudiant: {}", body.get("name"));
        Map<String, Object> response = new HashMap<>();
        response.put("id", "usr-" + System.currentTimeMillis());
        response.put("name", body.get("name"));
        response.put("matricule", body.get("matricule"));
        response.put("promotion_id", body.get("promotion_id"));
        response.put("average", 0.0);
        response.put("gpa", 0.0);
        response.put("mood", "stable");
        response.put("statutFrais", "En Règle");
        
        actionLogger.logAdminAction("ADD_STUDENT", "ADMIN", body.get("name"), 
            "Étudiant ajouté: " + body.get("name"));
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/students/{id}")
    @Operation(summary = "Supprimer étudiant", description = "Supprime un étudiant par son ID.")
    public ResponseEntity<Map<String, Object>> deleteStudent(@PathVariable("id") String id) {
        logger.info("DELETE /admin/students/{} - Suppression étudiant", id);
        
        actionLogger.logAdminAction("DELETE_STUDENT", "ADMIN", id, 
            "Étudiant supprimé: " + id);
        
        return ResponseEntity.ok(Map.of("success", true, "id", id));
    }

    @PostMapping("/professors")
    @Operation(summary = "Ajouter professeur", description = "Crée un nouveau professeur.")
    public ResponseEntity<Map<String, Object>> addProfessor(@RequestBody Map<String, String> body) {
        logger.info("POST /admin/professors - Ajout professeur: {}", body.get("name"));
        Map<String, Object> response = new HashMap<>();
        response.put("id", "usr-" + System.currentTimeMillis());
        response.put("name", body.get("name"));
        response.put("email", body.get("email"));
        
        actionLogger.logAdminAction("ADD_PROFESSOR", "ADMIN", body.get("name"), 
            "Professeur ajouté: " + body.get("name"));
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/professors/{id}")
    @Operation(summary = "Supprimer professeur", description = "Supprime un professeur par son ID.")
    public ResponseEntity<Map<String, Object>> deleteProfessor(@PathVariable("id") String id) {
        logger.info("DELETE /admin/professors/{} - Suppression professeur", id);
        
        actionLogger.logAdminAction("DELETE_PROFESSOR", "ADMIN", id, 
            "Professeur supprimé: " + id);
        
        return ResponseEntity.ok(Map.of("success", true, "id", id));
    }

    @GetMapping("/schedule")
    @Operation(summary = "Planning admin", description = "Retourne le planning complet des sessions.")
    public ResponseEntity<List<Map<String, Object>>> getSchedule() {
        logger.info("GET /admin/schedule - Recuperation du planning");
        return ResponseEntity.ok(List.of(
            Map.of("id", "sched-001", "courseTitle", "Architecture Logicielle", "day", "Lundi", "time", "08:00 - 11:00", "room", "Amphi A", "courseId", "course-1", "type", "CM", "dateStr", "2026-07-27", "status", "a_venir", "classe", "M1 GL"),
            Map.of("id", "sched-002", "courseTitle", "Base de données NoSQL", "day", "Mardi", "time", "14:00 - 17:00", "room", "Salle B2", "courseId", "course-2", "type", "TD", "dateStr", "2026-07-28", "status", "a_venir", "classe", "M1 GL")
        ));
    }
}
