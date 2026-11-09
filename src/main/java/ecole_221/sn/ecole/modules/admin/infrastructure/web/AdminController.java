package ecole_221.sn.ecole.modules.admin.infrastructure.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ecole_221.sn.ecole.modules.admin.application.port.usecase.AdminUseCase;
import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService.CompteCree;
import ecole_221.sn.ecole.modules.admin.domain.port.repository.AdminRepositoryPort;
import ecole_221.sn.ecole.modules.admin.domain.valueobject.StatutFrais;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.PointagePersonnelJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.PointagePersonnelJpaRepository;
import ecole_221.sn.ecole.infrastructure.logging.ActionLogger;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaEntity;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaRepository;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaRepository;

import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Administration d'UNE école (celle de la requête).
 *
 * <p>Révision 2026-11-09 (« plus aucune donnée fictive ») : toutes les listes et statistiques sont
 * cloisonnées à l'école courante (appartenances organization_users ou entités @TenantId) ; plus aucune
 * moyenne, présence, filière, classe, statut de frais ou téléphone inventé ; les comptes créés ont un
 * mot de passe (sinon connexion impossible) ; les écritures persistent réellement ou échouent avec un
 * message clair. Une information sans source réelle est renvoyée {@code null} (affichée « — » / « Non renseigné »).</p>
 */
@RestController
@RequestMapping("/admin")
@Tag(name = "Admin", description = "Administration — portails, statuts etudiants, logs temps reel")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired(required = false)
    private CourseJpaRepository courseJpaRepository;

    @Autowired(required = false)
    private ecole_221.sn.ecole.modules.admin.application.service.AdminStaffService adminStaffService;

    private final AdminUseCase adminUseCase;
    private final ActionLogger actionLogger;
    private final ComptesEcoleService comptesEcole;
    private final AdminRepositoryPort adminRepository;
    private final ClasseJpaRepository classeJpaRepository;
    private final ScheduleSessionJpaRepository scheduleSessionJpaRepository;
    private final PointagePersonnelJpaRepository pointages;
    private final InscriptionClasseJpaRepository inscriptions;

    public AdminController(AdminUseCase adminUseCase,
                           ActionLogger actionLogger,
                           ComptesEcoleService comptesEcole,
                           AdminRepositoryPort adminRepository,
                           ClasseJpaRepository classeJpaRepository,
                           ScheduleSessionJpaRepository scheduleSessionJpaRepository,
                           PointagePersonnelJpaRepository pointages,
                           InscriptionClasseJpaRepository inscriptions) {
        this.adminUseCase = adminUseCase;
        this.actionLogger = actionLogger;
        this.comptesEcole = comptesEcole;
        this.adminRepository = adminRepository;
        this.classeJpaRepository = classeJpaRepository;
        this.scheduleSessionJpaRepository = scheduleSessionJpaRepository;
        this.pointages = pointages;
        this.inscriptions = inscriptions;
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
    @Operation(summary = "Modifier statut etudiant", description = "Met a jour le statut de frais d'un etudiant de l'école (En Règle, En Retard).")
    public ResponseEntity<Map<String, Object>> updateStudentStatus(
            @Parameter(description = "Identifiant de l'etudiant") @PathVariable("id") String studentId,
            @RequestBody Map<String, String> body) {
        String saisi = body == null ? null : body.get("statutFrais");
        logger.info("PATCH /admin/students/{}/status - Mise a jour statut: {}", studentId, saisi);
        StatutFrais status = statutFraisStrict(saisi);
        comptesEcole.membre(studentId, "ETUDIANT");   // 404 si l'élève n'est pas de cette école
        adminUseCase.updateStudentStatus(studentId, status);

        actionLogger.logAdminAction("UPDATE_STUDENT_STATUS", "ADMIN", studentId,
            "Statut frais changé à " + status.getDisplayName() + " pour étudiant " + studentId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("status", status.getDisplayName());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/students/{id}/toggle-status")
    @Operation(summary = "Suspendre / réactiver un élève", description = "Bascule l'appartenance de l'élève à l'école (ACTIVE ⇄ SUSPENDED). Un élève suspendu n'a plus accès à l'école (badge refusé, connexion refusée).")
    public ResponseEntity<Map<String, Object>> toggleStudentStatus(@PathVariable("id") String studentId) {
        String statut = comptesEcole.basculerStatut(studentId, "ETUDIANT");
        actionLogger.logAdminAction("TOGGLE_STUDENT_STATUS", "ADMIN", studentId, "Accès élève : " + statut);
        return ResponseEntity.ok(Map.of("id", studentId, "status", ComptesEcoleService.libelleStatut(statut),
                "qrStatus", qrStatus(statut)));
    }

    @GetMapping("/realtime-logs")
    @Operation(summary = "Logs temps reel", description = "Retourne les derniers logs d'activite de la plateforme.")
    public ResponseEntity<List<String>> getRealtimeLogs() {
        logger.info("GET /admin/realtime-logs - Recuperation des logs temps reel");
        return ResponseEntity.ok(adminUseCase.getRealtimeLogs());
    }

    @PostMapping("/candidatures/settings")
    @Operation(summary = "Mettre a jour parametres candidature", description = "Active ou desactive le systeme de candidature.")
    public ResponseEntity<Map<String, Object>> updateCandidaturesSettings(@RequestBody Map<String, Object> body) {
        logger.info("POST /admin/candidatures/settings - Mise a jour parametres: {}", body);
        boolean enabled = false;
        if (body != null) {
            if (body.containsKey("enabled")) {
                enabled = Boolean.parseBoolean(String.valueOf(body.get("enabled")));
            } else if (body.containsKey("ouvert")) {
                enabled = Boolean.parseBoolean(String.valueOf(body.get("ouvert")));
            }
        }
        adminUseCase.updateCandidaturesSettings(enabled);
        return ResponseEntity.ok(Map.of("success", true, "candidaturesEnabled", enabled));
    }

    @GetMapping("/stats")
    @Operation(summary = "Statistiques de l'école", description = "Effectifs réels de l'école courante. Les indicateurs sans source (présence des professeurs, occupation des salles) valent null.")
    public ResponseEntity<Map<String, Object>> getStats() {
        logger.info("GET /admin/stats - Statistiques de l'école courante");
        Map<String, Object> stats = new HashMap<>();
        long professorsCount = comptesEcole.nombreActifs("PROFESSEUR");
        stats.put("studentsCount", comptesEcole.nombreActifs("ETUDIANT"));
        stats.put("professorsCount", professorsCount);
        stats.put("coursesCount", scheduleSessionJpaRepository.count());   // @TenantId : séances de l'école
        stats.put("classesCount", classeJpaRepository.count());            // @TenantId : classes de l'école
        // aucune source de pointage des professeurs ni de taux d'occupation des salles : pas de valeur inventée
        stats.put("presentProfessors", null);
        stats.put("salleOccupation", null);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users")
    @Operation(summary = "Comptes de l'école", description = "Élèves, professeurs et classes de l'école courante (données réelles uniquement).")
    public ResponseEntity<Map<String, Object>> getUsers() {
        logger.info("GET /admin/users - Comptes de l'école courante");
        Map<String, Object> response = new HashMap<>();
        response.put("students", eleves());
        response.put("professors", comptesEcole.membres("PROFESSEUR").stream()
                .map(e -> professeur(e.getKey(), e.getValue())).toList());
        List<ClasseJpaEntity> classeEntities = classeJpaRepository.findAll();   // @TenantId
        response.put("promotions", classeEntities.stream().map(c -> {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("id", c.getId());
            map.put("name", c.getName() != null ? c.getName() : c.getNom());
            map.put("filiere", c.getFiliereId());
            map.put("level", c.getLevel());
            map.put("capacity", c.getCapacity());
            return map;
        }).toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/students")
    @Operation(summary = "Lister les étudiants", description = "Élèves de l'école courante (données réelles uniquement).")
    public ResponseEntity<List<Map<String, Object>>> getStudents() {
        logger.info("GET /admin/students - Élèves de l'école courante");
        return ResponseEntity.ok(eleves());
    }

    @PostMapping("/students")
    @Operation(summary = "Ajouter étudiant", description = "Crée le compte de l'élève dans l'école courante. E-mail obligatoire (identifiant de connexion) ; "
            + "mot de passe saisi (8 caractères min.) ou généré et renvoyé une seule fois dans motDePasseProvisoire.")
    public ResponseEntity<Map<String, Object>> addStudent(@RequestBody Map<String, String> body) {
        String[] nom = noms(body);
        logger.info("POST /admin/students - Création d'un élève");
        ClasseJpaEntity classe = classeDeLEcole(premier(body.get("classeId"), body.get("promotion_id")));   // 400 avant toute création
        CompteCree cree = comptesEcole.creer(nom[0], nom[1], body.get("email"),
                premier(body.get("telephone"), body.get("phoneParent")), body.get("matricule"), "ETUDIANT",
                body.get("password"));
        UserJpaEntity u = cree.compte();
        actionLogger.logAdminAction("ADD_STUDENT", "ADMIN", u.getId(), "Élève créé : " + u.getEmail());
        if (classe != null) {
            inscriptions.save(new InscriptionClasseJpaEntity(UUID.randomUUID().toString(), u.getId(), classe.getId()));
        }

        Map<String, Object> response = eleve(u, "ACTIVE", classe == null ? null : Map.of(u.getId(), classe));
        if (cree.motDePasseProvisoire() != null) {
            response.put("motDePasseProvisoire", cree.motDePasseProvisoire());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/students/{id}/classe")
    @Operation(summary = "Affecter un élève à une classe", description = "Affecte l'élève à une classe de l'école ({\"classeId\": \"…\"}) ; classeId null = retirer l'affectation.")
    public ResponseEntity<Map<String, Object>> affecterClasse(@PathVariable("id") String id, @RequestBody Map<String, String> body) {
        comptesEcole.membre(id, "ETUDIANT");   // 404 si l'élève n'est pas de cette école
        ClasseJpaEntity classe = classeDeLEcole(body == null ? null : body.get("classeId"));
        List<InscriptionClasseJpaEntity> existantes = inscriptions.findByUserId(id);
        if (classe == null) {
            inscriptions.deleteAll(existantes);
        } else if (existantes.isEmpty()) {
            inscriptions.save(new InscriptionClasseJpaEntity(UUID.randomUUID().toString(), id, classe.getId()));
        } else {
            InscriptionClasseJpaEntity i = existantes.get(0);
            i.setClasseId(classe.getId());
            inscriptions.save(i);
            if (existantes.size() > 1) inscriptions.deleteAll(existantes.subList(1, existantes.size()));
        }
        actionLogger.logAdminAction("ASSIGN_CLASS", "ADMIN", id, "Classe : " + (classe == null ? "aucune" : nomClasse(classe)));
        Map<String, Object> res = new HashMap<>();
        res.put("id", id);
        res.put("classeId", classe == null ? null : classe.getId());
        res.put("classe", classe == null ? null : nomClasse(classe));
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/students/{id}")
    @Operation(summary = "Retirer un étudiant", description = "Retire l'élève de l'école courante (compte supprimé s'il n'appartient à aucune autre école).")
    public ResponseEntity<Map<String, Object>> deleteStudent(@PathVariable("id") String id) {
        logger.info("DELETE /admin/students/{} - Retrait de l'élève", id);
        comptesEcole.retirer(id, "ETUDIANT");
        inscriptions.deleteAll(inscriptions.findByUserId(id));
        actionLogger.logAdminAction("DELETE_STUDENT", "ADMIN", id, "Élève retiré de l'école : " + id);
        return ResponseEntity.ok(Map.of("success", true, "id", id));
    }

    @PostMapping("/professors")
    @Operation(summary = "Ajouter professeur", description = "Crée le compte du professeur dans l'école courante (mêmes règles que pour un élève).")
    public ResponseEntity<Map<String, Object>> addProfessor(@RequestBody Map<String, String> body) {
        String[] nom = noms(body);
        logger.info("POST /admin/professors - Création d'un professeur");
        CompteCree cree = comptesEcole.creer(nom[0], nom[1], body.get("email"), body.get("telephone"),
                body.get("matricule"), "PROFESSEUR", body.get("password"));
        UserJpaEntity u = cree.compte();
        actionLogger.logAdminAction("ADD_PROFESSOR", "ADMIN", u.getId(), "Professeur créé : " + u.getEmail());

        Map<String, Object> response = professeur(u, null);
        if (cree.motDePasseProvisoire() != null) {
            response.put("motDePasseProvisoire", cree.motDePasseProvisoire());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/professors/{id}")
    @Operation(summary = "Retirer professeur", description = "Retire le professeur de l'école courante.")
    public ResponseEntity<Map<String, Object>> deleteProfessor(@PathVariable("id") String id) {
        logger.info("DELETE /admin/professors/{} - Retrait du professeur", id);
        comptesEcole.retirer(id, "PROFESSEUR");
        actionLogger.logAdminAction("DELETE_PROFESSOR", "ADMIN", id, "Professeur retiré de l'école : " + id);
        return ResponseEntity.ok(Map.of("success", true, "id", id));
    }

    @GetMapping("/schedule")
    @Operation(summary = "Planning admin", description = "Séances de l'école courante.")
    public ResponseEntity<List<Map<String, Object>>> getSchedule() {
        logger.info("GET /admin/schedule - Séances de l'école courante");
        List<ScheduleSessionJpaEntity> sessions = scheduleSessionJpaRepository.findAll();   // @TenantId
        List<Map<String, Object>> list = sessions.stream().map(s -> {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("id", s.getId());
            map.put("courseTitle", s.getCourseTitle());
            map.put("day", s.getDay());
            map.put("time", s.getTime());
            map.put("room", s.getRoom());
            map.put("courseId", s.getCourseId());
            map.put("type", s.getType());
            map.put("dateStr", s.getDateStr());
            map.put("status", s.getStatus());
            map.put("classe", s.getClasse());
            return map;
        }).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/attendance")
    @Operation(summary = "Émargements du personnel", description = "Pointages du personnel saisis dans l'école courante (vide tant qu'aucun n'est saisi).")
    public ResponseEntity<List<Map<String, Object>>> getStaffAttendance() {
        return ResponseEntity.ok(pointages.findAllByOrderByCreatedAtDesc().stream().map(this::pointage).toList());
    }

    @PostMapping("/attendance")
    @Operation(summary = "Enregistrer un pointage personnel", description = "Enregistre un pointage pour un membre du personnel de l'école.")
    public ResponseEntity<Map<String, Object>> addStaffAttendance(@RequestBody Map<String, Object> body) {
        String userId = texte(body.get("userId"));
        String name = texte(body.get("name"));
        if (userId != null) {
            UserJpaEntity u = comptesEcole.membre(userId, null);   // personnel de CETTE école
            name = ComptesEcoleService.nomComplet(u);
        }
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le membre du personnel est obligatoire.");
        }
        PointagePersonnelJpaEntity p = new PointagePersonnelJpaEntity();
        p.setId("att-" + UUID.randomUUID());
        p.setUserId(userId);
        p.setName(name);
        p.setCreatedAt(LocalDateTime.now());
        appliquer(p, body);
        pointages.save(p);
        actionLogger.logAdminAction("ADD_ATTENDANCE", "ADMIN", p.getId(), "Pointage enregistré pour " + name);
        return ResponseEntity.status(HttpStatus.CREATED).body(pointage(p));
    }

    @PutMapping("/attendance/{id}")
    @Operation(summary = "Mettre à jour un pointage", description = "Met à jour l'émargement d'un collaborateur (horaires, statut, motif).")
    public ResponseEntity<Map<String, Object>> updateStaffAttendance(
            @PathVariable("id") String id,
            @RequestBody Map<String, Object> updates) {
        return pointages.findById(id).map(p -> {   // @TenantId : un pointage d'une autre école est introuvable
            appliquer(p, updates);
            pointages.save(p);
            actionLogger.logAdminAction("UPDATE_ATTENDANCE", "ADMIN", id, "Pointage mis à jour pour " + p.getName());
            return ResponseEntity.ok(pointage(p));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/staff")
    @Operation(summary = "Liste des membres du personnel", description = "Personnel de l'école courante.")
    public ResponseEntity<?> getStaffMembers() {
        return ResponseEntity.ok(staff().getStaffMembers());
    }

    @PostMapping("/staff")
    @Operation(summary = "Créer un membre du personnel", description = "Crée le compte d'un collaborateur dans l'école courante (mot de passe saisi ou provisoire).")
    public ResponseEntity<?> createStaffMember(@RequestBody Map<String, Object> body) {
        return staff().createStaffMember(body);
    }

    @PutMapping("/staff/{id}")
    @Operation(summary = "Modifier un membre du personnel", description = "Met à jour les coordonnées et le rôle d'un collaborateur de l'école.")
    public ResponseEntity<Map<String, Object>> updateStaffMember(
            @PathVariable("id") String id,
            @RequestBody Map<String, Object> updates) {
        logger.info("PUT /admin/staff/{} - Modification collaborateur", id);
        return ResponseEntity.ok(staff().updateStaffMember(id, updates));
    }

    @PatchMapping("/staff/{id}/toggle-status")
    @Operation(summary = "Suspendre/Réactiver un membre du personnel", description = "Bascule l'appartenance du collaborateur à l'école (ACTIVE ⇄ SUSPENDED).")
    public ResponseEntity<Map<String, Object>> toggleStaffStatus(@PathVariable("id") String id) {
        String statut = comptesEcole.basculerStatut(id, null);
        actionLogger.logAdminAction("TOGGLE_STAFF_STATUS", "ADMIN", id, "Statut collaborateur : " + statut);
        return ResponseEntity.ok(Map.of("id", id, "status", ComptesEcoleService.libelleStatut(statut)));
    }

    @DeleteMapping("/staff/{id}")
    @Operation(summary = "Retirer un membre du personnel", description = "Retire le collaborateur de l'école courante.")
    public ResponseEntity<Map<String, Object>> deleteStaffMember(@PathVariable("id") String id) {
        logger.info("DELETE /admin/staff/{} - Retrait collaborateur", id);
        comptesEcole.retirer(id, null);
        actionLogger.logAdminAction("DELETE_STAFF", "ADMIN", id, "Collaborateur retiré de l'école : " + id);
        return ResponseEntity.ok(Map.of("success", true, "id", id));
    }

    @PostMapping("/courses")
    @Operation(summary = "Créer un cours", description = "Crée une matière pour une classe de l'école, confiée à un professeur de l'école.")
    public ResponseEntity<Map<String, Object>> createCourse(@RequestBody Map<String, Object> body) {
        if (courseJpaRepository == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Enregistrement des cours indisponible.");
        }
        String titre = premier(texte(body.get("titre")), texte(body.get("name")));
        String profId = premier(texte(body.get("professeur_id")), texte(body.get("profId")));
        String promoId = premier(texte(body.get("promotion_id")), texte(body.get("classe")));
        if (titre == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'intitulé du cours est obligatoire.");
        if (promoId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La classe du cours est obligatoire.");
        if (profId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le professeur du cours est obligatoire.");
        comptesEcole.membre(profId, "PROFESSEUR");   // professeur de CETTE école
        int heures = entier(premier(texte(body.get("volumeHoraire")), texte(body.get("totalHours"))));

        String id = "crs-" + UUID.randomUUID();
        courseJpaRepository.save(new CourseJpaEntity(id, titre, promoId, heures, profId));
        logger.info("POST /admin/courses - Cours créé: {} ({})", id, titre);
        actionLogger.logAdminAction("CREATE_COURSE", "ADMIN", id, "Cours créé : " + titre);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "id", id, "titre", titre,
                "professeur_id", profId, "promotion_id", promoId));
    }

    @PostMapping("/promotions")
    @Operation(summary = "Créer une promotion / classe", description = "Enregistre une nouvelle classe de l'école courante.")
    public ResponseEntity<Map<String, Object>> createPromotion(@RequestBody Map<String, Object> body) {
        String name = texte(body.get("name"));
        if (name == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom de la promotion est obligatoire.");
        boolean existe = classeJpaRepository.findAll().stream()
                .anyMatch(c -> name.equalsIgnoreCase(c.getName() != null ? c.getName() : c.getNom()));
        if (existe) throw new ResponseStatusException(HttpStatus.CONFLICT, "Une promotion porte déjà ce nom.");
        ClasseJpaEntity c = new ClasseJpaEntity();
        c.setId("cls-" + UUID.randomUUID());
        c.setName(name);
        c.setFiliereId(texte(body.get("filiere")));
        c.setLevel(texte(body.get("level")));
        c.setCapacity(entier(texte(body.get("capacity"))));
        classeJpaRepository.save(c);   // organization_id posé par @TenantId
        logger.info("POST /admin/promotions - Promotion créée: {} ({})", c.getId(), name);
        actionLogger.logAdminAction("CREATE_PROMOTION", "ADMIN", c.getId(), "Promotion créée : " + name);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("id", c.getId());
        res.put("name", name);
        res.put("filiere", c.getFiliereId());
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    // ---------------------------------------------------------------- aides

    private ecole_221.sn.ecole.modules.admin.application.service.AdminStaffService staff() {
        if (adminStaffService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Gestion du personnel indisponible.");
        }
        return adminStaffService;
    }

    private List<Map<String, Object>> eleves() {
        Map<String, ClasseJpaEntity> classesParId = new HashMap<>();
        classeJpaRepository.findAll().forEach(c -> classesParId.put(c.getId(), c));   // @TenantId
        Map<String, ClasseJpaEntity> classeParEleve = new HashMap<>();
        inscriptions.findAll().forEach(i -> {                                        // @TenantId
            ClasseJpaEntity c = classesParId.get(i.getClasseId());
            if (c != null) classeParEleve.put(i.getUserId(), c);
        });
        return comptesEcole.membres("ETUDIANT").stream()
                .map(e -> eleve(e.getKey(), e.getValue().getStatus(), classeParEleve)).toList();
    }

    /** Classe de l'école désignée par son identifiant ; null si non fourni ; 400 si inconnue dans cette école. */
    private ClasseJpaEntity classeDeLEcole(String classeId) {
        if (classeId == null || classeId.isBlank()) return null;
        return classeJpaRepository.findById(classeId.trim())   // @TenantId : une classe d'une autre école est introuvable
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Classe inconnue dans cet établissement."));
    }

    private static String nomClasse(ClasseJpaEntity c) {
        return c.getName() != null ? c.getName() : c.getNom();
    }

    /** Fiche élève : uniquement des champs réels ; moyenne / présence n'ont pas encore de source → absents. */
    private Map<String, Object> eleve(UserJpaEntity s, String statutAppartenance, Map<String, ClasseJpaEntity> classeParEleve) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", s.getId());
        map.put("name", ComptesEcoleService.nomComplet(s));
        map.put("prenom", s.getPrenom());
        map.put("nom", s.getNom());
        map.put("matricule", s.getMatricule());
        map.put("email", s.getEmail());
        map.put("telephone", s.getTelephone());
        map.put("photoUrl", s.getPhotoUrl());
        ClasseJpaEntity classe = classeParEleve == null ? null : classeParEleve.get(s.getId());
        map.put("classeId", classe == null ? null : classe.getId());
        map.put("classe", classe == null ? null : nomClasse(classe));
        StatutFrais frais = adminRepository.getStudentStatus(s.getId());
        String libelle = frais == null ? null : frais.getDisplayName();   // null = non renseigné
        map.put("statutFrais", libelle);
        map.put("financialStatus", libelle);
        map.put("status", ComptesEcoleService.libelleStatut(statutAppartenance));
        map.put("qrStatus", qrStatus(statutAppartenance));
        return map;
    }

    private Map<String, Object> professeur(UserJpaEntity p, OrganizationUserJpaEntity lien) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", p.getId());
        map.put("name", ComptesEcoleService.nomComplet(p));
        map.put("email", p.getEmail());
        map.put("telephone", p.getTelephone());
        map.put("matricule", p.getMatricule());
        map.put("status", ComptesEcoleService.libelleStatut(lien == null ? "ACTIVE" : lien.getStatus()));
        return map;
    }

    private Map<String, Object> pointage(PointagePersonnelJpaEntity p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("userId", p.getUserId());
        m.put("name", p.getName());
        m.put("role", p.getRole());
        m.put("checkIn", p.getCheckIn());
        m.put("checkOut", p.getCheckOut());
        m.put("hours", p.getHours());
        m.put("status", p.getStatus());
        m.put("note", p.getNote());
        m.put("date", p.getCreatedAt() == null ? null : p.getCreatedAt().toLocalDate().toString());
        return m;
    }

    private static void appliquer(PointagePersonnelJpaEntity p, Map<String, Object> b) {
        if (b.containsKey("role")) p.setRole(texte(b.get("role")));
        if (b.containsKey("checkIn")) p.setCheckIn(texte(b.get("checkIn")));
        if (b.containsKey("checkOut")) p.setCheckOut(texte(b.get("checkOut")));
        if (b.containsKey("status")) p.setStatus(texte(b.get("status")));
        if (b.containsKey("note")) p.setNote(texte(b.get("note")));
        if (b.containsKey("hours")) {
            String h = texte(b.get("hours"));
            try {
                p.setHours(h == null ? null : Double.valueOf(h.replace(',', '.')));
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre d'heures invalide.");
            }
        }
    }

    private static String qrStatus(String statutAppartenance) {
        return "ACTIVE".equalsIgnoreCase(statutAppartenance) ? "AUTORISÉ" : "SUSPENDU";
    }

    /** Statut saisi : « En Règle » ou « En Retard » uniquement (avant : toute valeur inconnue devenait « En Retard »). */
    private static StatutFrais statutFraisStrict(String saisi) {
        if (saisi == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut de frais manquant.");
        String v = saisi.trim().toLowerCase(Locale.ROOT);
        return switch (v) {
            case "en règle", "en regle", "regle", "paye", "en_regle" -> StatutFrais.EN_REGLE;
            case "en retard", "retard", "impaye", "en_retard" -> StatutFrais.EN_RETARD;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Statut de frais invalide : « En Règle » ou « En Retard » attendu.");
        };
    }

    /** Prénom / nom : champs séparés s'ils sont fournis, sinon découpe de « name ». */
    private static String[] noms(Map<String, String> body) {
        if (body.get("prenom") != null || body.get("nom") != null) {
            return new String[] {body.get("prenom"), body.get("nom")};
        }
        if (body.get("firstName") != null || body.get("lastName") != null) {
            return new String[] {body.get("firstName"), body.get("lastName")};
        }
        return ComptesEcoleService.decouperNom(body.get("name"));
    }

    private static String texte(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }

    private static String premier(String a, String b) {
        return a != null && !a.isBlank() ? a : (b != null && !b.isBlank() ? b : null);
    }

    private static int entier(String s) {
        if (s == null) return 0;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre invalide : " + s);
        }
    }
}
