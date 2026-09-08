package ecole_221.sn.ecole.modules.dashboard.domain.valueobject;

public record KpiValue(String value, String unit) {
    public KpiValue {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("La valeur du KPI ne peut pas être vide");
        }
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("L'unité du KPI ne peut pas être vide");
        }
    }
    
    public String getFormattedValue() {
        return value + " " + unit;
    }
    
    public boolean isNumeric() {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public double asDouble() {
        if (!isNumeric()) {
            throw new IllegalStateException("La valeur n'est pas numérique: " + value);
        }
        return Double.parseDouble(value);
    }
}