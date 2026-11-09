package ecole_221.sn.ecole.modules.platform.billing.infrastructure.web;

import ecole_221.sn.ecole.modules.platform.billing.domain.model.Entitlement;
import ecole_221.sn.ecole.modules.platform.billing.domain.model.Plan;
import ecole_221.sn.ecole.modules.platform.billing.domain.model.Subscription;
import ecole_221.sn.ecole.modules.platform.billing.domain.port.repository.PlanRepositoryPort;
import ecole_221.sn.ecole.modules.platform.billing.domain.port.repository.SubscriptionRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Abonnement de l'école courante, vu par son administration (APP-FE-11).
 * Avant : l'écran « Mon abonnement » affichait une offre inventée (« Avancé », 420/500 élèves…).
 * Réponse : offre et quotas réels (table subscriptions / plans) et consommation réelle
 * (appartenances actives de l'école). Sans abonnement enregistré : disponible = false.
 * Sécurité : /admin/** réservé aux administrateurs (SecurityConfig), école = contexte de la requête.
 */
@RestController
public class AbonnementEcoleController {

    private static final Map<String, String> LIBELLES = Map.of(
            "MAX_STUDENTS", "Élèves maximum",
            "MAX_TEACHERS", "Enseignants & personnel",
            "MAX_CLASSES", "Classes / filières");

    private final SubscriptionRepositoryPort abonnements;
    private final PlanRepositoryPort offres;
    private final OrganizationUserRepositoryPort appartenances;

    public AbonnementEcoleController(SubscriptionRepositoryPort abonnements, PlanRepositoryPort offres,
                                     OrganizationUserRepositoryPort appartenances) {
        this.abonnements = abonnements;
        this.offres = offres;
        this.appartenances = appartenances;
    }

    @GetMapping("/admin/abonnement")
    public ResponseEntity<Map<String, Object>> abonnement() {
        Map<String, Object> reponse = new LinkedHashMap<>();
        String ecole = TenantContext.getCurrentOrganizationId().orElse(null);
        Optional<Subscription> abonnement = ecole == null ? Optional.empty() : abonnements.findByOrganizationId(ecole);
        Optional<Plan> offre = abonnement.flatMap(a -> offres.findById(a.getPlanId()));
        if (abonnement.isEmpty() || offre.isEmpty()) {
            reponse.put("disponible", false);
            return ResponseEntity.ok(reponse);
        }
        Subscription a = abonnement.get();
        Plan p = offre.get();
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("id", p.getId());
        plan.put("name", p.getName());
        plan.put("tagline", p.getDescription() == null ? "" : p.getDescription());
        plan.put("priceMonthly", p.getPriceMonthly());
        plan.put("priceYearly", p.getPriceYearly());
        plan.put("status", a.getStatus());
        plan.put("billingCycle", "monthly");
        plan.put("endDate", a.getEndDate() == null ? null : a.getEndDate().toLocalDate().toString());
        List<Map<String, Object>> quotas = new ArrayList<>();
        for (Entitlement e : p.getEntitlements() == null ? List.<Entitlement>of() : p.getEntitlements()) {
            if (!Boolean.TRUE.equals(e.getIsEnabled()) || !LIBELLES.containsKey(e.getFeatureCode())) {
                continue;
            }
            Map<String, Object> q = new LinkedHashMap<>();
            q.put("code", e.getFeatureCode());
            q.put("label", LIBELLES.get(e.getFeatureCode()));
            q.put("limit", e.getMaxLimit());
            q.put("used", consommation(ecole, e.getFeatureCode()));
            quotas.add(q);
        }
        plan.put("features", quotas);
        reponse.put("disponible", true);
        reponse.put("plan", plan);
        return ResponseEntity.ok(reponse);
    }

    /** Consommation réelle : appartenances ACTIVES de l'école ; null si aucune source fiable (classes). */
    private Integer consommation(String ecole, String code) {
        return switch (code) {
            case "MAX_STUDENTS" -> actifs(ecole, "ETUDIANT");
            case "MAX_TEACHERS" -> actifs(ecole, "PROFESSEUR");
            default -> null;
        };
    }

    private int actifs(String ecole, String role) {
        return (int) appartenances.findByOrganizationIdAndRole(ecole, role).stream().filter(m -> m.isActive()).count();
    }
}
