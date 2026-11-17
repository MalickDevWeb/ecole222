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
import java.util.Map;

@RestController
@RequestMapping("/admin/planning")
@Tag(name = "Admin Planning", description = "Planification Admin — salles, filieres, classes, crenaux horaires")
public class AdminPlanningController {

    private static final Logger logger = LoggerFactory.getLogger(AdminPlanningController.class);
    private final PlanningUseCase planningUseCase;

    public AdminPlanningController(PlanningUseCase planningUseCase) {
        this.planningUseCase = planningUseCase;
    }

    /**
     * Chaque enregistrement de l'emploi du temps crée ou met à jour les séances des professeurs.
     * Facultatif pour les tests construits sans contexte Spring.
     */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.modules.admin.application.service.SeancesEmploiDuTempsService seancesEmploiDuTemps;

    void setSeancesEmploiDuTemps(ecole_221.sn.ecole.modules.admin.application.service.SeancesEmploiDuTempsService s) {
        this.seancesEmploiDuTemps = s;
    }

    @GetMapping("/rooms")
    @Operation(summary = "Liste des salles", description = "Retourne toutes les salles disponibles.")
    public ResponseEntity<List<Room>> getRooms() {
        logger.info("GET /admin/planning/rooms - Liste des salles");
        return ResponseEntity.ok(planningUseCase.getRooms());
    }

    @PutMapping("/rooms")
    @Operation(summary = "Mettre a jour les salles", description = "Met a jour la liste complete des salles.")
    public ResponseEntity<Map<String, Object>> updateRooms(@RequestBody List<Map<String, Object>> corps) {
        List<Room> rooms = PlanningPayloads.salles(corps);
        logger.info("PUT /admin/planning/rooms - Mise a jour des salles: {} salles", rooms.size());
        return ResponseEntity.ok(bilanCorbeille(ecole_221.sn.ecole.modules.corbeille.application.TypeCorbeille.SALLE, planningUseCase.updateRooms(rooms)));
    }

    @GetMapping("/filieres")
    @Operation(summary = "Liste des filieres", description = "Retourne toutes les filieres de l'ecole.")
    public ResponseEntity<List<Filiere>> getFilieres() {
        logger.info("GET /admin/planning/filieres - Liste des filieres");
        return ResponseEntity.ok(planningUseCase.getFilieres());
    }

    @PutMapping("/filieres")
    @Operation(summary = "Mettre a jour les filieres", description = "Met a jour la liste complete des filieres.")
    public ResponseEntity<Map<String, Object>> updateFilieres(@RequestBody List<Map<String, Object>> corps) {
        List<Filiere> filieres = PlanningPayloads.filieres(corps);
        logger.info("PUT /admin/planning/filieres - Mise a jour des filieres: {} filieres", filieres.size());
        return ResponseEntity.ok(bilanCorbeille(ecole_221.sn.ecole.modules.corbeille.application.TypeCorbeille.FILIERE, planningUseCase.updateFilieres(filieres)));
    }

    @GetMapping("/classes")
    @Operation(summary = "Liste des classes", description = "Retourne toutes les classes de l'ecole.")
    public ResponseEntity<List<Classe>> getClasses() {
        logger.info("GET /admin/planning/classes - Liste des classes");
        return ResponseEntity.ok(planningUseCase.getClasses());
    }

    @PutMapping("/classes")
    @Operation(summary = "Mettre a jour les classes", description = "Met a jour la liste complete des classes.")
    public ResponseEntity<Map<String, Object>> updateClasses(@RequestBody List<Map<String, Object>> corps) {
        List<Classe> classes = PlanningPayloads.classes(corps);
        logger.info("PUT /admin/planning/classes - Mise a jour des classes: {} classes", classes.size());
        return ResponseEntity.ok(bilanCorbeille(ecole_221.sn.ecole.modules.corbeille.application.TypeCorbeille.CLASSE, planningUseCase.updateClasses(classes)));
    }

    @GetMapping("/slots")
    @Operation(summary = "Creneaux horaires", description = "Retourne tous les crenaux horaires disponibles.")
    public ResponseEntity<List<TimeSlot>> getSlots() {
        logger.info("GET /admin/planning/slots - Liste des crenaux horaires");
        return ResponseEntity.ok(planningUseCase.getSlots());
    }

    @PutMapping("/slots")
    @Operation(summary = "Mettre a jour les crenaux", description = "Met a jour la liste complete des crenaux horaires, "
            + "puis les séances des professeurs (une par créneau rattaché à une classe). Réponse : nombre de séances, "
            + "séances retirées, créneaux sans cours de même intitulé dans la classe (appel impossible) ou sans professeur.")
    @org.springframework.transaction.annotation.Transactional   // créneaux et séances enregistrés ensemble ou pas du tout
    public ResponseEntity<Map<String, Object>> updateSlots(@RequestBody List<Map<String, Object>> corps) {
        List<TimeSlot> slots = PlanningPayloads.creneaux(corps);
        logger.info("PUT /admin/planning/slots - Mise a jour des crenaux: {} crenaux", slots.size());
        // Doubles réservations refusées par le SERVEUR
        Map<String, String> nomsClasses = new java.util.HashMap<>();
        for (Classe c : planningUseCase.getClasses()) {
            if (c.getId() != null && c.getName() != null) nomsClasses.put(c.getId(), c.getName());
        }
        List<String> conflits = ConflitsPlanning.detecter(slots, nomsClasses);
        if (!conflits.isEmpty()) {
            String detail = String.join(" ; ", conflits.subList(0, Math.min(5, conflits.size())))
                    + (conflits.size() > 5 ? " ; et " + (conflits.size() - 5) + " autre(s)." : ".");
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                    "Emploi du temps non enregistré : " + conflits.size() + " conflit" + (conflits.size() > 1 ? "s" : "") + ". " + detail);
        }
        var retires = planningUseCase.updateSlots(slots);
        Map<String, Object> rep = new java.util.LinkedHashMap<>();
        rep.put("creneaux", slots.size());
        if (seancesEmploiDuTemps != null) {
            var bilan = seancesEmploiDuTemps.synchroniser(slots);
            rep.put("seances", bilan.seances());
            rep.put("seancesRetirees", bilan.retirees());
            rep.put("sansCours", bilan.sansCours());
            rep.put("sansProfesseur", bilan.sansProfesseur());
        }
        rep.putAll(bilanCorbeille(ecole_221.sn.ecole.modules.corbeille.application.TypeCorbeille.CRENEAU, retires));
        return ResponseEntity.ok(rep);
    }

    /**
     * Les lignes absentes de la liste envoyée sont mises à la corbeille (restaurables 30 jours) ; la réponse le dit.
     * {@code {"corbeille": bool, "misALaCorbeille": [{type, id, libelle}], "message"?}} — « message » seulement
     * si au moins une ligne a été mise à la corbeille.
     */
    static Map<String, Object> bilanCorbeille(ecole_221.sn.ecole.modules.corbeille.application.TypeCorbeille type, List<Map<String, Object>> retirees) {
        List<Map<String, Object>> liste = retirees == null ? List.of() : retirees;
        Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("corbeille", !liste.isEmpty());
        r.put("misALaCorbeille", liste);
        if (!liste.isEmpty()) {
            r.put("message", type.messageMiseALaCorbeille(liste.stream().map(e -> String.valueOf(e.get("libelle"))).toList()));
        }
        return r;
    }
}