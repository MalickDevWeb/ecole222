package ecole_221.sn.ecole.modules.platform.billing.application.port.dto;

import ecole_221.sn.ecole.modules.platform.billing.domain.model.Plan;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record PlanDto(
    String id,
    String name,
    String description,
    BigDecimal priceMonthly,
    BigDecimal priceYearly,
    Boolean isActive,
    LocalDateTime createdAt,
    List<EntitlementDto> entitlements
) {
    public static PlanDto fromDomain(Plan plan) {
        return new PlanDto(
            plan.getId(),
            plan.getName(),
            plan.getDescription(),
            plan.getPriceMonthly(),
            plan.getPriceYearly(),
            plan.getIsActive(),
            plan.getCreatedAt(),
            plan.getEntitlements().stream().map(EntitlementDto::fromDomain).collect(Collectors.toList())
        );
    }
}
