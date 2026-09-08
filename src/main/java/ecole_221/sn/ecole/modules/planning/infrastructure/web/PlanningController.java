package ecole_221.sn.ecole.modules.planning.infrastructure.web;

import ecole_221.sn.ecole.modules.planning.application.port.dto.*;
import ecole_221.sn.ecole.modules.planning.application.port.usecase.PlanningUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour le module Planning.
 * Expose les endpoints pour la gestion des salles, créneaux horaires, filières et classes.
 */
@RestController
@RequestMapping("/planning")
@CrossOrigin(origins = "*")
@Tag(name = "Planning", description = "Gestion du planning - salles, créneaux, filières, classes")
public class PlanningController {
    
    private static final Logger logger = LoggerFactory.getLogger(PlanningController.class);
    private final PlanningUseCase planningUseCase;
    
    public PlanningController(PlanningUseCase planningUseCase) {
        this.planningUseCase = planningUseCase;
    }
    
    // ==================== ENDPOINTS SALLES ====================
    
    @GetMapping("/rooms")
    @Operation(summary = "Liste des salles", description = "Retourne toutes les salles disponibles.")
    public ResponseEntity<List<RoomDto>> getAllRooms() {
        logger.info("GET /planning/rooms - Liste des salles");
        return ResponseEntity.ok(planningUseCase.getAllRooms());
    }
    
    @GetMapping("/rooms/{roomId}")
    @Operation(summary = "Détails d'une salle", description = "Retourne les détails d'une salle par son identifiant.")
    public ResponseEntity<RoomDto> getRoomById(
            @Parameter(description = "Identifiant de la salle") @PathVariable("roomId") String roomId) {
        logger.info("GET /planning/rooms/{} - Détails salle", roomId);
        RoomDto roomDto = planningUseCase.getRoomById(roomId);
        if (roomDto != null) {
            return ResponseEntity.ok(roomDto);
        }
        return ResponseEntity.notFound().build();
    }
    
    @PostMapping("/rooms")
    @Operation(summary = "Créer/Mettre à jour une salle", description = "Crée une nouvelle salle ou met à jour une salle existante.")
    public ResponseEntity<RoomDto> saveRoom(@RequestBody RoomDto roomDto) {
        logger.info("POST /planning/rooms - Création/Mise à jour salle: {}", roomDto.id());
        return ResponseEntity.ok(planningUseCase.saveRoom(roomDto));
    }
    
    @DeleteMapping("/rooms/{roomId}")
    @Operation(summary = "Supprimer une salle", description = "Supprime une salle par son identifiant.")
    public ResponseEntity<Void> deleteRoom(
            @Parameter(description = "Identifiant de la salle") @PathVariable("roomId") String roomId) {
        logger.info("DELETE /planning/rooms/{} - Suppression salle", roomId);
        planningUseCase.deleteRoom(roomId);
        return ResponseEntity.noContent().build();
    }
    
    // ==================== ENDPOINTS CRÉNEAUX HORAIRES ====================
    
    @GetMapping("/slots")
    @Operation(summary = "Liste des créneaux horaires", description = "Retourne tous les créneaux horaires.")
    public ResponseEntity<List<SlotDto>> getAllSlots() {
        logger.info("GET /planning/slots - Liste des créneaux horaires");
        return ResponseEntity.ok(planningUseCase.getAllSlots());
    }
    
    @GetMapping("/slots/{slotId}")
    @Operation(summary = "Détails d'un créneau horaire", description = "Retourne les détails d'un créneau horaire.")
    public ResponseEntity<SlotDto> getSlotById(
            @Parameter(description = "Identifiant du créneau") @PathVariable("slotId") String slotId) {
        logger.info("GET /planning/slots/{} - Détails créneau", slotId);
        SlotDto slotDto = planningUseCase.getSlotById(slotId);
        if (slotDto != null) {
            return ResponseEntity.ok(slotDto);
        }
        return ResponseEntity.notFound().build();
    }
    
    @PostMapping("/slots")
    @Operation(summary = "Créer/Mettre à jour un créneau horaire", description = "Crée un nouveau créneau ou met à jour un créneau existant.")
    public ResponseEntity<SlotDto> saveSlot(@RequestBody SlotDto slotDto) {
        logger.info("POST /planning/slots - Création/Mise à jour créneau: {}", slotDto.id());
        return ResponseEntity.ok(planningUseCase.saveSlot(slotDto));
    }
    
    @DeleteMapping("/slots/{slotId}")
    @Operation(summary = "Supprimer un créneau horaire", description = "Supprime un créneau horaire.")
    public ResponseEntity<Void> deleteSlot(
            @Parameter(description = "Identifiant du créneau") @PathVariable("slotId") String slotId) {
        logger.info("DELETE /planning/slots/{} - Suppression créneau", slotId);
        planningUseCase.deleteSlot(slotId);
        return ResponseEntity.noContent().build();
    }
    
    // ==================== ENDPOINTS FILIÈRES ====================
    
    @GetMapping("/filieres")
    @Operation(summary = "Liste des filières", description = "Retourne toutes les filières.")
    public ResponseEntity<List<FiliereDto>> getAllFilieres() {
        logger.info("GET /planning/filieres - Liste des filières");
        return ResponseEntity.ok(planningUseCase.getAllFilieres());
    }
    
    @GetMapping("/filieres/{filiereId}")
    @Operation(summary = "Détails d'une filière", description = "Retourne les détails d'une filière.")
    public ResponseEntity<FiliereDto> getFiliereById(
            @Parameter(description = "Identifiant de la filière") @PathVariable("filiereId") String filiereId) {
        logger.info("GET /planning/filieres/{} - Détails filière", filiereId);
        return ResponseEntity.ok(planningUseCase.getFiliereById(filiereId));
    }
    
    @PostMapping("/filieres")
    @Operation(summary = "Créer/Mettre à jour une filière", description = "Crée une nouvelle filière ou met à jour une filière existante.")
    public ResponseEntity<FiliereDto> saveFiliere(@RequestBody FiliereDto filiereDto) {
        logger.info("POST /planning/filieres - Création/Mise à jour filière: {}", filiereDto.id());
        return ResponseEntity.ok(planningUseCase.saveFiliere(filiereDto));
    }
    
    @DeleteMapping("/filieres/{filiereId}")
    @Operation(summary = "Supprimer une filière", description = "Supprime une filière.")
    public ResponseEntity<Void> deleteFiliere(
            @Parameter(description = "Identifiant de la filière") @PathVariable("filiereId") String filiereId) {
        logger.info("DELETE /planning/filieres/{} - Suppression filière", filiereId);
        planningUseCase.deleteFiliere(filiereId);
        return ResponseEntity.noContent().build();
    }
    
    // ==================== ENDPOINTS CLASSES ====================
    
    @GetMapping("/classes")
    @Operation(summary = "Liste des classes", description = "Retourne toutes les classes avec pagination.")
    public ResponseEntity<ecole_221.sn.ecole.partage.PageResult<ClasseDto>> getAllClasses(
            @Parameter(description = "Numéro de la page") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(value = "size", defaultValue = "10") int size) {
        logger.info("GET /planning/classes - Liste des classes (page={}, size={})", page, size);
        return ResponseEntity.ok(planningUseCase.getAllClasses(page, size));
    }
    
    @GetMapping("/classes/{classeId}")
    @Operation(summary = "Détails d'une classe", description = "Retourne les détails d'une classe.")
    public ResponseEntity<ClasseDto> getClasseById(
            @Parameter(description = "Identifiant de la classe") @PathVariable("classeId") String classeId) {
        logger.info("GET /planning/classes/{} - Détails classe", classeId);
        return ResponseEntity.ok(planningUseCase.getClasseById(classeId));
    }
    
    @PostMapping("/classes")
    @Operation(summary = "Créer/Mettre à jour une classe", description = "Crée une nouvelle classe ou met à jour une classe existante.")
    public ResponseEntity<ClasseDto> saveClasse(@RequestBody ClasseDto classeDto) {
        logger.info("POST /planning/classes - Création/Mise à jour classe: {}", classeDto.id());
        return ResponseEntity.ok(planningUseCase.saveClasse(classeDto));
    }
    
    @DeleteMapping("/classes/{classeId}")
    @Operation(summary = "Supprimer une classe", description = "Supprime une classe.")
    public ResponseEntity<Void> deleteClasse(
            @Parameter(description = "Identifiant de la classe") @PathVariable("classeId") String classeId) {
        logger.info("DELETE /planning/classes/{} - Suppression classe", classeId);
        planningUseCase.deleteClasse(classeId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/filieres/{filiereId}/classes")
    @Operation(summary = "Classes d'une filière", description = "Retourne toutes les classes d'une filière.")
    public ResponseEntity<List<ClasseDto>> getClassesByFiliere(
            @Parameter(description = "Identifiant de la filière") @PathVariable("filiereId") String filiereId) {
        logger.info("GET /planning/filieres/{}/classes - Classes de la filière", filiereId);
        return ResponseEntity.ok(planningUseCase.getClassesByFiliere(filiereId));
    }
}