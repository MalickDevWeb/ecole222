package ecole_221.sn.ecole.modules.notifications.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class NotificationJpaEntity {

    @Id
    private String id;
    
    @Column(name = "recipient_id")
    private String recipientId; // ID de l'utilisateur destinataire
    
    @Column(name = "recipient_role")
    private String recipientRole; // 'ADMIN', 'PROFESSEUR', 'ETUDIANT', 'ALL'
    
    @Column(name = "recipient_type")
    private String recipientType; // 'user', 'role', 'broadcast'
    
    @Column(name = "title")
    private String title;
    
    @Column(name = "message")
    private String message;
    
    @Column(name = "notification_type")
    private String notificationType; // 'info', 'warning', 'urgent', 'emergency', 'success'
    
    @Column(name = "category")
    private String category; // 'academic', 'financial', 'security', 'administrative', 'event'
    
    @Column(name = "priority")
    private String priority; // 'low', 'normal', 'high', 'urgent'
    
    @Column(name = "is_read")
    private Boolean isRead;
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @Column(name = "action_required")
    private Boolean actionRequired;
    
    @Column(name = "action_type")
    private String actionType; // 'payment', 'document', 'meeting', 'registration'
    
    @Column(name = "action_link")
    private String actionLink;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "sent_via_email")
    private Boolean sentViaEmail;
    
    @Column(name = "sent_via_sms")
    private Boolean sentViaSms;
    
    @Column(name = "sent_via_push")
    private Boolean sentViaPush;
    
    @Column(name = "additional_data")
    private String additionalData; // JSON pour données supplémentaires
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public NotificationJpaEntity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isRead = false;
        this.actionRequired = false;
        this.sentViaEmail = false;
        this.sentViaSms = false;
        this.sentViaPush = false;
    }
    
    @jakarta.persistence.PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public NotificationJpaEntity(String id, String recipientId, String recipientRole, 
                                String title, String message, String notificationType, 
                                String category, String priority) {
        this();
        this.id = id;
        this.recipientId = recipientId;
        this.recipientRole = recipientRole;
        this.title = title;
        this.message = message;
        this.notificationType = notificationType;
        this.category = category;
        this.priority = priority;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
    
    public String getRecipientRole() { return recipientRole; }
    public void setRecipientRole(String recipientRole) { this.recipientRole = recipientRole; }
    
    public String getRecipientType() { return recipientType; }
    public void setRecipientType(String recipientType) { this.recipientType = recipientType; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    
    public Boolean getActionRequired() { return actionRequired; }
    public void setActionRequired(Boolean actionRequired) { this.actionRequired = actionRequired; }
    
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    
    public String getActionLink() { return actionLink; }
    public void setActionLink(String actionLink) { this.actionLink = actionLink; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public Boolean getSentViaEmail() { return sentViaEmail; }
    public void setSentViaEmail(Boolean sentViaEmail) { this.sentViaEmail = sentViaEmail; }
    
    public Boolean getSentViaSms() { return sentViaSms; }
    public void setSentViaSms(Boolean sentViaSms) { this.sentViaSms = sentViaSms; }
    
    public Boolean getSentViaPush() { return sentViaPush; }
    public void setSentViaPush(Boolean sentViaPush) { this.sentViaPush = sentViaPush; }
    
    public String getAdditionalData() { return additionalData; }
    public void setAdditionalData(String additionalData) { this.additionalData = additionalData; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @jakarta.persistence.PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}