package ecole_221.sn.ecole.modules.notifications.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_templates")
public class NotificationTemplateJpaEntity {

    @Id
    private String id;
    
    @Column(name = "template_key")
    private String templateKey; // 'payment_reminder', 'exam_alert', 'maintenance', etc.
    
    @Column(name = "template_name")
    private String templateName;
    
    @Column(name = "subject")
    private String subject;
    
    @Column(name = "body_template")
    private String bodyTemplate; // Template avec placeholders: {{student_name}}, {{amount}}, etc.
    
    @Column(name = "template_type")
    private String templateType; // 'email', 'sms', 'push', 'in_app'
    
    @Column(name = "category")
    private String category; // 'academic', 'financial', 'security', 'administrative'
    
    @Column(name = "default_priority")
    private String defaultPriority; // 'low', 'normal', 'high', 'urgent'
    
    @Column(name = "action_required")
    private Boolean actionRequired;
    
    @Column(name = "default_action_type")
    private String defaultActionType;
    
    @Column(name = "default_action_link")
    private String defaultActionLink;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @Column(name = "supported_variables")
    private String supportedVariables; // JSON: ["student_name", "amount", "due_date"]
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public NotificationTemplateJpaEntity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
        this.actionRequired = false;
    }

    public NotificationTemplateJpaEntity(String id, String templateKey, String templateName, 
                                         String subject, String bodyTemplate, String templateType, 
                                         String category) {
        this();
        this.id = id;
        this.templateKey = templateKey;
        this.templateName = templateName;
        this.subject = subject;
        this.bodyTemplate = bodyTemplate;
        this.templateType = templateType;
        this.category = category;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }
    
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getBodyTemplate() { return bodyTemplate; }
    public void setBodyTemplate(String bodyTemplate) { this.bodyTemplate = bodyTemplate; }
    
    public String getTemplateType() { return templateType; }
    public void setTemplateType(String templateType) { this.templateType = templateType; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getDefaultPriority() { return defaultPriority; }
    public void setDefaultPriority(String defaultPriority) { this.defaultPriority = defaultPriority; }
    
    public Boolean getActionRequired() { return actionRequired; }
    public void setActionRequired(Boolean actionRequired) { this.actionRequired = actionRequired; }
    
    public String getDefaultActionType() { return defaultActionType; }
    public void setDefaultActionType(String defaultActionType) { this.defaultActionType = defaultActionType; }
    
    public String getDefaultActionLink() { return defaultActionLink; }
    public void setDefaultActionLink(String defaultActionLink) { this.defaultActionLink = defaultActionLink; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public String getSupportedVariables() { return supportedVariables; }
    public void setSupportedVariables(String supportedVariables) { this.supportedVariables = supportedVariables; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @jakarta.persistence.PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}