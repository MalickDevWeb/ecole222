package ecole_221.sn.ecole.modules.platform.organization.infrastructure.web;

import ecole_221.sn.ecole.modules.platform.organization.application.StudentEnrollmentService;
import ecole_221.sn.ecole.modules.platform.organization.application.StudentTransferApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/platform/students")
@CrossOrigin(origins = "*")
public class StudentTransferController {

    private static final Logger logger = LoggerFactory.getLogger(StudentTransferController.class);

    private final StudentEnrollmentService enrollmentService;
    private final StudentTransferApplicationService transferService;

    public StudentTransferController(StudentEnrollmentService enrollmentService, StudentTransferApplicationService transferService) {
        this.enrollmentService = enrollmentService;
        this.transferService = transferService;
    }

    /**
     * Tente d'inscrire un élève dans une école.
     * Échoue si l'élève est déjà actif dans une autre école (Règle Mono-École).
     */
    @PostMapping("/{studentId}/enroll")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'PLATFORM_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> enrollStudent(
            @PathVariable("studentId") String studentId,
            @RequestBody Map<String, String> body) {
        
        String organizationId = body.get("organizationId");
        if (organizationId == null || organizationId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "organizationId is required"));
        }

        logger.info("Tentative d'inscription de l'élève {} dans l'école {}", studentId, organizationId);
        
        try {
            enrollmentService.enrollStudent(studentId, organizationId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Élève inscrit avec succès dans l'école."
            ));
        } catch (IllegalStateException e) {
            logger.warn("Échec de l'inscription : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "requiresTransfer", true
            ));
        }
    }

    /**
     * Déclenche la procédure complète de transfert d'un élève.
     */
    @PostMapping("/{studentId}/transfer")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'PLATFORM_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> transferStudent(
            @PathVariable("studentId") String studentId,
            @RequestBody Map<String, String> body) {
        
        String fromOrganizationId = body.get("fromOrganizationId");
        String toOrganizationId = body.get("toOrganizationId");
        String reason = body.getOrDefault("reason", "Transfert administratif");

        if (fromOrganizationId == null || toOrganizationId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "fromOrganizationId and toOrganizationId are required"));
        }

        logger.info("Procédure de transfert de l'élève {} de l'école {} vers {}", studentId, fromOrganizationId, toOrganizationId);

        try {
            transferService.transferStudent(studentId, fromOrganizationId, toOrganizationId, reason);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Le transfert de l'élève a été effectué avec succès. Un archivage de son dossier a été déclenché."
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Erreur lors du transfert : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
