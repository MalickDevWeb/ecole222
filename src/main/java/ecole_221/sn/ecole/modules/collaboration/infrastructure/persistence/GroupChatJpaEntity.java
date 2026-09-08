package ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "group_chats", 
       indexes = {
           @Index(name = "idx_group_chats_creator", columnList = "creator_id"),
           @Index(name = "idx_group_chats_type", columnList = "chat_type"),
           @Index(name = "idx_group_chats_created", columnList = "created_at"),
           @Index(name = "idx_group_chats_last_message", columnList = "last_message_time"),
           @Index(name = "idx_group_chats_archived", columnList = "archived")
       })
public class GroupChatJpaEntity {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "organization_id")
    private String organizationId;

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

    
    @Id
    @Column(name = "id", length = 36)
    private String id;
    
    @Column(name = "name", length = 255)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
    
    @Column(name = "creator_id", length = 36, nullable = false)
    private String creatorId;
    
    @Column(name = "chat_type", length = 20, nullable = false)
    private String chatType;
    
    @ElementCollection
    @CollectionTable(name = "group_chat_participants", 
                    joinColumns = @JoinColumn(name = "chat_id"),
                    indexes = @Index(name = "idx_participants_user_id", columnList = "user_id"))
    @Column(name = "user_id", length = 36)
    private Set<String> participantIds = new HashSet<>();
    
    @ElementCollection
    @CollectionTable(name = "group_chat_admins", 
                    joinColumns = @JoinColumn(name = "chat_id"),
                    indexes = @Index(name = "idx_admins_user_id", columnList = "user_id"))
    @Column(name = "user_id", length = 36)
    private Set<String> adminIds = new HashSet<>();
    
    @Column(name = "last_message_time")
    private LocalDateTime lastMessageTime;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "muted")
    private boolean muted = false;
    
    @Column(name = "muted_until")
    private LocalDateTime mutedUntil;
    
    @Column(name = "archived")
    private boolean archived = false;
    
    @Column(name = "pinned_message_id", length = 36)
    private String pinnedMessageId;
    
    @Column(name = "max_participants")
    private int maxParticipants = 0;
    
    // Constructeurs
    public GroupChatJpaEntity() {}
    
    public GroupChatJpaEntity(String id, String name, String description, String avatarUrl, 
                             String creatorId, String chatType, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.avatarUrl = avatarUrl;
        this.creatorId = creatorId;
        this.chatType = chatType;
        this.createdAt = createdAt;
        this.lastMessageTime = createdAt;
    }
    
    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    
    public String getChatType() { return chatType; }
    public void setChatType(String chatType) { this.chatType = chatType; }
    
    public Set<String> getParticipantIds() { return participantIds; }
    public void setParticipantIds(Set<String> participantIds) { this.participantIds = participantIds; }
    
    public Set<String> getAdminIds() { return adminIds; }
    public void setAdminIds(Set<String> adminIds) { this.adminIds = adminIds; }
    
    public LocalDateTime getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(LocalDateTime lastMessageTime) { this.lastMessageTime = lastMessageTime; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public boolean isMuted() { return muted; }
    public void setMuted(boolean muted) { this.muted = muted; }
    
    public LocalDateTime getMutedUntil() { return mutedUntil; }
    public void setMutedUntil(LocalDateTime mutedUntil) { this.mutedUntil = mutedUntil; }
    
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
    
    public String getPinnedMessageId() { return pinnedMessageId; }
    public void setPinnedMessageId(String pinnedMessageId) { this.pinnedMessageId = pinnedMessageId; }
    
    public int getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(int maxParticipants) { this.maxParticipants = maxParticipants; }
}