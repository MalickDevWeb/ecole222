package ecole_221.sn.ecole.modules.admissions.infrastructure.web;

import ecole_221.sn.ecole.modules.admissions.application.service.ReglesAdmissionService;
import ecole_221.sn.ecole.modules.admissions.application.service.ReglesAdmissionService.Dossier;
import ecole_221.sn.ecole.modules.admissions.application.service.ReglesAdmissionService.Evaluation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Règles d'admission de l'école. Administration : droit « admissions » (préfixe /admissions).
 * Évaluation publique : le portail candidat et la page parent demandent quelles pièces la règle exige et si le dépôt
 * est bloqué, AVANT l'envoi. Les avertissements internes ne sont jamais renvoyés au public.
 */
@RestController
@Tag(name = "Admissions")
public class ReglesAdmissionController {

    private final ReglesAdmissionService regles;

    public ReglesAdmissionController(ReglesAdmissionService regles) {
        this.regles = regles;
    }

    @GetMapping({"/admissions/regles", "/v1/admissions/regles"})
    @Operation(summary = "Règles d'admission de l'école")
    public List<Map<String, Object>> lister() {
        return regles.lister();
    }

    @PostMapping({"/admissions/regles", "/v1/admissions/regles"})
    @Operation(summary = "Créer une règle d'admission", description = "SI condition(s) ALORS effet ; 400 avec la raison si la règle est incomplète")
    public ResponseEntity<Map<String, Object>> creer(@RequestBody Map<String, Object> corps) {
        return ResponseEntity.status(HttpStatus.CREATED).body(regles.creer(corps));
    }

    @PutMapping({"/admissions/regles/{id}", "/v1/admissions/regles/{id}"})
    @Operation(summary = "Modifier une règle d'admission")
    public Map<String, Object> modifier(@PathVariable String id, @RequestBody Map<String, Object> corps) {
        return regles.modifier(id, corps);
    }

    @DeleteMapping({"/admissions/regles/{id}", "/v1/admissions/regles/{id}"})
    @Operation(summary = "Supprimer une règle d'admission")
    public ResponseEntity<Void> supprimer(@PathVariable String id) {
        regles.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/candidatures/regles/evaluer")
    @Operation(summary = "Pièces exigées et blocage pour un dossier (public)",
            description = "Corps : campagneId, nationalite (code ISO), sexe, dateNaissance, serieBac, mentionBac, anneeBac, depotParent")
    public Map<String, Object> evaluer(@RequestBody(required = false) Map<String, Object> corps) {
        Map<String, Object> c = corps == null ? Map.of() : corps;
        Evaluation ev = regles.evaluer(Dossier.depuis(c, Boolean.TRUE.equals(c.get("depotParent"))));
        Map<String, Object> rep = new LinkedHashMap<>();
        rep.put("piecesExigees", ev.piecesExigees().stream()
                .map(p -> Map.of("code", p.code(), "libelle", p.libelle(), "formats", p.formats())).toList());
        rep.put("blocages", ev.blocages());
        return rep;
    }
}
