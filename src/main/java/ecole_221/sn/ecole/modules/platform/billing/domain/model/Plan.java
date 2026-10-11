package ecole_221.sn.ecole.modules.platform.billing.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class Plan {
    private final String id;
    private String name;
    private String description;
    private BigDecimal priceMonthly;
    private BigDecimal priceYearly;
    private Boolean isActive;
    private LocalDateTime createdAt;
    
    private final List<Entitlement> entitlements;

    public Plan(String id, String name, String description, BigDecimal priceMonthly, BigDecimal priceYearly, 
                Boolean isActive, LocalDateTime createdAt, List<Entitlement> entitlements) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = Objects.requireNonNull(name, "Le nom du plan est requis");
        this.description = description;
        this.priceMonthly = priceMonthly != null ? priceMonthly : BigDecimal.ZERO;
        this.priceYearly = priceYearly != null ? priceYearly : BigDecimal.ZERO;
        this.isActive = isActive != null ? isActive : true;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.entitlements = entitlements != null ? new ArrayList<>(entitlements) : new ArrayList<>();
    }

    public static Plan create(String name, String description, BigDecimal priceMonthly, BigDecimal priceYearly) {
        return new Plan(UUID.randomUUID().toString(), name, description, priceMonthly, priceYearly, true, LocalDateTime.now(), new ArrayList<>());
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPriceMonthly() { return priceMonthly; }
    public BigDecimal getPriceYearly() { return priceYearly; }
    public Boolean getIsActive() { return isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<Entitlement> getEntitlements() { return List.copyOf(entitlements); }

    public void update(String name, String description, BigDecimal priceMonthly, BigDecimal priceYearly) {
        this.name = Objects.requireNonNull(name);
        this.description = description;
        this.priceMonthly = priceMonthly;
        this.priceYearly = priceYearly;
    }

    public void activate() { this.isActive = true; }
    public void deactivate() { this.isActive = false; }

    public void addEntitlement(Entitlement entitlement) {
        Optional<Entitlement> existing = entitlements.stream()
                .filter(e -> e.getFeatureCode().equals(entitlement.getFeatureCode()))
                .findFirst();
        
        if (existing.isPresent()) {
            existing.get().updateLimit(entitlement.getMaxLimit());
            existing.get().toggle(entitlement.getIsEnabled());
        } else {
            this.entitlements.add(entitlement);
        }
    }
    
    public void removeEntitlement(String featureCode) {
        this.entitlements.removeIf(e -> e.getFeatureCode().equals(featureCode));
    }
}
