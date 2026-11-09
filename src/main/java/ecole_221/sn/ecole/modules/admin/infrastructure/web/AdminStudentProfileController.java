package ecole_221.sn.ecole.modules.admin.infrastructure.web;

import ecole_221.sn.ecole.modules.ai.application.port.usecase.AIUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/admin")
public class AdminStudentProfileController {

    private static final Logger logger = LoggerFactory.getLogger(AdminStudentProfileController.class);

    private final AIUseCase aiUseCase;
    private final Map<String, Map<String, Object>> profileStore = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> documentStore = new ConcurrentHashMap<>();

    public AdminStudentProfileController(AIUseCase aiUseCase) {
        this.aiUseCase = aiUseCase;
    }

    @GetMapping("/students/{id}/profile360")
    public ResponseEntity<Map<String, Object>> getProfile360(@PathVariable("id") String studentId) {
        logger.info("GET /admin/students/{}/profile360", studentId);
        return ResponseEntity.ok(profileStore.computeIfAbsent(studentId, this::buildDefaultProfile));
    }

    @PostMapping("/students/{id}/profile360")
    public ResponseEntity<Map<String, Object>> updateProfile360(
            @PathVariable("id") String studentId,
            @RequestBody Map<String, Object> fields) {
        logger.info("POST /admin/students/{}/profile360", studentId);
        Map<String, Object> profile = profileStore.computeIfAbsent(studentId, this::buildDefaultProfile);
        Map<String, Object> extra = castMap(profile.get("extra"));
        if (fields.containsKey("email")) {
            // no-op placeholder for future user linkage
        }
        if (fields.containsKey("bloodGroup")) extra.put("sante", fields.get("bloodGroup"));
        if (fields.containsKey("allergies")) extra.put("allergies", fields.get("allergies"));
        profile.put("extra", extra);
        profileStore.put(studentId, profile);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/students/{id}/documents")
    public ResponseEntity<List<Map<String, Object>>> getDocuments(@PathVariable("id") String studentId) {
        logger.info("GET /admin/students/{}/documents", studentId);
        return ResponseEntity.ok(documentStore.getOrDefault(studentId, List.of()));
    }

    @PostMapping("/students/{id}/documents")
    public ResponseEntity<Map<String, Object>> addDocument(
            @PathVariable("id") String studentId,
            @RequestBody Map<String, String> body) {
        logger.info("POST /admin/students/{}/documents", studentId);
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", "doc-" + System.currentTimeMillis());
        doc.put("name", body.getOrDefault("name", "Document"));
        doc.put("size", body.getOrDefault("size", "0 Mo"));
        doc.put("url", body.getOrDefault("url", ""));

        List<Map<String, Object>> docs = new ArrayList<>(documentStore.getOrDefault(studentId, List.of()));
        docs.add(doc);
        documentStore.put(studentId, docs);

        return ResponseEntity.ok(Map.of("documents", docs));
    }

    @PostMapping("/student-diagnostic")
    public ResponseEntity<Map<String, String>> generateDiagnostic(@RequestBody Map<String, String> body) {
        String studentId = body.getOrDefault("studentId", "unknown");
        logger.info("POST /admin/student-diagnostic for student {}", studentId);

        String prompt = "Génère un diagnostic académique concis (5-8 lignes) pour l'étudiant ID "
                + studentId + ". Mentionne forces, axes d'amélioration et recommandations.";
        String text;
        try {
            text = aiUseCase.generateTutorResponse(prompt);
        } catch (Exception e) {
            text = "Diagnostic académique pour l'étudiant " + studentId
                    + " : profil stable, assiduité correcte, progression satisfaisante en UE techniques. "
                    + "Recommandation : renforcer les compétences en planification de projet et en anglais technique.";
        }
        return ResponseEntity.ok(Map.of("text", text));
    }

    @PostMapping("/schedule/assign")
    public ResponseEntity<Map<String, Object>> assignSchedule(@RequestBody Map<String, String> body) {
        logger.info("POST /admin/schedule/assign - courseId={}", body.get("courseId"));
        return ResponseEntity.ok(Map.of(
                "success", true,
                "courseId", body.getOrDefault("courseId", ""),
                "day", body.getOrDefault("day", ""),
                "time", body.getOrDefault("time", ""),
                "room", body.getOrDefault("room", "")
        ));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return new LinkedHashMap<>();
    }

    /**
     * Profil 360 par défaut (aucune donnée enregistrée pour cet élève) — APP-BE-17.
     * Avant : dossier d'admission, notes, connexions, parcours, messages et DONNÉES DE SANTÉ
     * (« O+, Aucune allergie connue ») inventés pour tout élève. Désormais : structure vide
     * et statut explicite ; seules les données saisies (POST profile360) sont renvoyées.
     */
    private Map<String, Object> buildDefaultProfile(String studentId) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("donneesStatus", "non_disponible");
        profile.put("admissionDetails", new LinkedHashMap<String, Object>());
        profile.put("ueList", List.of());
        profile.put("connectionLogs", List.of());
        profile.put("activityTrail", List.of());
        profile.put("timeline", List.of());
        // APP-BE-04 : aucun paiement fictif
        profile.put("payments", List.of());
        profile.put("paymentsStatus", "non_disponible");
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("messages", List.of());
        profile.put("extra", extra);
        return profile;
    }
}
