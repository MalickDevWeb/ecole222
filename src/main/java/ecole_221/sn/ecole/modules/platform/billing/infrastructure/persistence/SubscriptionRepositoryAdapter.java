package ecole_221.sn.ecole.modules.platform.billing.infrastructure.persistence;

import ecole_221.sn.ecole.modules.platform.billing.domain.model.Subscription;
import ecole_221.sn.ecole.modules.platform.billing.domain.port.repository.SubscriptionRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SubscriptionRepositoryAdapter implements SubscriptionRepositoryPort {

    private final SubscriptionJpaRepository repository;

    public SubscriptionRepositoryAdapter(SubscriptionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Subscription save(Subscription subscription) {
        SubscriptionJpaEntity entity = new SubscriptionJpaEntity();
        entity.setId(subscription.getId());
        entity.setOrganizationId(subscription.getOrganizationId());
        entity.setPlanId(subscription.getPlanId());
        entity.setStatus(subscription.getStatus());
        entity.setStartDate(subscription.getStartDate());
        entity.setEndDate(subscription.getEndDate());
        entity.setNextBillingDate(subscription.getNextBillingDate());

        SubscriptionJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Subscription> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Subscription> findByOrganizationId(String organizationId) {
        return repository.findByOrganizationId(organizationId).map(this::toDomain);
    }

    @Override
    public List<Subscription> findAll() {
        return repository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private Subscription toDomain(SubscriptionJpaEntity entity) {
        return new Subscription(
                entity.getId(),
                entity.getOrganizationId(),
                entity.getPlanId(),
                entity.getStatus(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getNextBillingDate()
        );
    }
}
