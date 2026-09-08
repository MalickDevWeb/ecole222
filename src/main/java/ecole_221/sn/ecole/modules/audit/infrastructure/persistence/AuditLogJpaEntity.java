package ecole_221.sn.ecole.modules.audit.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLogJpaEntity {

    @Id
    private String id;
    
    @Column(name = "action_type")
    private String actionType; // 'LOGIN', 'LOGOUT', 'CREATE', 'UPDATE', 'DELETE', 'SCAN', 'BLOCK', etc.
    
    @Column(name = "entity_type")
    private String entityType; // 'user', 'student', 'professor', 'course', 'payment', 'gate', etc.
    
    @Column(name = "entity_id")
    private String entityId;
    
    @Column(name = "actor_id")
    private String actorId; // ID de l'utilisateur qui a effectué l'action
    
    @Column(name = "actor_name")
    private String actorName;
    
    @Column(name = "actor_role")
    private String actorRole; // 'ADMIN', 'PROFESSEUR', 'ETUDIANT', 'VIGIL', 'SYSTEM'
    
    @Column(name = "action_description")
    private String actionDescription;
    
    @Column(name = "old_value")
    private String oldValue; // JSON de l'ancienne valeur pour les updates
    
    @Column(name = "new_value")
    private String newValue; // JSON de la nouvelle valeur
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(name = "severity")
    private String severity; // 'info', 'warning', 'critical'
    
    @Column(name = "status")
    private String status; // 'success', 'failure', 'pending'
    
    @Column(name = "failure_reason")
    private String failureReason;
    
    @Column(name = "module")
    private String module; // 'auth', 'finance', 'security', 'academic', 'admin'
    
    @Column(name = "additional_context")
    private String additionalContext; // JSON pour contexte supplémentaire
    
    @Column(name = "action_timestamp")
    private LocalDateTime actionTimestamp;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public AuditLogJpaEntity() {
        this.createdAt = LocalDateTime.now();
        this.actionTimestamp = LocalDateTime.now();
        this.status = "success";
        this.severity = "info";
    }

    public AuditLogJpaEntity(String id, String actionType, String entityType, String entityId,
                            String actorId, String actorName, String actorRole, 
                            String actionDescription, String module) {
        this();
        this.id = id;
        this.actionType = actionType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.actorId = actorId;
        this.actorName = actorName;
        this.actorRole = actorRole;
        this.actionDescription = actionDescription;
        this.module = module;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    
    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    
    public String getActorRole() { return actorRole; }
    public void setActorRole(String actorRole) { this.actorRole = actorRole; }
    
    public String getActionDescription() { return actionDescription; }
    public void setActionDescription(String actionDescription) { this.actionDescription = actionDescription; }
    
    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }
    
    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    
    public String getAdditionalContext() { return additionalContext; }
    public void setAdditionalContext(String additionalContext) { this.additionalContext = additionalContext; }
    
    public LocalDateTime getActionTimestamp() { return actionTimestamp; }
    public void setActionTimestamp(LocalDateTime actionTimestamp) { this.actionTimestamp = actionTimestamp; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}