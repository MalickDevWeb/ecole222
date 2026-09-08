package ecole_221.sn.ecole.modules.collaboration.domain.model;

import ecole_221.sn.ecole.modules.collaboration.domain.valueobject.MessageStatus;
import ecole_221.sn.ecole.modules.collaboration.domain.valueobject.MessageType;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Agrégat représentant un message de collaboration (type WhatsApp).
 * Constructeur privé pour garantir l'invariance - utiliser MessageFactory.
 */
public class Message {
    
    private final String id;
    private final String groupId;
    private final String senderId;
    private String content;
    private final MessageType messageType;
    private MessageStatus status;
    private final LocalDateTime timestamp;
    private String mediaUrl; // Pour les messages audio/image/video
    private Long duration; // Durée en secondes pour les messages audio/video
    
    // Constructeur package-private pour permettre aux Factories d'y accéder
    Message(String id, String groupId, String senderId, String content, 
                   MessageType messageType, MessageStatus status, LocalDateTime timestamp,
                   String mediaUrl, Long duration) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.groupId = Objects.requireNonNull(groupId, "L'ID du groupe ne peut pas être null");
        this.senderId = Objects.requireNonNull(senderId, "L'ID de l'expéditeur ne peut pas être null");
        this.content = content;
        this.messageType = Objects.requireNonNull(messageType, "Le type de message ne peut pas être null");
        this.status = Objects.requireNonNull(status, "Le statut ne peut pas être null");
        this.timestamp = Objects.requireNonNull(timestamp, "L'horodatage ne peut pas être null");
        this.mediaUrl = mediaUrl;
        this.duration = duration;
    }
    
    // Getters
    public String getId() { return id; }
    public String getGroupId() { return groupId; }
    public String getSenderId() { return senderId; }
    public String getContent() { return content; }
    public MessageType getMessageType() { return messageType; }
    public MessageStatus getStatus() { return status; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getMediaUrl() { return mediaUrl; }
    public Long getDuration() { return duration; }
    
    // Comportements métier
    public void markAsDelivered() {
        this.status = MessageStatus.DELIVERED;
    }
    
    public void markAsRead() {
        this.status = MessageStatus.READ;
    }
    
    public void markAsFailed() {
        this.status = MessageStatus.FAILED;
    }
    
    public void updateContent(String newContent) {
        if (messageType.isText()) {
            this.content = Objects.requireNonNull(newContent, "Le contenu ne peut pas être null");
        }
    }
    
    public boolean isFromSender(String senderId) {
        return this.senderId.equals(senderId);
    }
    
    public boolean hasMedia() {
        return messageType.isMedia() && mediaUrl != null && !mediaUrl.isBlank();
    }
    
    public boolean isAudioMessage() {
        return messageType.isAudio();
    }
    
    public boolean isSystemMessage() {
        return messageType.isSystem();
    }
    
    public String getFormattedDuration() {
        if (duration == null) return "";
        
        long minutes = duration / 60;
        long seconds = duration % 60;
        
        if (minutes > 0) {
            return String.format("%d:%02d", minutes, seconds);
        }
        return String.format("0:%02d", seconds);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(id, message.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Message{" +
                "id='" + id + '\'' +
                ", groupId='" + groupId + '\'' +
                ", senderId='" + senderId + '\'' +
                ", messageType=" + messageType +
                ", status=" + status +
                ", timestamp=" + timestamp +
                '}';
    }
}