package ecole_221.sn.ecole.modules.auth.infrastructure.web;

import ecole_221.sn.ecole.modules.auth.application.service.ReinitialisationMotDePasseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Auth", description = "Authentification — mot de passe oublié")
public class ReinitialisationMotDePasseController {

    /** Même réponse que le compte existe ou non (aucune fuite de l'existence des comptes). */
    static final String MESSAGE_DEMANDE = "Si un compte correspond à cette adresse, un e-mail contenant un lien de "
            + "réinitialisation vient d'être envoyé. Le lien est valable "
            + ReinitialisationMotDePasseService.VALIDITE_MINUTES + " minutes.";

    private final ReinitialisationMotDePasseService service;

    public ReinitialisationMotDePasseController(ReinitialisationMotDePasseService service) {
        this.service = service;
    }

    @PostMapping("/auth/mot-de-passe-oublie")
    @Operation(summary = "Demander un lien de réinitialisation",
            description = "Public. Corps {email}. Réponse identique que le compte existe ou non ; 429 après 3 demandes en 15 minutes.")
    public ResponseEntity<Map<String, Object>> demander(@RequestBody(required = false) Map<String, String> corps) {
        // Rien de la requête n'entre dans le lien envoyé par e-mail (ni Host, ni X-Forwarded-Host, ni Origin, ni
        // Referer, ni X-Tenant-Slug). Base = adresse configurée du site ; école = école d'appartenance du compte
        // (calculée par le service).
        service.demander(corps == null ? null : corps.get("email"));
        return ResponseEntity.accepted().body(Map.of("success", true, "message", MESSAGE_DEMANDE));
    }

    @PostMapping("/auth/reinitialiser-mot-de-passe")
    @Operation(summary = "Choisir un nouveau mot de passe",
            description = "Public. Corps {jeton, motDePasse, code?}. Jeton à usage unique (30 min en libre-service, 2 h émis "
                    + "par l'école, 72 h pour une activation) ; code : 6 chiffres dits de vive voix (comptes à privilèges).")
    public ResponseEntity<Map<String, Object>> reinitialiser(@RequestBody(required = false) Map<String, String> corps) {
        service.reinitialiser(corps == null ? null : corps.get("jeton"), corps == null ? null : corps.get("motDePasse"),
                corps == null ? null : corps.get("code"));
        return ResponseEntity.ok(Map.of("success", true,
                "message", "Mot de passe modifié : vous pouvez vous connecter avec votre nouveau mot de passe."));
    }
}
