package ecole_221.sn.ecole.modules.admin.infrastructure.web;

import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.DocumentEleveJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.DocumentEleveJpaRepository;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.ProfilEleveJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.ProfilEleveJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Dossier 360° d'un élève, ses documents, et affectation d'un cours à un créneau.
 *
 * <p>Révision 2026-11-09 : tout était gardé en mémoire (perdu au redémarrage ; documents indexés sans
 * école, donc lisibles depuis une autre école) ; le diagnostic « IA » était inventé à partir du seul
 * identifiant (texte fixe en cas d'échec) ; l'affectation de créneau répondait « success » sans rien
 * enregistrer. Désormais : persistance par école (@TenantId), élève obligatoirement membre de l'école,
 * diagnostic refusé faute de données réelles, créneau réellement créé.</p>
 */
@RestController
@RequestMapping("/admin")
public class AdminStudentProfileController {

    private static final Logger logger = LoggerFactory.getLogger(AdminStudentProfileController.class);

    private final ComptesEcoleService comptesEcole;
    private final ProfilEleveJpaRepository profils;
    private final DocumentEleveJpaRepository documents;
    private final CourseJpaRepository cours;
    private final ScheduleSessionJpaRepository seances;

    public AdminStudentProfileController(ComptesEcoleService comptesEcole, ProfilEleveJpaRepository profils,
                                         DocumentEleveJpaRepository documents, CourseJpaRepository cours,
                                         ScheduleSessionJpaRepository seances) {
        this.comptesEcole = comptesEcole;
        this.profils = profils;
        this.documents = documents;
        this.cours = cours;
        this.seances = seances;
    }

    @GetMapping("/students/{id}/profile360")
    public ResponseEntity<Map<String, Object>> getProfile360(@PathVariable("id") String studentId) {
        comptesEcole.membre(studentId, "ETUDIANT");   // 404 si l'élève n'est pas de cette école
        return ResponseEntity.ok(profil(profils.findFirstByUserId(studentId).orElse(null)));
    }

    @PostMapping("/students/{id}/profile360")
    public ResponseEntity<Map<String, Object>> updateProfile360(
            @PathVariable("id") String studentId,
            @RequestBody Map<String, Object> fields) {
        comptesEcole.membre(studentId, "ETUDIANT");
        ProfilEleveJpaEntity p = profils.findFirstByUserId(studentId)
                .orElseGet(() -> new ProfilEleveJpaEntity(UUID.randomUUID().toString(), studentId));
        if (fields.containsKey("bloodGroup")) p.setGroupeSanguin(texte(fields.get("bloodGroup")));
        if (fields.containsKey("allergies")) p.setAllergies(texte(fields.get("allergies")));
        p.setUpdatedAt(LocalDateTime.now());
        profils.save(p);
        logger.info("POST /admin/students/{}/profile360 - dossier mis à jour", studentId);
        return ResponseEntity.ok(profil(p));
    }

    @GetMapping("/students/{id}/documents")
    public ResponseEntity<List<Map<String, Object>>> getDocuments(@PathVariable("id") String studentId) {
        comptesEcole.membre(studentId, "ETUDIANT");
        return ResponseEntity.ok(documents.findByUserIdOrderByCreatedAtAsc(studentId).stream().map(this::document).toList());
    }

    @PostMapping("/students/{id}/documents")
    public ResponseEntity<Map<String, Object>> addDocument(
            @PathVariable("id") String studentId,
            @RequestBody Map<String, String> body) {
        comptesEcole.membre(studentId, "ETUDIANT");
        String nom = texte(body.get("name"));
        String url = texte(body.get("url"));
        if (nom == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom du document est obligatoire.");
        if (url == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Le document doit d'abord être déposé (adresse du fichier manquante).");
        documents.save(new DocumentEleveJpaEntity("doc-" + UUID.randomUUID(), studentId, nom, texte(body.get("size")), url));
        List<Map<String, Object>> docs = documents.findByUserIdOrderByCreatedAtAsc(studentId).stream().map(this::document).toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("documents", docs));
    }

    /**
     * Diagnostic « IA » d'un élève : aucune donnée académique réelle (notes, présences) n'est transmise au
     * modèle, qui ne recevait que l'identifiant → texte inventé. Refusé tant qu'il n'est pas branché sur des données réelles.
     */
    @PostMapping("/student-diagnostic")
    public ResponseEntity<Map<String, String>> generateDiagnostic(@RequestBody Map<String, String> body) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Diagnostic indisponible : il n'est pas encore alimenté par les notes et présences réelles de l'élève.");
    }

    @PostMapping("/schedule/assign")
    public ResponseEntity<Map<String, Object>> assignSchedule(@RequestBody Map<String, String> body) {
        String courseId = texte(body.get("courseId"));
        String day = texte(body.get("day"));
        String time = texte(body.get("time"));
        String room = texte(body.get("room"));
        if (courseId == null || day == null || time == null || room == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cours, jour, horaire et salle sont obligatoires.");
        }
        CourseJpaEntity c = cours.findById(courseId)   // @TenantId : cours de cette école uniquement
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cours introuvable dans cet établissement."));
        String prof = Optional.ofNullable(texte(body.get("professorId"))).orElse(c.getProfId());
        if (prof != null) comptesEcole.membre(prof, "PROFESSEUR");
        ScheduleSessionJpaEntity s = new ScheduleSessionJpaEntity();
        s.setId("ses-" + UUID.randomUUID());
        s.setCourseId(c.getId());
        s.setCourseTitle(c.getName());
        s.setClasse(c.getClasse());
        s.setDay(day);
        s.setTime(time);
        s.setRoom(room);
        s.setProfessorId(prof);
        s.setStatus("PLANIFIE");
        s.setCreatedAt(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        seances.save(s);
        logger.info("POST /admin/schedule/assign - séance {} créée pour le cours {}", s.getId(), courseId);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("id", s.getId());
        res.put("courseId", courseId);
        res.put("day", day);
        res.put("time", time);
        res.put("room", room);
        res.put("professorId", prof);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    /**
     * Profil 360 : seules les données réellement saisies sont renvoyées (APP-BE-17) ; les rubriques sans
     * source (notes, connexions, parcours, paiements) restent vides avec un statut explicite.
     */
    private Map<String, Object> profil(ProfilEleveJpaEntity p) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("donneesStatus", p == null ? "non_disponible" : "partiel");
        profile.put("admissionDetails", new LinkedHashMap<String, Object>());
        profile.put("ueList", List.of());
        profile.put("connectionLogs", List.of());
        profile.put("activityTrail", List.of());
        profile.put("timeline", List.of());
        profile.put("payments", List.of());
        profile.put("paymentsStatus", "non_disponible");
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("messages", List.of());
        if (p != null) {
            if (p.getGroupeSanguin() != null) extra.put("sante", p.getGroupeSanguin());
            if (p.getAllergies() != null) extra.put("allergies", p.getAllergies());
        }
        profile.put("extra", extra);
        return profile;
    }

    private Map<String, Object> document(DocumentEleveJpaEntity d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("name", d.getName());
        m.put("size", d.getSize());
        m.put("url", d.getUrl());
        m.put("date", d.getCreatedAt() == null ? null : d.getCreatedAt().toLocalDate().toString());
        return m;
    }

    private static String texte(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }
}
