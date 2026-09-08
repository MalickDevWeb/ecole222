package ecole_221.sn.ecole.modules.dashboard.domain.model;

import ecole_221.sn.ecole.modules.dashboard.domain.valueobject.KpiTrend;
import ecole_221.sn.ecole.modules.dashboard.domain.valueobject.KpiValue;

import java.util.UUID;

public class DashboardKpiFactory {
    
    public static DashboardKpi create(String name, KpiValue value, KpiTrend trend) {
        String id = UUID.randomUUID().toString();
        
        return new DashboardKpi(id, name, value.value(), value.unit(), trend.trend());
    }
    
    public static DashboardKpi createDefault(String name, String value, String unit) {
        return create(name, new KpiValue(value, unit), KpiTrend.NEUTRAL);
    }
    
    public static DashboardKpi reconstitute(String id, String name, String value, String unit, String trend) {
        return new DashboardKpi(id, name, value, unit, trend);
    }
}