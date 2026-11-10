package ecole_221.sn.ecole.modules.platform.billing.application.service;

import ecole_221.sn.ecole.modules.platform.billing.application.port.command.AddEntitlementCommand;
import ecole_221.sn.ecole.modules.platform.billing.application.port.command.CreatePlanCommand;
import ecole_221.sn.ecole.modules.platform.billing.application.port.dto.PlanDto;
import ecole_221.sn.ecole.modules.platform.billing.domain.model.Entitlement;
import ecole_221.sn.ecole.modules.platform.billing.domain.model.Plan;
import ecole_221.sn.ecole.modules.platform.billing.domain.port.repository.PlanRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlanService {
    
    /** Codes de quota connus de l'application (écran « Mon abonnement » et formulaire des offres). */
    static final java.util.Set<String> CODES_QUOTA = java.util.Set.of("MAX_USERS", "MAX_STUDENTS", "MAX_TEACHERS", "MAX_CLASSES",
            "AI_TUTOR", "LIVE_MEET", "ADVANCED_REPORTS", "MULTI_CAMPUS", "CUSTOM_DOMAIN", "STORAGE");

    private final PlanRepositoryPort planRepository;
    private final ecole_221.sn.ecole.modules.platform.billing.domain.port.repository.SubscriptionRepositoryPort abonnements;

    public PlanService(PlanRepositoryPort planRepository,
                       ecole_221.sn.ecole.modules.platform.billing.domain.port.repository.SubscriptionRepositoryPort abonnements) {
        this.planRepository = planRepository;
        this.abonnements = abonnements;
    }

    private static org.springframework.web.server.ResponseStatusException erreur(org.springframework.http.HttpStatus s, String m) {
        return new org.springframework.web.server.ResponseStatusException(s, m);
    }

    private Plan plan(String planId) {
        return planRepository.findById(planId).orElseThrow(() -> erreur(org.springframework.http.HttpStatus.NOT_FOUND, "Offre introuvable."));
    }

    @Transactional
    public PlanDto createPlan(CreatePlanCommand cmd) {
        // erreurs de saisie trouvées au test du 2026-11-10 : prix négatifs et doublons acceptés
        if (cmd.priceMonthly().signum() < 0 || cmd.priceYearly().signum() < 0) {
            throw erreur(org.springframework.http.HttpStatus.BAD_REQUEST, "Les prix d'une offre ne peuvent pas être négatifs.");
        }
        String nom = cmd.name().trim();
        if (planRepository.findAll().stream().anyMatch(p -> p.getName() != null && p.getName().trim().equalsIgnoreCase(nom))) {
            throw erreur(org.springframework.http.HttpStatus.CONFLICT, "Une offre porte déjà le nom « " + nom + " ».");
        }
        Plan plan = Plan.create(nom, cmd.description(), cmd.priceMonthly(), cmd.priceYearly());
        Plan saved = planRepository.save(plan);
        if (cmd.entitlements() != null) {
            for (AddEntitlementCommand quota : cmd.entitlements()) {
                addEntitlement(saved.getId(), quota);   // mêmes contrôles (code connu, limite ≥ 0)
            }
            saved = plan(saved.getId());
        }
        return PlanDto.fromDomain(saved);
    }

    @Transactional
    public PlanDto addEntitlement(String planId, AddEntitlementCommand cmd) {
        Plan plan = plan(planId);
        String code = cmd.featureCode().trim().toUpperCase(java.util.Locale.ROOT);
        if (!CODES_QUOTA.contains(code)) {
            throw erreur(org.springframework.http.HttpStatus.BAD_REQUEST, "Code de quota inconnu : " + cmd.featureCode()
                    + ". Codes autorisés : " + String.join(", ", new java.util.TreeSet<>(CODES_QUOTA)) + ".");
        }
        if (cmd.maxLimit() != null && cmd.maxLimit() < 0) {
            throw erreur(org.springframework.http.HttpStatus.BAD_REQUEST, "La limite d'un quota ne peut pas être négative.");
        }
        // un code déjà présent est MIS À JOUR (même quota, nouvelle limite) — pas de doublon
        Entitlement entitlement = Entitlement.create(code, cmd.maxLimit(), cmd.isEnabled());
        plan.addEntitlement(entitlement);
        Plan saved = planRepository.save(plan);
        return PlanDto.fromDomain(saved);
    }

    @Transactional(readOnly = true)
    public List<PlanDto> listPlans() {
        return planRepository.findAll().stream()
            .map(PlanDto::fromDomain)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlanDto getPlan(String planId) {
        return PlanDto.fromDomain(plan(planId));
    }

    @Transactional
    public void deletePlan(String planId) {
        plan(planId);   // 404 si l'offre n'existe pas
        long utilisations = abonnements.findAll().stream().filter(a -> planId.equals(a.getPlanId())).count();
        if (utilisations > 0) {
            throw erreur(org.springframework.http.HttpStatus.CONFLICT,
                    "Cette offre est utilisée par " + utilisations + " abonnement(s) : changez d'abord leur offre.");
        }
        planRepository.deleteById(planId);
    }
}
