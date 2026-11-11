package ecole_221.sn.ecole.modules.student.infrastructure.web;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.professor.application.service.RendusDevoirsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/** Lot B — l'élève relit les devoirs de sa classe, son rendu, sa note et le commentaire du professeur. */
@RestController
@Tag(name = "Student — devoirs de classe", description = "Devoirs publiés par les professeurs pour la classe de l'élève")
public class DevoirsClasseEleveController {

    private final RendusDevoirsService service;

    public DevoirsClasseEleveController(RendusDevoirsService service) {
        this.service = service;
    }

    @GetMapping("/student/devoirs-classe")
    @Operation(summary = "Mes devoirs de classe", description = "Devoirs des cours de ma classe : date limite, mon rendu (fichier, retard), ma note et le commentaire du professeur.")
    public ResponseEntity<List<Map<String, Object>>> mesDevoirs() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User u)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connexion requise.");
        }
        return ResponseEntity.ok(service.devoirsEleve(u.getId()));
    }
}
