package ecole_221.sn.ecole.modules.dashboard.domain.model;

import ecole_221.sn.ecole.modules.dashboard.domain.valueobject.KpiTrend;
import ecole_221.sn.ecole.modules.dashboard.domain.valueobject.KpiValue;

/**
 * Modèle de domaine représentant un KPI (Key Performance Indicator) du dashboard.
 */
public class DashboardKpi {
    private final String id;
    private final String name;
    private final String value;
    private final String unit;
    private final String trend;
    
    DashboardKpi(String id, String name, String value, String unit, String trend) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.unit = unit;
        this.trend = trend;
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getValue() { return value; }
    public String getUnit() { return unit; }
    public String getTrend() { return trend; }
    
    // Helper methods using Value Objects
    public KpiValue getKpiValue() {
        return new KpiValue(value, unit);
    }
    
    public KpiTrend getKpiTrend() {
        return new KpiTrend(trend);
    }
}