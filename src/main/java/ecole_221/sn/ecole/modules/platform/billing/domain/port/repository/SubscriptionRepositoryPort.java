package ecole_221.sn.ecole.modules.platform.billing.domain.port.repository;

import ecole_221.sn.ecole.modules.platform.billing.domain.model.Subscription;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepositoryPort {
    Subscription save(Subscription subscription);
    Optional<Subscription> findById(String id);
    Optional<Subscription> findByOrganizationId(String organizationId);
    List<Subscription> findAll();
}
