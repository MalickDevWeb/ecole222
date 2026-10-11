package ecole_221.sn.ecole.modules.platform.billing.infrastructure.persistence;

import ecole_221.sn.ecole.modules.platform.billing.domain.model.Entitlement;
import ecole_221.sn.ecole.modules.platform.billing.domain.model.Plan;
import ecole_221.sn.ecole.modules.platform.billing.domain.port.repository.PlanRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PlanRepositoryAdapter implements PlanRepositoryPort {

    private final PlanJpaRepository planJpaRepository;
    private final EntitlementJpaRepository entitlementJpaRepository;

    public PlanRepositoryAdapter(PlanJpaRepository planJpaRepository, EntitlementJpaRepository entitlementJpaRepository) {
        this.planJpaRepository = planJpaRepository;
        this.entitlementJpaRepository = entitlementJpaRepository;
    }

    @Override
    public Plan save(Plan plan) {
        PlanJpaEntity planEntity = new PlanJpaEntity();
        planEntity.setId(plan.getId());
        planEntity.setName(plan.getName());
        planEntity.setDescription(plan.getDescription());
        planEntity.setPriceMonthly(plan.getPriceMonthly());
        planEntity.setPriceYearly(plan.getPriceYearly());
        planEntity.setIsActive(plan.getIsActive());
        planEntity.setCreatedAt(plan.getCreatedAt());
        
        planJpaRepository.save(planEntity);

        // Delete existing entitlements and re-insert
        entitlementJpaRepository.deleteByPlanId(plan.getId());
        
        List<EntitlementJpaEntity> entitlementEntities = plan.getEntitlements().stream().map(e -> {
            EntitlementJpaEntity ee = new EntitlementJpaEntity();
            ee.setId(e.getId());
            ee.setPlanId(plan.getId());
            ee.setFeatureCode(e.getFeatureCode());
            ee.setMaxLimit(e.getMaxLimit());
            ee.setIsEnabled(e.getIsEnabled());
            return ee;
        }).collect(Collectors.toList());
        
        entitlementJpaRepository.saveAll(entitlementEntities);
        
        return plan;
    }

    @Override
    public Optional<Plan> findById(String id) {
        return planJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Plan> findAll() {
        return planJpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        entitlementJpaRepository.deleteByPlanId(id);
        planJpaRepository.deleteById(id);
    }

    private Plan toDomain(PlanJpaEntity entity) {
        List<Entitlement> entitlements = entitlementJpaRepository.findByPlanId(entity.getId()).stream()
                .map(ee -> new Entitlement(ee.getId(), ee.getFeatureCode(), ee.getMaxLimit(), ee.getIsEnabled()))
                .collect(Collectors.toList());
        
        return new Plan(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPriceMonthly(),
                entity.getPriceYearly(),
                entity.getIsActive(),
                entity.getCreatedAt(),
                entitlements
        );
    }
}
