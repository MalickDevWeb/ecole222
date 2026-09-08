package ecole_221.sn.ecole.modules.admin.infrastructure.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ecole_221.sn.ecole.modules.admin.application.port.usecase.PlanningUseCase;
import ecole_221.sn.ecole.modules.admin.domain.model.*;

import java.util.List;

@RestController
@RequestMapping("/admin/planning")
@CrossOrigin(origins = "*")
@Tag(name = "Admin Planning", description = "Planification Admin — salles, filieres, classes, crenaux horaires")
public class AdminPlanningController {

    private static final Logger logger = LoggerFactory.getLogger(AdminPlanningController.class);
    private final PlanningUseCase planningUseCase;

    public AdminPlanningController(PlanningUseCase planningUseCase) {
        this.planningUseCase = planningUseCase;
    }

    @GetMapping("/rooms")
    @Operation(summary = "Liste des salles", description = "Retourne toutes les salles disponibles.")
    public ResponseEntity<List<Room>> getRooms() {
        logger.info("GET /admin/planning/rooms - Liste des salles");
        return ResponseEntity.ok(planningUseCase.getRooms());
    }

    @PutMapping("/rooms")
    @Operation(summary = "Mettre a jour les salles", description = "Met a jour la liste complete des salles.")
    public ResponseEntity<Void> updateRooms(@RequestBody List<Room> rooms) {
        logger.info("PUT /admin/planning/rooms - Mise a jour des salles: {} salles", rooms.size());
        planningUseCase.updateRooms(rooms);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/filieres")
    @Operation(summary = "Liste des filieres", description = "Retourne toutes les filieres de l'ecole.")
    public ResponseEntity<List<Filiere>> getFilieres() {
        logger.info("GET /admin/planning/filieres - Liste des filieres");
        return ResponseEntity.ok(planningUseCase.getFilieres());
    }

    @PutMapping("/filieres")
    @Operation(summary = "Mettre a jour les filieres", description = "Met a jour la liste complete des filieres.")
    public ResponseEntity<Void> updateFilieres(@RequestBody List<Filiere> filieres) {
        logger.info("PUT /admin/planning/filieres - Mise a jour des filieres: {} filieres", filieres.size());
        planningUseCase.updateFilieres(filieres);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/classes")
    @Operation(summary = "Liste des classes", description = "Retourne toutes les classes de l'ecole.")
    public ResponseEntity<List<Classe>> getClasses() {
        logger.info("GET /admin/planning/classes - Liste des classes");
        return ResponseEntity.ok(planningUseCase.getClasses());
    }

    @PutMapping("/classes")
    @Operation(summary = "Mettre a jour les classes", description = "Met a jour la liste complete des classes.")
    public ResponseEntity<Void> updateClasses(@RequestBody List<Classe> classes) {
        logger.info("PUT /admin/planning/classes - Mise a jour des classes: {} classes", classes.size());
        planningUseCase.updateClasses(classes);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/slots")
    @Operation(summary = "Creneaux horaires", description = "Retourne tous les crenaux horaires disponibles.")
    public ResponseEntity<List<TimeSlot>> getSlots() {
        logger.info("GET /admin/planning/slots - Liste des crenaux horaires");
        return ResponseEntity.ok(planningUseCase.getSlots());
    }

    @PutMapping("/slots")
    @Operation(summary = "Mettre a jour les crenaux", description = "Met a jour la liste complete des crenaux horaires.")
    public ResponseEntity<Void> updateSlots(@RequestBody List<TimeSlot> slots) {
        logger.info("PUT /admin/planning/slots - Mise a jour des crenaux: {} crenaux", slots.size());
        planningUseCase.updateSlots(slots);
        return ResponseEntity.ok().build();
    }
}