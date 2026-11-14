package ecole_221.sn.ecole.modules.admin.periodes;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Trimestres de l'école et clôture des notes (BR-04, 2026-11-14). Accès : administration et personnel autorisé. */
@RestController
@RequestMapping("/admin/periodes")
public class PeriodesScolairesController {

    private final PeriodesScolairesService periodes;

    public PeriodesScolairesController(PeriodesScolairesService periodes) {
        this.periodes = periodes;
    }

    @Operation(summary = "Périodes scolaires", description = "Trimestres de l'école, dans l'ordre chronologique, avec leur état (clôturée ou non).")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> liste() {
        return ResponseEntity.ok(periodes.liste());
    }

    @Operation(summary = "Créer une période", description = "Corps {libelle, dateDebut, dateFin} (AAAA-MM-JJ) ; sans chevauchement avec une autre période.")
    @PostMapping
    public ResponseEntity<Map<String, Object>> creer(@RequestBody(required = false) Map<String, Object> corps) {
        return ResponseEntity.status(HttpStatus.CREATED).body(periodes.creer(corps));
    }

    @Operation(summary = "Modifier une période", description = "Impossible une fois la période clôturée.")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> modifier(@PathVariable("id") String id, @RequestBody(required = false) Map<String, Object> corps) {
        return ResponseEntity.ok(periodes.modifier(id, corps));
    }

    @Operation(summary = "Supprimer une période", description = "Impossible une fois la période clôturée.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> supprimer(@PathVariable("id") String id) {
        periodes.supprimer(id);
        return ResponseEntity.ok(Map.of("success", true, "id", id));
    }

    @Operation(summary = "Clôturer une période (BR-04)", description = "Les évaluations datées dans la période et leurs notes ne peuvent plus être créées, modifiées ni supprimées.")
    @PostMapping("/{id}/cloturer")
    public ResponseEntity<Map<String, Object>> cloturer(@PathVariable("id") String id) {
        return ResponseEntity.ok(periodes.cloturer(id));
    }

    @Operation(summary = "Rouvrir une période clôturée", description = "Exceptionnel : administration seulement, tracé au journal d'audit.")
    @PostMapping("/{id}/rouvrir")
    public ResponseEntity<Map<String, Object>> rouvrir(@PathVariable("id") String id) {
        return ResponseEntity.ok(periodes.rouvrir(id));
    }
}
