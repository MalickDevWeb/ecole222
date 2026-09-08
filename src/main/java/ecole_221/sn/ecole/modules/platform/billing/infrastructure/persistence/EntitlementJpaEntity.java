package ecole_221.sn.ecole.modules.platform.billing.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "entitlements")
public class EntitlementJpaEntity {

    @Id
    private String id;
    
    @Column(name = "plan_id", nullable = false)
    private String planId;
    
    @Column(name = "feature_code", nullable = false)
    private String featureCode;
    
    @Column(name = "max_limit")
    private Integer maxLimit;
    
    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getFeatureCode() { return featureCode; }
    public void setFeatureCode(String featureCode) { this.featureCode = featureCode; }

    public Integer getMaxLimit() { return maxLimit; }
    public void setMaxLimit(Integer maxLimit) { this.maxLimit = maxLimit; }

    public Boolean getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Boolean enabled) { isEnabled = enabled; }
}

