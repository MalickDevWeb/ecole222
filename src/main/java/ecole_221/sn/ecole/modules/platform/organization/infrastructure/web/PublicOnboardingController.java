package ecole_221.sn.ecole.modules.platform.organization.infrastructure.web;

import ecole_221.sn.ecole.modules.platform.organization.application.OrganizationApplicationService;
import ecole_221.sn.ecole.modules.platform.organization.application.dto.SchoolRegistrationRequest;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/register-school")
public class PublicOnboardingController {

    private static final Logger log = LoggerFactory.getLogger(PublicOnboardingController.class);

    private final OrganizationApplicationService organizationService;

    public PublicOnboardingController(OrganizationApplicationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    public ResponseEntity<?> registerSchool(@RequestBody SchoolRegistrationRequest request) {
        try {
            Organization org = organizationService.registerSchool(request);
            return ResponseEntity.ok(org);
        } catch (Exception e) {
            log.error("[register-school] Erreur lors de l'inscription de l'école", e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.toString();
            return ResponseEntity.internalServerError()
                    .body(java.util.Map.of("error", errorMsg));
        }
    }
}


