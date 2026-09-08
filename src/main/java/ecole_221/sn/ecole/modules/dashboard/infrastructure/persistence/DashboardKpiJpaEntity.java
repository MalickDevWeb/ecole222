package ecole_221.sn.ecole.modules.dashboard.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "dashboard_kpis")
public class DashboardKpiJpaEntity {

    @Id
    private String id;
    
    @Column(name = "kpi_key")
    private String kpiKey; // 'recovery_rate', 'cash_flow', 'ebitda', 'qr_suspended', 'open_tickets'
    
    @Column(name = "kpi_name")
    private String kpiName;
    
    @Column(name = "value")
    private String value; // valeur comme string pour supporter différents formats
    
    @Column(name = "value_type")
    private String valueType; // 'percentage', 'currency', 'count', 'ratio'
    
    @Column(name = "trend")
    private String trend; // 'up', 'down', 'stable'
    
    @Column(name = "trend_value")
    private String trendValue; // '+2.4%', '-1.2%', etc.
    
    @Column(name = "currency")
    private String currency; // 'FCFA', 'EUR', etc.
    
    @Column(name = "category")
    private String category; // 'finance', 'academic', 'security', 'operations'
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public DashboardKpiJpaEntity() {
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }

    public DashboardKpiJpaEntity(String id, String kpiKey, String kpiName, String value, 
                                 String valueType, String trend, String trendValue, 
                                 String currency, String category, String description) {
        this();
        this.id = id;
        this.kpiKey = kpiKey;
        this.kpiName = kpiName;
        this.value = value;
        this.valueType = valueType;
        this.trend = trend;
        this.trendValue = trendValue;
        this.currency = currency;
        this.category = category;
        this.description = description;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getKpiKey() { return kpiKey; }
    public void setKpiKey(String kpiKey) { this.kpiKey = kpiKey; }
    
    public String getKpiName() { return kpiName; }
    public void setKpiName(String kpiName) { this.kpiName = kpiName; }
    
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    
    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }
    
    public String getTrend() { return trend; }
    public void setTrend(String trend) { this.trend = trend; }
    
    public String getTrendValue() { return trendValue; }
    public void setTrendValue(String trendValue) { this.trendValue = trendValue; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    @jakarta.persistence.PreUpdate
    public void preUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }
}