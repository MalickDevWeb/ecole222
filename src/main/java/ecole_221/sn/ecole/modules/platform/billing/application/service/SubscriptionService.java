package ecole_221.sn.ecole.modules.platform.billing.application.service;

import ecole_221.sn.ecole.modules.platform.billing.application.port.command.AssignPlanCommand;
import ecole_221.sn.ecole.modules.platform.billing.application.port.dto.SubscriptionDto;
import ecole_221.sn.ecole.modules.platform.billing.domain.model.Plan;
import ecole_221.sn.ecole.modules.platform.billing.domain.model.Subscription;
import ecole_221.sn.ecole.modules.platform.billing.domain.port.repository.PlanRepositoryPort;
import ecole_221.sn.ecole.modules.platform.billing.domain.port.repository.SubscriptionRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SubscriptionService {
    
    private final SubscriptionRepositoryPort subscriptionRepository;
    private final PlanRepositoryPort planRepository;

    public SubscriptionService(SubscriptionRepositoryPort subscriptionRepository, PlanRepositoryPort planRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
    }

    @Transactional
    public SubscriptionDto subscribeTenant(AssignPlanCommand cmd) {
        Plan plan = planRepository.findById(cmd.planId()).orElseThrow(() -> new IllegalArgumentException("Plan non trouvé"));
        
        Optional<Subscription> existing = subscriptionRepository.findByOrganizationId(cmd.organizationId());
        Subscription subscription;
        if (existing.isPresent()) {
            subscription = existing.get();
            subscription.changePlan(plan.getId());
            subscription.activate();
        } else {
            subscription = Subscription.create(cmd.organizationId(), plan.getId(), cmd.endDate());
        }
        
        Subscription saved = subscriptionRepository.save(subscription);
        return SubscriptionDto.fromDomain(saved);
    }

    @Transactional(readOnly = true)
    public SubscriptionDto getSubscriptionForTenant(String tenantId) {
        Subscription subscription = subscriptionRepository.findByOrganizationId(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Aucun abonnement trouvé pour ce tenant"));
        return SubscriptionDto.fromDomain(subscription);
    }
    
    @Transactional(readOnly = true)
    public List<SubscriptionDto> listSubscriptions() {
        return subscriptionRepository.findAll().stream()
            .map(SubscriptionDto::fromDomain)
            .collect(Collectors.toList());
    }

    @Transactional
    public void suspendSubscription(String subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Abonnement non trouvé"));
        subscription.suspend();
        subscriptionRepository.save(subscription);
    }
}
