package ecole_221.sn.ecole.modules.platform.billing.domain.model;

import java.util.Objects;
import java.util.UUID;

public class Entitlement {
    private final String id;
    private String featureCode;
    private Integer maxLimit;
    private Boolean isEnabled;

    public Entitlement(String id, String featureCode, Integer maxLimit, Boolean isEnabled) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.featureCode = Objects.requireNonNull(featureCode, "Le code de la fonctionnalité est requis");
        this.maxLimit = maxLimit;
        this.isEnabled = isEnabled != null ? isEnabled : true;
    }

    public static Entitlement create(String featureCode, Integer maxLimit, Boolean isEnabled) {
        return new Entitlement(UUID.randomUUID().toString(), featureCode, maxLimit, isEnabled);
    }

    public String getId() {
        return id;
    }

    public String getFeatureCode() {
        return featureCode;
    }

    public Integer getMaxLimit() {
        return maxLimit;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void updateLimit(Integer maxLimit) {
        this.maxLimit = maxLimit;
    }

    public void toggle(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entitlement that = (Entitlement) o;
        return Objects.equals(id, that.id) && Objects.equals(featureCode, that.featureCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, featureCode);
    }
}
