package ecole_221.sn.ecole.modules.dashboard.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO représentant un KPI (Key Performance Indicator) du dashboard.
 */
@Schema(description = "KPI du dashboard")
public record DashboardKpiDto(
    @Schema(description = "Identifiant du KPI", example = "kpi-001")
    String id,
    
    @Schema(description = "Nom du KPI", example = "Total étudiants")
    String name,
    
    @Schema(description = "Valeur du KPI", example = "1500")
    String value,
    
    @Schema(description = "Unité", example = "étudiants")
    String unit,
    
    @Schema(description = "Tendance", example = "+5%")
    String trend
) {
    public DashboardKpiDto {
        // Validation can be added here
    }
}