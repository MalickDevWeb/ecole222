package ecole_221.sn.ecole.modules.parent.infrastructure.web;

import ecole_221.sn.ecole.modules.parent.application.port.dto.*;
import ecole_221.sn.ecole.modules.parent.application.port.usecase.ParentUseCase;
import ecole_221.sn.ecole.infrastructure.security.JwtService;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
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
@CrossOrigin(origins = "*")
@Tag(name = "Parent", description = "Gestion des parents - enfants, stats, messages, paiements")
public class ParentController {

    private static final Logger logger = LoggerFactory.getLogger(ParentController.class);
    private final ParentUseCase parentUseCase;
    private final JwtService jwtService;
    private final UserRepositoryPort userRepositoryPort;

    public ParentController(ParentUseCase parentUseCase, JwtService jwtService, UserRepositoryPort userRepositoryPort) {
        this.parentUseCase = parentUseCase;
        this.jwtService = jwtService;
        this.userRepositoryPort = userRepositoryPort;
    }

    // ==================== ENDPOINTS ENFANTS ====================

    @GetMapping("/enfants")
    @Operation(summary = "Liste des enfants du parent", description = "Retourne la liste des enfants liés au parent connecté")
    public ResponseEntity<List<ChildDto>> getEnfants(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
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
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
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
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
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
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
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
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
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

    @GetMapping("/messages")
    @Operation(summary = "Boîte de réception", description = "Retourne la liste des messages reçus par le parent")
    public ResponseEntity<List<MessageDto>> getMessages(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        String parentId = extractParentIdFromToken(authHeader);
        logger.info("GET /parent/messages - Messages du parent: {}", parentId);
        List<MessageDto> messages = parentUseCase.getParentMessages(parentId);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/messages")
    @Operation(summary = "Envoyer un message", description = "Le parent envoie un message à un professeur ou à l'administration")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @RequestBody SendMessageRequestDto request) {
        String parentId = extractParentIdFromToken(authHeader);
        
        // Validation
        if (request.destinataireId() == null || request.destinataireId().isBlank() ||
            request.sujet() == null || request.sujet().isBlank() ||
            request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tous les champs sont obligatoires"));
        }
        
        logger.info("POST /parent/messages - Envoi message par: {}", parentId);
        MessageDto message = parentUseCase.sendMessage(parentId, request);
        return ResponseEntity.ok(Map.of("success", true, "message", message));
    }

    @PatchMapping("/messages/{messageId}/lu")
    @Operation(summary = "Marquer comme lu", description = "Marque un message comme lu")
    public ResponseEntity<Map<String, Object>> markMessageAsRead(
            @Parameter(description = "ID du message") @PathVariable String messageId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        String parentId = extractParentIdFromToken(authHeader);
        logger.info("PATCH /parent/messages/{}/lu - Marquer message comme lu", messageId);
        MessageDto message = parentUseCase.markMessageAsRead(messageId, parentId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ==================== ENDPOINTS CANDIDATURES ====================

    @GetMapping("/candidatures")
    @Operation(summary = "Liste des candidatures", description = "Retourne la liste des dossiers d'inscription soumis par le parent")
    public ResponseEntity<List<ApplicationDto>> getApplications(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        String parentId = extractParentIdFromToken(authHeader);
        logger.info("GET /parent/candidatures - Candidatures du parent: {}", parentId);
        List<ApplicationDto> applications = parentUseCase.getParentApplications(parentId);
        return ResponseEntity.ok(applications);
    }

    @PostMapping("/inscrire-enfant")
    @Operation(summary = "Soumettre un dossier de candidature", description = "Soumet un nouveau dossier de candidature pour un enfant")
    public ResponseEntity<Map<String, Object>> enrollChild(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
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
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        String parentId = extractParentIdFromToken(authHeader);
        logger.info("GET /parent/paiements - Paiements du parent: {}", parentId);
        PaymentDto payments = parentUseCase.getParentPayments(parentId);
        return ResponseEntity.ok(payments);
    }

    // ==================== ENDPOINTS PROFIL ====================

    @PatchMapping("/profil")
    @Operation(summary = "Mettre à jour le profil", description = "Met à jour les informations du profil parent")
    public ResponseEntity<ParentProfileDto> updateProfile(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
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
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @RequestBody AppointmentRequestDto request) {
        String parentId = extractParentIdFromToken(authHeader);
        logger.info("POST /parent/rdv - Demande RDV par: {}", parentId);
        String rdvId = parentUseCase.requestAppointment(parentId, request);
        return ResponseEntity.ok(Map.of("success", true, "rdvId", rdvId));
    }

    // ==================== ENDPOINTS JUSTIFICATIFS ====================

    @PostMapping("/presences/{presenceId}/justifier")
    @Operation(summary = "Justifier une absence", description = "Soumet un justificatif pour une absence/retard")
    public ResponseEntity<Map<String, Object>> justifyAbsence(
            @Parameter(description = "ID de la présence") @PathVariable String presenceId,
            @Parameter(description = "Motif de l'absence") @RequestParam String motif,
            @Parameter(description = "URL du fichier justificatif") @RequestParam(required = false) String fileUrl,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        String parentId = extractParentIdFromToken(authHeader);
        logger.info("POST /parent/presences/{}/justifier - Justification absence", presenceId);
        String justificatifId = parentUseCase.justifyAbsence(presenceId, parentId, motif, fileUrl);
        return ResponseEntity.ok(Map.of("success", true, "justificatifId", justificatifId));
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Extrait l'ID du parent à partir du token JWT et vérifie le rôle.
     */
    private String extractParentIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token JWT manquant ou invalide");
        }
        
        String token = authHeader.substring(7);
        String username = jwtService.extractUsername(token);
        
        // Trouver l'utilisateur et vérifier le rôle
        User user = userRepositoryPort.findByEmail(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non trouvé"));
        
        // Vérifier que l'utilisateur a le rôle PARENT
        if (!"PARENT".equals(user.getRole().value())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé: rôle incorrect. Rôle requis: PARENT");
        }
        
        return user.getId();
    }
}
