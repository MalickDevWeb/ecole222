package ecole_221.sn.ecole.modules.platform.billing.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Abonnement de l'école courante, vu par son administration (APP-FE-11).
 * Réponse : offre et quotas réels (table subscriptions / plans) et consommation réelle
 * (appartenances actives de l'école). Sans abonnement enregistré : disponible = false.
 * Sécurité : /admin/** réservé aux administrateurs (SecurityConfig), école = contexte de la requête.
 * Le personnel y accède avec la permission « ecole.lire » (préfixe /admin/abonnement, CataloguePermissions).
 */
@RestController
public class AbonnementEcoleController {

    private static final Map<String, String> LIBELLES = Map.of(
            "MAX_STUDENTS", "Élèves maximum",
            "MAX_TEACHERS", "Enseignants & personnel",
            "MAX_CLASSES", "Classes / filières");

    /**
     * Fonctionnalités d'une formule montrées à l'école (GET /admin/abonnement/formules). Les quotas portent une limite ;
     * les autres sont de simples inclusions. AI_TUTOR n'y figure pas : le tuteur IA est hors du périmètre de cette
     * version (APP-TL-24), une formule ne doit pas le promettre. Un code inconnu n'est jamais montré.
     */
    private static final Map<String, String> LIBELLES_FORMULE = Map.of(
            "MAX_STUDENTS", "Élèves maximum",
            "MAX_TEACHERS", "Enseignants & personnel",
            "MAX_CLASSES", "Classes / filières",
            "MAX_USERS", "Comptes utilisateurs",
            "STORAGE", "Stockage de documents",
            "LIVE_MEET", "Cours en direct",
            "ADVANCED_REPORTS", "Rapports avancés",
            "MULTI_CAMPUS", "Multi-campus",
            "CUSTOM_DOMAIN", "Domaine personnalisé");

    private static final java.util.Set<String> CODES_QUOTA = java.util.Set.of(
            "MAX_STUDENTS", "MAX_TEACHERS", "MAX_CLASSES", "MAX_USERS", "STORAGE");

    /** Fonctionnalité d'une formule : limite = valeur fixée par la plateforme (quota), null pour une simple inclusion. */
    public record FonctionnaliteFormule(String code, String label, boolean quota, Integer limit) {
    }

    /** Formule proposée par la plateforme, sans champ interne (ni état d'activation, ni dates, ni identifiants de quota). */
    public record FormuleProposee(String id, String name, String description, BigDecimal priceMonthly,
                                  BigDecimal priceYearly, List<FonctionnaliteFormule> fonctionnalites) {
    }

    private final SubscriptionRepositoryPort abonnements;
    private final PlanRepositoryPort offres;
    private final OrganizationUserRepositoryPort appartenances;

    public AbonnementEcoleController(SubscriptionRepositoryPort abonnements, PlanRepositoryPort offres,
                                     OrganizationUserRepositoryPort appartenances) {
        this.abonnements = abonnements;
        this.offres = offres;
        this.appartenances = appartenances;
    }

    @Operation(summary = "Abonnement de l'école", description = "Offre et quotas réels de l'école courante et consommation réelle (comptes actifs) ; disponible = false sans abonnement enregistré.")
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

    /**
     * Formules que la plateforme propose aux écoles : uniquement les offres ACTIVES créées par le super-admin, avec leur
     * prix réels et les fonctionnalités activées de leurs quotas. Liste vide si aucune n'est proposée (aucune formule
     * inventée). Rien n'est enregistré : le changement de formule se fait avec l'équipe de la plateforme.
     */
    @Operation(summary = "Formules proposées aux écoles", description = "Offres actives de la plateforme (nom, description, prix mensuel et annuel réels, quotas et fonctionnalités activés), triées par prix mensuel ; liste vide si aucune.")
    @GetMapping("/admin/abonnement/formules")
    public ResponseEntity<List<FormuleProposee>> formules() {
        List<FormuleProposee> formules = offres.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .sorted(Comparator.comparing(Plan::getPriceMonthly, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Plan::getName, String.CASE_INSENSITIVE_ORDER))
                .map(AbonnementEcoleController::formule)
                .toList();
        return ResponseEntity.ok(formules);
    }

    private static FormuleProposee formule(Plan p) {
        List<FonctionnaliteFormule> fonctionnalites = new ArrayList<>();
        for (Entitlement e : p.getEntitlements()) {
            String code = e.getFeatureCode();
            if (!Boolean.TRUE.equals(e.getIsEnabled()) || !LIBELLES_FORMULE.containsKey(code)) {
                continue;
            }
            boolean quota = CODES_QUOTA.contains(code);
            fonctionnalites.add(new FonctionnaliteFormule(code, LIBELLES_FORMULE.get(code), quota, quota ? e.getMaxLimit() : null));
        }
        // quotas d'abord, puis inclusions, chacun par libellé : ordre stable d'une formule à l'autre
        fonctionnalites.sort(Comparator.comparing((FonctionnaliteFormule f) -> !f.quota()).thenComparing(FonctionnaliteFormule::label));
        return new FormuleProposee(p.getId(), p.getName(), p.getDescription() == null ? "" : p.getDescription(),
                p.getPriceMonthly(), p.getPriceYearly(), fonctionnalites);
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
