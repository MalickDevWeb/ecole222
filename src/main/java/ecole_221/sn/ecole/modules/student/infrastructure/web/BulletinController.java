package ecole_221.sn.ecole.modules.student.infrastructure.web;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.ParentEnfantJpaRepository;
import ecole_221.sn.ecole.modules.student.application.service.BulletinEleveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** Bulletin (évaluations multiples) : l'élève pour lui-même, le parent pour SES enfants uniquement. */
@RestController
@Tag(name = "Bulletin", description = "Moyennes par matière et moyenne générale calculées à partir des évaluations")
public class BulletinController {

    private final BulletinEleveService service;
    private final ParentEnfantJpaRepository filiations;

    public BulletinController(BulletinEleveService service, ParentEnfantJpaRepository filiations) {
        this.service = service;
        this.filiations = filiations;
    }

    @GetMapping("/student/bulletin")
    @Operation(summary = "Mon bulletin", description = "Par matière : moyenne, mention, rang, détail des notes ; moyenne générale. « provisoire » tant que l'école n'a pas verrouillé les évaluations.")
    public ResponseEntity<Map<String, Object>> monBulletin() {
        return ResponseEntity.ok(service.bulletin(connecte().getId()));
    }

    @GetMapping("/parent/enfants/{enfantId}/bulletin")
    @Operation(summary = "Bulletin d'un enfant", description = "Réservé au parent rattaché à cet enfant.")
    public ResponseEntity<Map<String, Object>> bulletinEnfant(@PathVariable String enfantId) {
        String parentId = connecte().getId();
        boolean sonEnfant = filiations.findByParentId(parentId).stream().anyMatch(l -> enfantId.equals(l.getEnfantId()));
        if (!sonEnfant) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cet élève n'est pas l'un de vos enfants.");
        return ResponseEntity.ok(service.bulletin(enfantId));
    }

    private static User connecte() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User u)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connexion requise.");
        }
        return u;
    }
}
