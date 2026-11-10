package ecole_221.sn.ecole.modules.auth.infrastructure.web;

import ecole_221.sn.ecole.modules.auth.application.service.ChangementMotDePasseService;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Auth", description = "Authentification — changement du mot de passe")
public class ChangementMotDePasseController {

    private final ChangementMotDePasseService service;

    public ChangementMotDePasseController(ChangementMotDePasseService service) {
        this.service = service;
    }

    @PostMapping("/auth/changer-mot-de-passe")
    @Operation(summary = "Changer son mot de passe",
            description = "Corps {ancien, nouveau}. Nouveau : ≥ 10 caractères, une lettre et un chiffre, différent de l'ancien. "
                    + "Efface l'obligation de changer un mot de passe provisoire.")
    public ResponseEntity<Map<String, Object>> changer(@RequestBody(required = false) Map<String, String> corps) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth != null && auth.getPrincipal() instanceof User u ? u.getId() : null;
        service.changer(userId, corps == null ? null : corps.get("ancien"), corps == null ? null : corps.get("nouveau"));
        return ResponseEntity.ok(Map.of(
                "success", true,
                "doitChangerMotDePasse", false,
                "message", "Mot de passe modifié."));
    }
}
