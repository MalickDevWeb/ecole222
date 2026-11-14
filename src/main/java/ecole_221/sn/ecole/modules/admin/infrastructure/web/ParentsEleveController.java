package ecole_221.sn.ecole.modules.admin.infrastructure.web;

import ecole_221.sn.ecole.modules.admin.application.service.ParentsEleveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Parents d'un élève (APP-BE-27, 2026-11-14) : l'administration crée ou rattache le compte du parent et le lie à
 * l'élève. Droits : module « Élèves » de la matrice de l'école (lecture pour la liste, écriture pour lier / délier).
 */
@RestController
@RequestMapping("/admin/students/{id}/parents")
@Tag(name = "Admin — parents d'un élève", description = "Comptes parents liés à un élève de l'école")
public class ParentsEleveController {

    private final ParentsEleveService service;

    public ParentsEleveController(ParentsEleveService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Parents liés à l'élève", description = "Nom, e-mail, téléphone, lien de parenté. 404 si l'élève n'est pas de l'école.")
    public ResponseEntity<List<Map<String, Object>>> liste(@PathVariable("id") String eleveId) {
        return ResponseEntity.ok(service.parents(eleveId));
    }

    @PostMapping
    @Operation(summary = "Lier un parent à l'élève",
            description = "Corps {email, nom (si le compte n'existe pas), telephone?, relation : PERE, MERE, TUTEUR ou AUTRE}. "
                    + "Compte créé avec un mot de passe provisoire renvoyé une seule fois, ou compte parent existant rattaché. "
                    + "409 : adresse d'un compte non parent, parent déjà lié, ou 4 parents déjà liés.")
    public ResponseEntity<Map<String, Object>> lier(@PathVariable("id") String eleveId,
                                                    @RequestBody(required = false) Map<String, Object> corps) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.lier(eleveId, corps));
    }

    @DeleteMapping("/{parentId}")
    @Operation(summary = "Délier un parent", description = "Retire le lien (le compte du parent n'est pas supprimé). 404 si le lien n'existe pas.")
    public ResponseEntity<Void> delier(@PathVariable("id") String eleveId, @PathVariable("parentId") String parentId) {
        service.delier(eleveId, parentId);
        return ResponseEntity.noContent().build();
    }
}
