package ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages", 
       indexes = {
           @Index(name = "idx_messages_group_id", columnList = "group_id"),
           @Index(name = "idx_messages_sender_id", columnList = "sender_id"),
           @Index(name = "idx_messages_timestamp", columnList = "timestamp"),
           @Index(name = "idx_messages_group_timestamp", columnList = "group_id, timestamp"),
           @Index(name = "idx_messages_sender_timestamp", columnList = "sender_id, timestamp")
       })
public class MessageJpaEntityOptimized {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "organization_id")
    private String organizationId;

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

    
    @Id
    @Column(name = "id", length = 36)
    private String id;
    
    @Column(name = "group_id", length = 36, nullable = false)
    private String groupId;
    
    @Column(name = "sender_id", length = 36, nullable = false)
    private String senderId;
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    @Transient
    private String messageType = "TEXT";
    
    @Transient
    private String status = "SENT";
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "media_url", length = 500)
    private String mediaUrl;
    
    @Column(name = "duration")
    private Long duration;
    
    @Column(name = "reply_to_id", length = 36)
    private String replyToId;
    
    @Column(name = "forwarded_from", length = 36)
    private String forwardedFrom;
    
    @Column(name = "deleted_for_everyone")
    private boolean deletedForEveryone = false;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructeurs
    public MessageJpaEntityOptimized() {}
    
    public MessageJpaEntityOptimized(String id, String groupId, String senderId, String content, 
                                    String messageType, String status, LocalDateTime timestamp) {
        this.id = id;
        this.groupId = groupId;
        this.senderId = senderId;
        this.content = content;
        this.messageType = messageType;
        this.status = status;
        this.timestamp = timestamp;
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { 
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    
    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }
    
    public String getReplyToId() { return replyToId; }
    public void setReplyToId(String replyToId) { this.replyToId = replyToId; }
    
    public String getForwardedFrom() { return forwardedFrom; }
    public void setForwardedFrom(String forwardedFrom) { this.forwardedFrom = forwardedFrom; }
    
    public boolean isDeletedForEveryone() { return deletedForEveryone; }
    public void setDeletedForEveryone(boolean deletedForEveryone) { 
        this.deletedForEveryone = deletedForEveryone;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PostLoad
    public void postLoad() {
        if (this.messageType == null) {
            this.messageType = "TEXT";
        }
        if (this.status == null) {
            this.status = "SENT";
        }
    }
}