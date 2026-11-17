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

import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class PublicOnboardingController {

    private static final Logger log = LoggerFactory.getLogger(PublicOnboardingController.class);

    private final OrganizationApplicationService organizationService;

    public PublicOnboardingController(OrganizationApplicationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping("/register-school")
    public ResponseEntity<Organization> registerSchool(@RequestBody SchoolRegistrationRequest request) {
        log.info("[register-school] Inscription demandée pour: {}", request.getSchoolName());
        Organization org = organizationService.registerSchool(request);
        // US-02 [FEAT-001-SC-03] : création → 201 Created (relevé OBS-01, Agent 05)
        return ResponseEntity.status(201).body(org);
    }

    @GetMapping({"/public/check-availability", "/v1/public/check-availability"})
    public ResponseEntity<Map<String, Boolean>> checkAvailability(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String slug) {
        Map<String, Boolean> res = new HashMap<>();
        if (email != null && !email.isBlank()) {
            res.put("emailAvailable", organizationService.isEmailAvailable(email));
        }
        if (phone != null && !phone.isBlank()) {
            res.put("phoneAvailable", organizationService.isPhoneAvailable(phone));
        }
        if (slug != null && !slug.isBlank()) {
            res.put("slugAvailable", organizationService.isSlugAvailable(slug));
        }
        return ResponseEntity.ok(res);
    }

    @GetMapping({"/public/tenants/{slug}", "/v1/public/tenants/{slug}"})
    public ResponseEntity<?> getPublicTenant(@PathVariable String slug) {
        if (slug == null || slug.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // Défaut F7 : une école SUSPENDED, ARCHIVED, REJECTED (ou EXPIRED, CANCELLED) n'a plus de fiche
        // publique (404, même réponse qu'un identifiant inconnu) ; une école PENDING reste lisible avec statut « PENDING »
        // (écran « en attente de validation »).
        // Cas spécial Ecole 221
        if ("ecole221".equalsIgnoreCase(slug) || "ecole-221".equalsIgnoreCase(slug)) {
            java.util.Optional<Organization> ecole221 = organizationService.getOrganizationBySlug("ecole-221");
            if (ecole221.isPresent() && !ficheVisible(ecole221.get())) {
                return introuvable(slug);
            }
            return ecole221
                .map(org -> {
                    Map<String, Object> branding = new HashMap<>();
                    String primaryColor = "#B3181C";
                    if (org.getSettings() != null && org.getSettings().containsKey("primaryColor")) {
                        primaryColor = String.valueOf(org.getSettings().get("primaryColor"));
                    }
                    branding.put("primaryColor", primaryColor);
                    if (org.getLogo() != null && !org.getLogo().isBlank()) {
                        branding.put("logoUrl", org.getLogo());
                if (org.getSettings() != null && org.getSettings().get("backgroundUrl") != null) {
                    branding.put("backgroundUrl", String.valueOf(org.getSettings().get("backgroundUrl")));
                }
                    }
                    if (org.getSettings() != null && org.getSettings().get("backgroundUrl") != null) {
                        branding.put("backgroundUrl", String.valueOf(org.getSettings().get("backgroundUrl")));
                    }
                    return ResponseEntity.ok(publique(org.getId(), org.getName(), "ecole-221", branding,
                            org.getPhone(), org.getEmail(), org.getCity(), org.getStatus().value()));
                })
                .orElseGet(() -> ResponseEntity.ok(Map.of(
                    "id", "default-tenant-id",
                    "name", "École 221",
                    "slug", "ecole-221",
                    "branding", Map.of(
                        "primaryColor", "#B3181C"
                    )
                )));
        }

        return organizationService.getOrganizationBySlug(slug)
            .filter(PublicOnboardingController::ficheVisible)
            .map(org -> {
                Map<String, Object> branding = new HashMap<>();
                String primaryColor = "#7C3AED"; // fallback default
                if (org.getSettings() != null && org.getSettings().containsKey("primaryColor")) {
                    primaryColor = String.valueOf(org.getSettings().get("primaryColor"));
                }
                branding.put("primaryColor", primaryColor);
                branding.put("logoUrl", org.getLogo());

                return ResponseEntity.ok(publique(org.getId(), org.getName(), org.getSlug(), branding,
                        org.getPhone(), org.getEmail(), org.getCity(), org.getStatus().value()));
            })
            .orElseGet(() -> introuvable(slug));
    }

    /** Fiche publique servie pour une école active ou en attente de validation seulement (défaut F7). */
    static boolean ficheVisible(Organization org) {
        return org.isActive() || ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus.PENDING
                .equals(org.getStatus());
    }

    private static ResponseEntity<Map<String, Object>> introuvable(String slug) {
        return ResponseEntity.status(404).body(Map.<String, Object>of("error", "Établissement non trouvé pour le slug: " + slug));
    }

    /**
     * Fiche publique de l'école : identité, couleurs et COORDONNÉES RÉELLES (téléphone, e-mail, ville saisis à
     * l'inscription). Valeur absente = null (jamais de coordonnée inventée).
     */
    private static Map<String, Object> publique(String id, String nom, String slug, Map<String, Object> branding,
                                                String telephone, String email, String ville, String statut) {
        Map<String, Object> m = new HashMap<>();
        m.put("statut", statut);   // F7 : « ACTIVE » ou « PENDING »
        m.put("id", id);
        m.put("name", nom);
        m.put("slug", slug);
        m.put("branding", branding);
        m.put("phone", telephone);
        m.put("email", email);
        m.put("city", ville);
        return m;
    }
}
