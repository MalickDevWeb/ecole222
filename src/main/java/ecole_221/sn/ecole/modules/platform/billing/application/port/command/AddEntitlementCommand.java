package ecole_221.sn.ecole.modules.platform.billing.application.port.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddEntitlementCommand(
    @NotBlank(message = "Le code de fonctionnalité est obligatoire")
    String featureCode,
    Integer maxLimit,
    @NotNull(message = "Le statut d'activation est obligatoire")
    Boolean isEnabled
) {}
