package ecole_221.sn.ecole.modules.auth.infrastructure.web;

import ecole_221.sn.ecole.modules.auth.application.service.ReinitialisationMotDePasseService;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    private final OrganizationRepositoryPort ecoles;

    public ReinitialisationMotDePasseController(ReinitialisationMotDePasseService service, OrganizationRepositoryPort ecoles) {
        this.service = service;
        this.ecoles = ecoles;
    }

    @PostMapping("/auth/mot-de-passe-oublie")
    @Operation(summary = "Demander un lien de réinitialisation",
            description = "Public. Corps {email}. Réponse identique que le compte existe ou non ; 429 après 3 demandes en 15 minutes.")
    public ResponseEntity<Map<String, Object>> demander(@RequestBody(required = false) Map<String, String> corps,
                                                        @RequestHeader(value = "X-Tenant-Slug", required = false) String slugDemande) {
        // Les routes /auth/** ne passent pas par la résolution d'école : l'école du lien est lue dans l'en-tête et
        // n'est retenue que si elle EXISTE (jamais de valeur arbitraire recopiée dans le lien envoyé par e-mail).
        String slug = TenantContext.getCurrentOrganization().map(Organization::getSlug)
                .or(() -> slugDemande == null || slugDemande.isBlank() ? java.util.Optional.empty()
                        : ecoles.findBySlug(slugDemande.trim()).map(Organization::getSlug))
                .orElse(null);
        service.demander(corps == null ? null : corps.get("email"), slug);
        return ResponseEntity.accepted().body(Map.of("success", true, "message", MESSAGE_DEMANDE));
    }

    @PostMapping("/auth/reinitialiser-mot-de-passe")
    @Operation(summary = "Choisir un nouveau mot de passe",
            description = "Public. Corps {jeton, motDePasse}. Jeton à usage unique, valable 30 minutes.")
    public ResponseEntity<Map<String, Object>> reinitialiser(@RequestBody(required = false) Map<String, String> corps) {
        service.reinitialiser(corps == null ? null : corps.get("jeton"), corps == null ? null : corps.get("motDePasse"));
        return ResponseEntity.ok(Map.of("success", true,
                "message", "Mot de passe modifié : vous pouvez vous connecter avec votre nouveau mot de passe."));
    }
}
