package ecole_221.sn.ecole.modules.platform.billing.application.port.dto;

import ecole_221.sn.ecole.modules.platform.billing.domain.model.Subscription;
import java.time.LocalDateTime;

public record SubscriptionDto(
    String id,
    String organizationId,
    String planId,
    String status,
    LocalDateTime startDate,
    LocalDateTime endDate,
    LocalDateTime nextBillingDate
) {
    public static SubscriptionDto fromDomain(Subscription sub) {
        return new SubscriptionDto(
            sub.getId(),
            sub.getOrganizationId(),
            sub.getPlanId(),
            sub.getStatus(),
            sub.getStartDate(),
            sub.getEndDate(),
            sub.getNextBillingDate()
        );
    }
}
