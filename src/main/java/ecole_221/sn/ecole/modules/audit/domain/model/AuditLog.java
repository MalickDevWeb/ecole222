package ecole_221.sn.ecole.modules.audit.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Agrégat racine représentant un log d'audit.
 * Constructeur privé pour garantir l'invariance - utiliser AuditLogFactory.
 */
public class AuditLog {
    
    private final String id;
    private final String userId;
    private final String action;
    private String description;
    private final LocalDateTime timestamp;
    private final String ipAddress;
    private final String userAgent;
    
    // Constructeur package-private (accessible par Factory dans le même package)
    AuditLog(String id, String userId, String action, String description, 
                    LocalDateTime timestamp, String ipAddress, String userAgent) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.userId = Objects.requireNonNull(userId, "L'ID utilisateur ne peut pas être null");
        this.action = Objects.requireNonNull(action, "L'action ne peut pas être null");
        this.description = description;
        this.timestamp = Objects.requireNonNull(timestamp, "L'horodatage ne peut pas être null");
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }
    
    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getAction() { return action; }
    public String getDescription() { return description; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    
    // Comportements métier
    public void updateDescription(String newDescription) {
        this.description = newDescription;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditLog auditLog = (AuditLog) o;
        return Objects.equals(id, auditLog.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "AuditLog{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", action='" + action + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}