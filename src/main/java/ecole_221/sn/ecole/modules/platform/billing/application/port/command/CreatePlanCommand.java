package ecole_221.sn.ecole.modules.platform.billing.application.port.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreatePlanCommand(
    @NotBlank(message = "Le nom du plan est obligatoire")
    String name,
    String description,
    @NotNull(message = "Le prix mensuel est obligatoire")
    BigDecimal priceMonthly,
    @NotNull(message = "Le prix annuel est obligatoire")
    BigDecimal priceYearly
) {}
