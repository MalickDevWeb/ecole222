package ecole_221.sn.ecole.modules.platform.billing.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Subscription {
    private final String id;
    private final String organizationId;
    private String planId;
    private String status; // ACTIVE, SUSPENDED, CANCELLED, EXPIRED
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime nextBillingDate;

    public Subscription(String id, String organizationId, String planId, String status, 
                        LocalDateTime startDate, LocalDateTime endDate, LocalDateTime nextBillingDate) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID is required");
        this.planId = Objects.requireNonNull(planId, "Plan ID is required");
        this.status = status != null ? status : "ACTIVE";
        this.startDate = startDate != null ? startDate : LocalDateTime.now();
        this.endDate = endDate;
        this.nextBillingDate = nextBillingDate;
    }

    public static Subscription create(String organizationId, String planId, LocalDateTime endDate) {
        return new Subscription(
            UUID.randomUUID().toString(),
            organizationId,
            planId,
            "ACTIVE",
            LocalDateTime.now(),
            endDate,
            LocalDateTime.now().plusMonths(1) // Assuming monthly billing by default
        );
    }

    public String getId() { return id; }
    public String getOrganizationId() { return organizationId; }
    public String getPlanId() { return planId; }
    public String getStatus() { return status; }
    public LocalDateTime getStartDate() { return startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public LocalDateTime getNextBillingDate() { return nextBillingDate; }

    public void suspend() {
        this.status = "SUSPENDED";
    }

    public void activate() {
        this.status = "ACTIVE";
    }

    public void cancel() {
        this.status = "CANCELLED";
    }

    public void changePlan(String newPlanId) {
        this.planId = Objects.requireNonNull(newPlanId);
    }
}
