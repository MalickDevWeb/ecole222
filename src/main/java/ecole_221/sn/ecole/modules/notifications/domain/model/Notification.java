package ecole_221.sn.ecole.modules.notifications.domain.model;

import ecole_221.sn.ecole.modules.notifications.domain.valueobject.NotificationType;
import ecole_221.sn.ecole.modules.notifications.domain.valueobject.ReadStatus;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Agrégat racine représentant une notification.
 * Constructeur package-private pour garantir l'invariance - utiliser NotificationFactory.
 */
public class Notification {
    
    private final String id;
    private final String recipientId;
    private String title;
    private String content;
    private final NotificationType type;
    private ReadStatus readStatus;
    private final LocalDateTime createdAt;
    
    // Constructeur package-private pour accès par NotificationFactory
    Notification(String id, String recipientId, String title, String content,
                        NotificationType type, ReadStatus readStatus, LocalDateTime createdAt) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.recipientId = Objects.requireNonNull(recipientId, "L'ID destinataire ne peut pas être null");
        this.title = Objects.requireNonNull(title, "Le titre ne peut pas être null");
        this.content = content;
        this.type = Objects.requireNonNull(type, "Le type ne peut pas être null");
        this.readStatus = Objects.requireNonNull(readStatus, "Le statut de lecture ne peut pas être null");
        this.createdAt = Objects.requireNonNull(createdAt, "La date de création ne peut pas être null");
    }
    
    // Getters
    public String getId() { return id; }
    public String getRecipientId() { return recipientId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public NotificationType getType() { return type; }
    public ReadStatus getReadStatus() { return readStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    
    // Comportements métier
    public void updateTitle(String newTitle) {
        this.title = Objects.requireNonNull(newTitle, "Le titre ne peut pas être null");
    }
    
    public void updateContent(String newContent) {
        this.content = newContent;
    }
    
    public void markAsRead() {
        this.readStatus = ReadStatus.READ;
    }
    
    public void markAsUnread() {
        this.readStatus = ReadStatus.UNREAD;
    }
    
    public boolean isRead() {
        return this.readStatus.equals(ReadStatus.READ);
    }
    
    public boolean isUnread() {
        return this.readStatus.equals(ReadStatus.UNREAD);
    }
    
    public boolean isInfo() {
        return this.type.equals(NotificationType.INFO);
    }
    
    public boolean isWarning() {
        return this.type.equals(NotificationType.WARNING);
    }
    
    public boolean isError() {
        return this.type.equals(NotificationType.ERROR);
    }
    
    public boolean isSuccess() {
        return this.type.equals(NotificationType.SUCCESS);
    }
    
    public boolean isRecent(int hours) {
        return this.createdAt.isAfter(LocalDateTime.now().minusHours(hours));
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Notification{" +
                "id='" + id + '\'' +
                ", recipientId='" + recipientId + '\'' +
                ", title='" + title + '\'' +
                ", type=" + type +
                ", readStatus=" + readStatus +
                ", createdAt=" + createdAt +
                '}';
    }
}