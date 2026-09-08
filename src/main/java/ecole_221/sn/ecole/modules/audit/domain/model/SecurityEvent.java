package ecole_221.sn.ecole.modules.audit.domain.model;

import ecole_221.sn.ecole.modules.audit.domain.valueobject.SeverityLevel;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Agrégat racine représentant un événement de sécurité.
 * Constructeur privé pour garantir l'invariance - utiliser SecurityEventFactory.
 */
public class SecurityEvent {
    
    private final String id;
    private final String eventType;
    private final String userId;
    private String description;
    private final SeverityLevel severity;
    private final LocalDateTime timestamp;
    private final String sourceIp;
    
    // Constructeur package-private (accessible par Factory dans le même package)
    SecurityEvent(String id, String eventType, String userId, String description, 
                         SeverityLevel severity, LocalDateTime timestamp, String sourceIp) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.eventType = Objects.requireNonNull(eventType, "Le type d'événement ne peut pas être null");
        this.userId = Objects.requireNonNull(userId, "L'ID utilisateur ne peut pas être null");
        this.description = description;
        this.severity = Objects.requireNonNull(severity, "Le niveau de gravité ne peut pas être null");
        this.timestamp = Objects.requireNonNull(timestamp, "L'horodatage ne peut pas être null");
        this.sourceIp = sourceIp;
    }
    
    // Getters
    public String getId() { return id; }
    public String getEventType() { return eventType; }
    public String getUserId() { return userId; }
    public String getDescription() { return description; }
    public SeverityLevel getSeverity() { return severity; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getSourceIp() { return sourceIp; }
    
    // Comportements métier
    public void updateDescription(String newDescription) {
        this.description = newDescription;
    }
    
    public boolean isCritical() {
        return severity.equals(SeverityLevel.CRITICAL);
    }
    
    public boolean isHigh() {
        return severity.equals(SeverityLevel.HIGH);
    }
    
    public boolean isMedium() {
        return severity.equals(SeverityLevel.MEDIUM);
    }
    
    public boolean isLow() {
        return severity.equals(SeverityLevel.LOW);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecurityEvent that = (SecurityEvent) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "SecurityEvent{" +
                "id='" + id + '\'' +
                ", eventType='" + eventType + '\'' +
                ", userId='" + userId + '\'' +
                ", severity=" + severity +
                ", timestamp=" + timestamp +
                '}';
    }
}