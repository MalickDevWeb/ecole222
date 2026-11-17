package ecole_221.sn.ecole.modules.auth.infrastructure.web;

import ecole_221.sn.ecole.modules.auth.application.service.PolitiqueMotDePasse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Règles de mot de passe d'un rôle, pour l'indicateur de force du frontend (15/11, public, sans donnée de compte). */
@RestController
@Tag(name = "Auth", description = "Politique de mot de passe")
public class ReglesMotDePasseController {

    @GetMapping("/auth/regles-mot-de-passe")
    @Operation(summary = "Règles de mot de passe", description = "role = ETUDIANT, PARENT, PROFESSEUR, ADMIN, SUPER_ADMIN… → "
            + "{role, longueurMin, longueurMax, conseils}")
    public ResponseEntity<Map<String, Object>> regles(@RequestParam(value = "role", required = false) String role) {
        return ResponseEntity.ok(PolitiqueMotDePasse.regles(role));
    }
}
