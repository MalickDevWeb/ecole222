package ecole_221.sn.ecole.modules.platform.billing.application;

import ecole_221.sn.ecole.modules.platform.billing.infrastructure.persistence.EntitlementJpaEntity;
import ecole_221.sn.ecole.modules.platform.billing.infrastructure.persistence.SubscriptionJpaEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

@Service
public class EntitlementService {

    private final EntityManager entityManager;

    public EntitlementService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public boolean checkLimit(String organizationId, String featureCode, int currentUsage) {
        // 1. Trouver l'abonnement actif
        List<SubscriptionJpaEntity> subs = entityManager.createQuery(
                "SELECT s FROM SubscriptionJpaEntity s WHERE s.organizationId = :orgId AND s.status = 'ACTIVE'", 
                SubscriptionJpaEntity.class)
                .setParameter("orgId", organizationId)
                .getResultList();
                
        if (subs.isEmpty()) {
            return false; // Pas d'abonnement actif
        }
        
        String planId = subs.get(0).getPlanId();
        
        // 2. Trouver la limite pour cette feature
        List<EntitlementJpaEntity> entitlements = entityManager.createQuery(
                "SELECT e FROM EntitlementJpaEntity e WHERE e.planId = :planId AND e.featureCode = :code",
                EntitlementJpaEntity.class)
                .setParameter("planId", planId)
                .setParameter("code", featureCode)
                .getResultList();
                
        if (entitlements.isEmpty() || !entitlements.get(0).getIsEnabled()) {
            return false; // Feature non incluse
        }
        
        Integer maxLimit = entitlements.get(0).getMaxLimit();
        if (maxLimit == null) {
            return true; // Illimité
        }
        
        return currentUsage < maxLimit;
    }
}

