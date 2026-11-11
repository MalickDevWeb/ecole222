package ecole_221.sn.ecole.modules.platform.organization.infrastructure.web;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.application.StudentEnrollmentService;
import ecole_221.sn.ecole.modules.platform.organization.application.StudentTransferApplicationService;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

/**
 * Inscription et transfert d'un élève entre écoles.
 *
 * <p>Révision 2026-11-10 (test des routes super-admin) :</p>
 * <ul>
 *   <li>l'élève et les écoles doivent EXISTER (avant : inscription d'un élève inexistant dans une école
 *       inexistante → lien fantôme, puis « transfert effectué avec succès ») ;</li>
 *   <li>un administrateur d'école n'agit que sur SA propre école (avant : il pouvait inscrire dans, ou retirer
 *       un élève de, n'importe quelle autre école) ; le super-admin agit sur toutes ;</li>
 *   <li>erreurs renvoyées avec un champ « message » lisible ; plus d'annonce d'un archivage qui n'existe pas.</li>
 * </ul>
 */
@RestController
@RequestMapping("/platform/students")
public class StudentTransferController {

    private static final Logger logger = LoggerFactory.getLogger(StudentTransferController.class);
    private static final Set<String> ROLES_PLATEFORME = Set.of("ROLE_SUPER_ADMIN", "ROLE_PLATFORM_ADMIN");

    private final StudentEnrollmentService enrollmentService;
    private final StudentTransferApplicationService transferService;
    private final UserRepositoryPort comptes;
    private final OrganizationRepositoryPort etablissements;

    public StudentTransferController(StudentEnrollmentService enrollmentService,
                                     StudentTransferApplicationService transferService,
                                     UserRepositoryPort comptes,
                                     OrganizationRepositoryPort etablissements) {
        this.enrollmentService = enrollmentService;
        this.transferService = transferService;
        this.comptes = comptes;
        this.etablissements = etablissements;
    }

    /** Inscrit un élève dans une école (refusé s'il est déjà actif ailleurs : passer par le transfert). */
    @PostMapping("/{studentId}/enroll")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> enrollStudent(
            @PathVariable("studentId") String studentId,
            @RequestBody(required = false) Map<String, String> body) {
        String organizationId = texte(body, "organizationId");
        if (organizationId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'école d'inscription (organizationId) est obligatoire.");
        }
        eleve(studentId);
        ecole(organizationId);
        verifierPerimetre(organizationId, "inscrire un élève dans une autre école que la vôtre");

        logger.info("Inscription de l'élève {} dans l'école {}", studentId, organizationId);
        try {
            enrollmentService.enrollStudent(studentId, organizationId);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Élève inscrit dans l'école."));
    }

    /** Transfère un élève de l'école source vers l'école de destination. */
    @PostMapping("/{studentId}/transfer")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> transferStudent(
            @PathVariable("studentId") String studentId,
            @RequestBody(required = false) Map<String, String> body) {
        String depuis = texte(body, "fromOrganizationId");
        String vers = texte(body, "toOrganizationId");
        String motif = texte(body, "reason");
        if (depuis == null || vers == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'école source et l'école de destination sont obligatoires.");
        }
        if (depuis.equals(vers)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'école de destination doit être différente de l'école source.");
        }
        eleve(studentId);
        ecole(depuis);
        ecole(vers);
        verifierPerimetre(depuis, "transférer un élève qui n'appartient pas à votre école");

        logger.info("Transfert de l'élève {} de {} vers {}", studentId, depuis, vers);
        try {
            transferService.transferStudent(studentId, depuis, vers, motif == null ? "Transfert administratif" : motif);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        return ResponseEntity.ok(Map.of("success", true,
                "message", "Transfert effectué : l'élève est désormais inscrit dans l'école de destination."));
    }

    // ---------------------------------------------------------------- contrôles

    private void eleve(String studentId) {
        User u = comptes.findById(studentId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Élève introuvable."));
        String role = u.getRole() == null ? "" : u.getRole().value();
        if (!"ETUDIANT".equalsIgnoreCase(role) && !"STUDENT".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce compte n'est pas un compte élève.");
        }
    }

    private void ecole(String organizationId) {
        etablissements.findById(organizationId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "École introuvable : " + organizationId));
    }

    /** Un administrateur d'école ne peut agir que sur l'école de sa session. */
    private void verifierPerimetre(String organizationId, String action) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean plateforme = auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).anyMatch(ROLES_PLATEFORME::contains);
        if (plateforme) return;
        String ecoleSession = TenantContext.getCurrentOrganizationId().orElse(null);
        if (ecoleSession == null || !ecoleSession.equals(organizationId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous ne pouvez pas " + action + ".");
        }
    }

    private static String texte(Map<String, String> body, String cle) {
        if (body == null) return null;
        String v = body.get(cle);
        return v == null || v.isBlank() ? null : v.trim();
    }
}
