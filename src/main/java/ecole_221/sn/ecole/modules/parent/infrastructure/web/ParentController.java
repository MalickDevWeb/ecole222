package ecole_221.sn.ecole.modules.parent.infrastructure.web;

import ecole_221.sn.ecole.modules.parent.application.port.dto.*;
import ecole_221.sn.ecole.modules.parent.application.port.usecase.ParentUseCase;
import ecole_221.sn.ecole.infrastructure.security.JwtService;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour le module Parent.
 * Expose les endpoints pour la gestion des parents.
 */
@RestController
@RequestMapping("/parent")
@Tag(name = "Parent", description = "Gestion des parents - enfants, stats, messages, paiements")
public class ParentController {

    private static final Logger logger = LoggerFactory.getLogger(ParentController.class);
    private final ParentUseCase parentUseCase;
    private final JwtService jwtService;
    private final UserRepositoryPort userRepositoryPort;
    private final ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort appartenances;

    @org.springframework.beans.factory.annotation.Autowired
    public ParentController(ParentUseCase parentUseCase, JwtService jwtService, UserRepositoryPort userRepositoryPort,
                            ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort appartenances) {
        this.parentUseCase = parentUseCase;
        this.jwtService = jwtService;
        this.userRepositoryPort = userRepositoryPort;
        this.appartenances = appartenances;
    }

    /** Compatibilité (tests) : sans annuaire des appartenances, aucun professeur n'est listé. */
    public ParentController(ParentUseCase parentUseCase, JwtService jwtService, UserRepositoryPort userRepositoryPort) {
        this(parentUseCase, jwtService, userRepositoryPort, null);
    }

    // ==================== ENDPOINTS ENFANTS ====================

    @GetMapping("/enfants")
    @Operation(summary = "Liste des enfants du parent", description = "Retourne la liste des enfants liés au parent connecté")
    public ResponseEntity<List<ChildDto>> getEnfants(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String parentId = extractParentIdFromToken(authHeader);
        logger.info("GET /parent/enfants - Récupération enfants du parent: {}", parentId);
        List<ChildDto> enfants = parentUseCase.getEnfantsByParentId(parentId);
        return ResponseEntity.ok(enfants);
    }

    // ==================== ENDPOINTS STATS ====================

    @GetMapping("/enfants/{enfantId}/stats")
    @Operation(summary = "Stats d'un enfant", description = "Retourne le tableau de bord complet d'un enfant")
    public ResponseEntity<ChildStatsDto> getChildStats(
            @Parameter(description = "ID de l'enfant") @PathVariable String enfantId,
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String parentId = extractParentIdFromToken(authHeader);
        
        // Vérifier que l'enfant appartient bien au parent (sécurité)
        if (!parentUseCase.getEnfantsByParentId(parentId).stream()
                .anyMatch(child -> child.id().equals(enfantId))) {
            return ResponseEntity.status(403).build(); // Forbidden
        }
        
        logger.info("GET /parent/enfants/{}/stats - Stats enfant", enfantId);
        ChildStatsDto stats = parentUseCase.getChildStats(enfantId);
        return ResponseEntity.ok(stats);
    }

    // ==================== ENDPOINTS PRESENCES ====================

    @GetMapping("/enfants/{enfantId}/presences")
    @Operation(summary = "Historique des présences", description = "Retourne l'historique des présences de l'enfant")
    public ResponseEntity<List<PresenceDto>> getChildPresences(
            @Parameter(description = "ID de l'enfant") @PathVariable String enfantId,
            @Parameter(description = "Mois au format YYYY-MM") @RequestParam(required = false) String mois,
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String parentId = extractParentIdFromToken(authHeader);
        
        // Vérifier que l'enfant appartient bien au parent
        if (!parentUseCase.getEnfantsByParentId(parentId).stream()
                .anyMatch(child -> child.id().equals(enfantId))) {
            return ResponseEntity.status(403).build();
        }
        
        logger.info("GET /parent/enfants/{}/presences - Présences enfant", enfantId);
        List<PresenceDto> presences = parentUseCase.getChildPresences(enfantId, mois);
        return ResponseEntity.ok(presences);
    }

    // ==================== ENDPOINTS NOTES ====================

    @GetMapping("/enfants/{enfantId}/notes")
    @Operation(summary = "Notes de l'enfant", description = "Retourne les notes et résultats académiques de l'enfant")
    public ResponseEntity<List<GradeDto>> getChildGrades(
            @Parameter(description = "ID de l'enfant") @PathVariable String enfantId,
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String parentId = extractParentIdFromToken(authHeader);
        
        // Vérifier que l'enfant appartient bien au parent
        if (!parentUseCase.getEnfantsByParentId(parentId).stream()
                .anyMatch(child -> child.id().equals(enfantId))) {
            return ResponseEntity.status(403).build();
        }
        
        logger.info("GET /parent/enfants/{}/notes - Notes enfant", enfantId);
        List<GradeDto> grades = parentUseCase.getChildGrades(enfantId);
        return ResponseEntity.ok(grades);
    }

    // ==================== ENDPOINTS PLANNING ====================

    @GetMapping("/enfants/{enfantId}/planning")
    @Operation(summary = "Emploi du temps de l'enfant", description = "Retourne l'emploi du temps hebdomadaire de l'enfant")
    public ResponseEntity<List<ScheduleDto>> getChildSchedule(
            @Parameter(description = "ID de l'enfant") @PathVariable String enfantId,
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String parentId = extractParentIdFromToken(authHeader);
        
        // Vérifier que l'enfant appartient bien au parent
        if (!parentUseCase.getEnfantsByParentId(parentId).stream()
                .anyMatch(child -> child.id().equals(enfantId))) {
            return ResponseEntity.status(403).build();
        }
        
        logger.info("GET /parent/enfants/{}/planning - Planning enfant", enfantId);
        List<ScheduleDto> schedule = parentUseCase.getChildSchedule(enfantId);
        return ResponseEntity.ok(schedule);
    }

    // ==================== ENDPOINTS MESSAGES ====================

    // Messagerie parent ↔ école : MessagerieParentsController (messages réellement enregistrés)

    // ==================== ENDPOINTS CANDIDATURES ====================

    @GetMapping("/candidatures")
    @Operation(summary = "Liste des candidatures", description = "Retourne la liste des dossiers d'inscription soumis par le parent")
    public ResponseEntity<List<ApplicationDto>> getApplications(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String parentId = extractParentIdFromToken(authHeader);
        logger.info("GET /parent/candidatures - Candidatures du parent: {}", parentId);
        List<ApplicationDto> applications = parentUseCase.getParentApplications(parentId);
        return ResponseEntity.ok(applications);
    }

    @PostMapping("/inscrire-enfant")
    @Operation(summary = "Soumettre un dossier de candidature", description = "Soumet un nouveau dossier de candidature pour un enfant")
    public ResponseEntity<Map<String, Object>> enrollChild(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody EnrollChildPayloadDto payload) {
        String parentId = extractParentIdFromToken(authHeader);
        logger.info("POST /parent/inscrire-enfant - Candidature par: {}", parentId);
        String dossierId = parentUseCase.enrollChild(parentId, payload);
        return ResponseEntity.ok(Map.of("success", true, "dossierId", dossierId));
    }

    // ==================== ENDPOINTS PAIEMENTS ====================

    @GetMapping("/paiements")
    @Operation(summary = "Détail des paiements", description = "Retourne le détail de la scolarité et des échéances de paiement")
    public ResponseEntity<PaymentDto> getPayments(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String parentId = extractParentIdFromToken(authHeader);
        logger.info("GET /parent/paiements - Paiements du parent: {}", parentId);
        PaymentDto payments = parentUseCase.getParentPayments(parentId);
        return ResponseEntity.ok(payments);
    }

    // ==================== ENDPOINTS PROFIL ====================

    @GetMapping("/profil")
    @Operation(summary = "Profil du parent", description = "Coordonnées enregistrées du parent connecté")
    public ResponseEntity<ParentProfileDto> getProfile(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return ResponseEntity.ok(parentUseCase.getParentProfile(extractParentIdFromToken(authHeader)));
    }

    @PatchMapping("/profil")
    @Operation(summary = "Mettre à jour le profil", description = "Met à jour les informations du profil parent")
    public ResponseEntity<ParentProfileDto> updateProfile(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody ParentProfileDto profileDto) {
        String parentId = extractParentIdFromToken(authHeader);
        logger.info("PATCH /parent/profil - Mise à jour profil parent: {}", parentId);
        ParentProfileDto updatedProfile = parentUseCase.updateParentProfile(parentId, profileDto);
        return ResponseEntity.ok(updatedProfile);
    }

    // ==================== ENDPOINTS RDV ====================

    @PostMapping("/rdv")
    @Operation(summary = "Demander un rendez-vous", description = "Demande un rendez-vous avec un professeur")
    public ResponseEntity<Map<String, Object>> requestAppointment(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody AppointmentRequestDto request) {
        String parentId = extractParentIdFromToken(authHeader);
        logger.info("POST /parent/rdv - Demande RDV par: {}", parentId);
        String rdvId = parentUseCase.requestAppointment(parentId, request);
        return ResponseEntity.ok(Map.of("success", true, "rdvId", rdvId));
    }

    @GetMapping("/rdv")
    @Operation(summary = "Liste des rendez-vous", description = "Retourne la liste des rendez-vous et visioconférences planifiés pour le parent")
    public ResponseEntity<List<Map<String, Object>>> getAppointments(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String parentId = extractParentIdFromToken(authHeader);
        logger.info("GET /parent/rdv - Récupération rendez-vous parent: {}", parentId);
        return ResponseEntity.ok(parentUseCase.getParentAppointments(parentId));
    }

    @GetMapping("/professeurs")
    @Operation(summary = "Liste des professeurs", description = "Retourne la liste des professeurs pour la prise de rendez-vous")
    public ResponseEntity<List<Map<String, Object>>> getProfesseurs(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // APP-BE-17 / APP-SEC-17 : uniquement les professeurs ACTIFS de l'école courante.
        // Avant : tous les comptes PROFESSEUR de toutes les écoles (emails compris), titre « Dr. »
        // ajouté à tous, et deux professeurs inventés si la liste était vide.
        String ecole = ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext.getCurrentOrganizationId().orElse(null);
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        if (ecole != null && appartenances != null) {
            for (var m : appartenances.findByOrganizationIdAndRole(ecole, RoleUtilisateur.PROFESSEUR.value())) {
                if (!m.isActive()) {
                    continue;
                }
                userRepositoryPort.findById(m.getUserId()).ifPresent(p -> {
                    Map<String, Object> map = new java.util.HashMap<String, Object>();
                    map.put("id", p.getId());
                    map.put("nom", p.getNom());
                    map.put("prenom", p.getPrenom());
                    map.put("email", p.getEmail());
                    map.put("displayName", ((p.getPrenom() == null ? "" : p.getPrenom() + " ") + (p.getNom() == null ? "" : p.getNom())).trim());
                    result.add(map);
                });
            }
        }
        return ResponseEntity.ok(result);
    }

    // Justificatifs d'absence : JustificatifsAbsenceParentController (enregistrés et validés par l'administration)

    // ==================== UTILITY METHODS ====================

    /**
     * Extrait l'ID du parent à partir du token JWT et vérifie le rôle.
     */
    /**
     * Identifiant du parent connecté (APP-SEC-11).
     * L'utilisateur est celui authentifié par le filtre JWT (cookie ou en-tête Bearer) ; l'en-tête
     * n'est plus redécodé ici, ce qui faisait échouer les appels authentifiés par cookie.
     * Le paramètre authHeader est conservé pour la signature des routes (documentation OpenAPI masquée).
     */
    private String extractParentIdFromToken(String authHeader) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");
        }
        if (user.getRole() == null || !"PARENT".equals(user.getRole().value())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé: rôle incorrect. Rôle requis: PARENT");
        }
        return user.getId();
    }
}
