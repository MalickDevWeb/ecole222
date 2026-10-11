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
    
    private final PlanRepositoryPort planRepository;

    public PlanService(PlanRepositoryPort planRepository) {
        this.planRepository = planRepository;
    }

    @Transactional
    public PlanDto createPlan(CreatePlanCommand cmd) {
        Plan plan = Plan.create(cmd.name(), cmd.description(), cmd.priceMonthly(), cmd.priceYearly());
        Plan saved = planRepository.save(plan);
        return PlanDto.fromDomain(saved);
    }

    @Transactional
    public PlanDto addEntitlement(String planId, AddEntitlementCommand cmd) {
        Plan plan = planRepository.findById(planId).orElseThrow(() -> new IllegalArgumentException("Plan non trouvé"));
        Entitlement entitlement = Entitlement.create(cmd.featureCode(), cmd.maxLimit(), cmd.isEnabled());
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
        Plan plan = planRepository.findById(planId).orElseThrow(() -> new IllegalArgumentException("Plan non trouvé"));
        return PlanDto.fromDomain(plan);
    }

    @Transactional
    public void deletePlan(String planId) {
        planRepository.deleteById(planId);
    }
}
