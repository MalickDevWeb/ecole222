package ecole_221.sn.ecole.modules.platform.billing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionJpaEntity, String> {
    Optional<SubscriptionJpaEntity> findByOrganizationId(String organizationId);
}
