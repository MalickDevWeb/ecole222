package ecole_221.sn.ecole.modules.dashboard.domain.valueobject;

public record KpiTrend(String trend) {
    public static final KpiTrend UP = new KpiTrend("UP");
    public static final KpiTrend DOWN = new KpiTrend("DOWN");
    public static final KpiTrend STABLE = new KpiTrend("STABLE");
    public static final KpiTrend NEUTRAL = new KpiTrend("NEUTRAL");
    
    public KpiTrend {
        if (trend == null || trend.isBlank()) {
            throw new IllegalArgumentException("La tendance ne peut pas être vide");
        }
    }
    
    public boolean isUp() {
        return "UP".equals(trend);
    }
    
    public boolean isDown() {
        return "DOWN".equals(trend);
    }
    
    public boolean isStable() {
        return "STABLE".equals(trend);
    }
    
    public boolean isNeutral() {
        return "NEUTRAL".equals(trend);
    }
    
    public boolean isPositive() {
        return isUp() || isStable();
    }
}