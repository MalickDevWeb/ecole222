package ecole_221.sn.ecole.modules.platform.billing.application.port.command;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record AssignPlanCommand(
    @NotBlank(message = "L'ID de l'organisation est obligatoire")
    String organizationId,
    @NotBlank(message = "L'ID du plan est obligatoire")
    String planId,
    LocalDateTime endDate
) {}
