package ecole_221.sn.ecole.modules.admin.infrastructure.web;

import ecole_221.sn.ecole.modules.admin.application.service.MotDePasseEleveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Mot de passe provisoire remis à un élève par l'école (APP-TL-23, 2026-11-14) : indispensable pour un élève inscrit
 * sans e-mail, qui ne peut pas recevoir de lien « Mot de passe oublié ». Droits : module « Élèves », écriture.
 */
@RestController
@RequestMapping("/admin/students/{id}/mot-de-passe-provisoire")
@Tag(name = "Admin — mot de passe d'un élève", description = "Mot de passe provisoire remis à un élève de l'école")
public class MotDePasseEleveController {

    private final MotDePasseEleveService service;

    public MotDePasseEleveController(MotDePasseEleveService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Remettre un mot de passe provisoire à l'élève",
            description = "Génère un mot de passe provisoire, renvoyé une seule fois (jamais envoyé ni journalisé) ; "
                    + "l'élève doit le changer à sa prochaine connexion et ses sessions ouvertes sont fermées. Réponse : "
                    + "{eleveId, nom, identifiant (matricule si l'élève n'a pas d'e-mail), sansEmail, motDePasseProvisoire}. "
                    + "404 : élève d'une autre école ; 409 : élève transféré, ou compte qui sert aussi dans un autre établissement.")
    public ResponseEntity<Map<String, Object>> nouveau(@PathVariable("id") String eleveId) {
        return ResponseEntity.ok(service.nouveauMotDePasseProvisoire(eleveId));
    }
}
