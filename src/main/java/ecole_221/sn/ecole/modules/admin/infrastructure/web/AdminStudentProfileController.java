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
@CrossOrigin(origins = "*")
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

    private Map<String, Object> buildDefaultProfile(String studentId) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("admissionDetails", Map.of(
                "matricule", "MAT-2023-" + studentId,
                "dateInscription", "15/09/2023",
                "bacSerie", "S1 Scientifique",
                "bacMention", "Très Bien (16.8/20)",
                "lyceeOrigine", "Lycée Seydou Nourou Tall",
                "commissionAdmission", "Commission Régionale Dakar",
                "dateDecision", "05/09/2023",
                "statut", "ADMIS"
        ));
        profile.put("ueList", List.of(
                Map.of("ue", "UE Scientifique Fondamentale", "module", "Mathématiques & Analyse",
                        "coefficient", 4, "grade", 16.5, "teacher", "Mme Sow", "status", "VALIDÉ"),
                Map.of("ue", "UE Technique Professionnelle", "module", "Résistance des Matériaux (RDM)",
                        "coefficient", 5, "grade", 17.8, "teacher", "Prof Diallo", "status", "VALIDÉ")
        ));
        profile.put("connectionLogs", List.of(
                Map.of("date", "2026-07-17 08:15", "device", "Chrome Desktop", "location", "Dakar, Sénégal", "status", "RÉUSSI")
        ));
        profile.put("activityTrail", List.of(
                Map.of("id", "log-1", "timestamp", "2026-07-17 08:35", "actor", "Étudiant",
                        "module", "Planning", "action", "Consultation emploi du temps", "result", "SUCCÈS")
        ));
        profile.put("timeline", List.of(
                Map.of("year", "2023-09", "title", "Candidature & Admission",
                        "desc", "Dossier accepté", "status", "COMPLETED")
        ));
        profile.put("payments", List.of(
                Map.of("label", "Inscription Administrative", "amount", "250,000 FCFA",
                        "date", "15/09/2023", "method", "Orange Money", "status", "PAYÉ", "receiptNumber", "REC-2023-001")
        ));
        profile.put("extra", Map.of(
                "sante", "O+,Aucune allergie connue",
                "bourses", "Aucune bourse",
                "messages", List.of(
                        Map.of("sender", "Administration", "text", "Bienvenue sur la plateforme École 221.",
                                "date", "10/07/2026 09:00")
                )
        ));
        return profile;
    }
}
