package ecole_221.sn.ecole.modules.dashboard.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
public class ActivityLogJpaEntity {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "organization_id")
    private String organizationId;

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }


    @Id
    private String id;
    
    @Column(name = "activity_type")
    private String activityType; // 'inscription', 'paiement', 'securite', 'planning', 'autre'
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "user_id")
    private String userId;
    
    @Column(name = "user_name")
    private String userName;
    
    @Column(name = "user_role")
    private String userRole;
    
    @Column(name = "entity_type")
    private String entityType; // 'student', 'professor', 'course', 'payment', etc.
    
    @Column(name = "entity_id")
    private String entityId;
    
    @Column(name = "additional_info")
    private String additionalInfo; // JSON pour données supplémentaires
    
    @Column(name = "activity_time")
    private LocalDateTime activityTime;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public ActivityLogJpaEntity() {
        this.createdAt = LocalDateTime.now();
        this.activityTime = LocalDateTime.now();
    }

    public ActivityLogJpaEntity(String id, String activityType, String description, 
                                 String userId, String userName, String userRole,
                                 String entityType, String entityId, String additionalInfo) {
        this();
        this.id = id;
        this.activityType = activityType;
        this.description = description;
        this.userId = userId;
        this.userName = userName;
        this.userRole = userRole;
        this.entityType = entityType;
        this.entityId = entityId;
        this.additionalInfo = additionalInfo;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
    
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    
    public String getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }
    
    public LocalDateTime getActivityTime() { return activityTime; }
    public void setActivityTime(LocalDateTime activityTime) { this.activityTime = activityTime; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}