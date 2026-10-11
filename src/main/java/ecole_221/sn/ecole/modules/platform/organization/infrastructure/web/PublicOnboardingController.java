package ecole_221.sn.ecole.modules.platform.organization.infrastructure.web;

import ecole_221.sn.ecole.modules.platform.organization.application.OrganizationApplicationService;
import ecole_221.sn.ecole.modules.platform.organization.application.dto.SchoolRegistrationRequest;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class PublicOnboardingController {

    private static final Logger log = LoggerFactory.getLogger(PublicOnboardingController.class);

    private final OrganizationApplicationService organizationService;

    public PublicOnboardingController(OrganizationApplicationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping("/register-school")
    public ResponseEntity<?> registerSchool(@RequestBody SchoolRegistrationRequest request) {
        try {
            Organization org = organizationService.registerSchool(request);
            return ResponseEntity.ok(org);
        } catch (IllegalArgumentException e) {
            log.warn("[register-school] Validation rejetée : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "message", e.getMessage()));
        } catch (Exception e) {
            log.error("[register-school] Erreur interne lors de l'inscription de l'école", e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Une erreur inattendue est survenue";
            return ResponseEntity.internalServerError().body(Map.of("error", errorMsg, "message", errorMsg));
        }
    }

    @GetMapping({"/public/tenants/{slug}", "/v1/public/tenants/{slug}"})
    public ResponseEntity<?> getPublicTenant(@PathVariable String slug) {
        if (slug == null || slug.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // Cas spécial Ecole 221
        if ("ecole221".equalsIgnoreCase(slug) || "ecole-221".equalsIgnoreCase(slug)) {
            return ResponseEntity.ok(Map.of(
                "id", "ecole-221-id",
                "name", "École 221",
                "slug", "ecole221",
                "branding", Map.of(
                    "primaryColor", "#B3181C",
                    "logoUrl", "/myschool-icon.png"
                )
            ));
        }

        return organizationService.getOrganizationBySlug(slug)
            .map(org -> {
                Map<String, Object> branding = new HashMap<>();
                String primaryColor = "#7C3AED"; // fallback default
                if (org.getSettings() != null && org.getSettings().containsKey("primaryColor")) {
                    primaryColor = String.valueOf(org.getSettings().get("primaryColor"));
                }
                branding.put("primaryColor", primaryColor);
                branding.put("logoUrl", org.getLogo());

                return ResponseEntity.ok(Map.of(
                    "id", org.getId(),
                    "name", org.getName(),
                    "slug", org.getSlug(),
                    "branding", branding
                ));
            })
            .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Établissement non trouvé pour le slug: " + slug)));
    }
}


