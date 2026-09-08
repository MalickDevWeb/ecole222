package ecole_221.sn.ecole.modules.collaboration.domain.model;

import ecole_221.sn.ecole.modules.collaboration.domain.valueobject.GroupChatType;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Agrégat représentant une discussion de groupe (type WhatsApp).
 * Constructeur privé pour garantir l'invariance - utiliser GroupChatFactory.
 */
public class GroupChat {
    
    private final String id;
    private String name;
    private String description;
    private String avatarUrl; // Stocké sur Cloudinary
    private final String creatorId;
    private final GroupChatType chatType;
    private final Set<String> participantIds;
    private final Set<String> adminIds;
    private LocalDateTime lastMessageTime;
    private final LocalDateTime createdAt;
    private boolean muted;
    private LocalDateTime mutedUntil;
    private boolean archived;
    private String pinnedMessageId;
    private int maxParticipants;
    
    // Constructeur package-private pour permettre aux Factories d'y accéder
    GroupChat(String id, String name, String description, String avatarUrl, String creatorId,
                     GroupChatType chatType, Set<String> participantIds, Set<String> adminIds,
                     LocalDateTime lastMessageTime, LocalDateTime createdAt, boolean muted,
                     LocalDateTime mutedUntil, boolean archived, String pinnedMessageId,
                     int maxParticipants) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.name = name;
        this.description = description;
        this.avatarUrl = avatarUrl;
        this.creatorId = Objects.requireNonNull(creatorId, "L'ID du créateur ne peut pas être null");
        this.chatType = Objects.requireNonNull(chatType, "Le type de chat ne peut pas être null");
        this.participantIds = new HashSet<>(Objects.requireNonNull(participantIds, "Les participants ne peuvent pas être null"));
        this.adminIds = new HashSet<>(Objects.requireNonNull(adminIds, "Les admins ne peuvent pas être null"));
        this.lastMessageTime = lastMessageTime;
        this.createdAt = Objects.requireNonNull(createdAt, "La date de création ne peut pas être null");
        this.muted = muted;
        this.mutedUntil = mutedUntil;
        this.archived = archived;
        this.pinnedMessageId = pinnedMessageId;
        this.maxParticipants = maxParticipants;
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getCreatorId() { return creatorId; }
    public GroupChatType getChatType() { return chatType; }
    public Set<String> getParticipantIds() { return new HashSet<>(participantIds); }
    public Set<String> getAdminIds() { return new HashSet<>(adminIds); }
    public LocalDateTime getLastMessageTime() { return lastMessageTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isMuted() { return muted; }
    public LocalDateTime getMutedUntil() { return mutedUntil; }
    public boolean isArchived() { return archived; }
    public String getPinnedMessageId() { return pinnedMessageId; }
    public int getMaxParticipants() { return maxParticipants; }
    public int getParticipantCount() { return participantIds.size(); }
    
    // Comportements métier
    public void updateName(String newName) {
        if (chatType.isIndividual()) {
            throw new IllegalStateException("Impossible de renommer une conversation individuelle");
        }
        this.name = Objects.requireNonNull(newName, "Le nom ne peut pas être null");
    }
    
    public void updateDescription(String newDescription) {
        this.description = newDescription;
    }
    
    public void updateAvatar(String newAvatarUrl) {
        this.avatarUrl = newAvatarUrl;
    }
    
    public void addParticipant(String participantId) {
        Objects.requireNonNull(participantId, "L'ID du participant ne peut pas être null");
        
        if (maxParticipants > 0 && participantIds.size() >= maxParticipants) {
            throw new IllegalStateException("Le groupe est plein");
        }
        
        participantIds.add(participantId);
    }
    
    public void removeParticipant(String participantId) {
        participantIds.remove(participantId);
        adminIds.remove(participantId); // Retirer aussi des admins si c'était un admin
    }
    
    public void addAdmin(String adminId) {
        Objects.requireNonNull(adminId, "L'ID de l'admin ne peut pas être null");
        
        if (!participantIds.contains(adminId)) {
            throw new IllegalStateException("L'utilisateur doit être participant pour être admin");
        }
        
        adminIds.add(adminId);
    }
    
    public void removeAdmin(String adminId) {
        if (adminId.equals(creatorId)) {
            throw new IllegalStateException("Impossible de retirer le créateur des admins");
        }
        adminIds.remove(adminId);
    }
    
    public void updateLastMessageTime(LocalDateTime timestamp) {
        this.lastMessageTime = Objects.requireNonNull(timestamp, "L'horodatage ne peut pas être null");
    }
    
    public void mute(LocalDateTime until) {
        this.muted = true;
        this.mutedUntil = until;
    }
    
    public void unmute() {
        this.muted = false;
        this.mutedUntil = null;
    }
    
    public void archive() {
        this.archived = true;
    }
    
    public void unarchive() {
        this.archived = false;
    }
    
    public void pinMessage(String messageId) {
        this.pinnedMessageId = messageId;
    }
    
    public void unpinMessage() {
        this.pinnedMessageId = null;
    }
    
    public boolean isCreator(String userId) {
        return creatorId.equals(userId);
    }
    
    public boolean isAdmin(String userId) {
        return adminIds.contains(userId) || isCreator(userId);
    }
    
    public boolean isParticipant(String userId) {
        return participantIds.contains(userId);
    }
    
    public boolean canUserPost(String userId) {
        return isParticipant(userId) && !isMutedForUser(userId);
    }
    
    public boolean canUserManage(String userId) {
        return isAdmin(userId);
    }
    
    public boolean isMutedForUser(String userId) {
        if (!muted) return false;
        if (mutedUntil == null) return true;
        return LocalDateTime.now().isBefore(mutedUntil);
    }
    
    public boolean hasCapacity() {
        return maxParticipants <= 0 || participantIds.size() < maxParticipants;
    }
    
    public boolean isActive() {
        return !archived && lastMessageTime != null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupChat groupChat = (GroupChat) o;
        return Objects.equals(id, groupChat.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "GroupChat{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", chatType=" + chatType +
                ", participantCount=" + participantIds.size() +
                ", adminCount=" + adminIds.size() +
                ", archived=" + archived +
                '}';
    }
}