package ecole_221.sn.ecole.modules.platform.billing.application.port.dto;

import ecole_221.sn.ecole.modules.platform.billing.domain.model.Entitlement;

public record EntitlementDto(
    String id,
    String featureCode,
    Integer maxLimit,
    Boolean isEnabled
) {
    public static EntitlementDto fromDomain(Entitlement entitlement) {
        return new EntitlementDto(
            entitlement.getId(),
            entitlement.getFeatureCode(),
            entitlement.getMaxLimit(),
            entitlement.getIsEnabled()
        );
    }
}
