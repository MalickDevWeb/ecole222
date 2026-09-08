package ecole_221.sn.ecole.modules.sync.domain.model;

import ecole_221.sn.ecole.modules.sync.domain.valueobject.SyncStatus;

import java.util.Objects;

/**
 * Agrégat racine représentant un résultat de synchronisation.
 * Constructeur privé pour garantir l'invariance - utiliser SyncResultFactory.
 */
public class SyncResult {
    
    private final String actionId;
    private final SyncStatus status;
    private final String message;
    private final long timestamp;
    
    // Constructeur privé
    private SyncResult(String actionId, SyncStatus status, String message, long timestamp) {
        this.actionId = Objects.requireNonNull(actionId, "L'ID action ne peut pas être null");
        this.status = Objects.requireNonNull(status, "Le statut ne peut pas être null");
        this.message = message;
        this.timestamp = timestamp;
    }

    // Méthode package-private pour la Factory
    static SyncResult createInternal(String actionId, SyncStatus status, String message, long timestamp) {
        return new SyncResult(actionId, status, message, timestamp);
    }
    
    // Getters
    public String getActionId() { return actionId; }
    public SyncStatus getStatus() { return status; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    
    // Comportements métier
    public boolean isSuccess() {
        return this.status.equals(SyncStatus.SUCCESS);
    }
    
    public boolean isFailed() {
        return this.status.equals(SyncStatus.FAILED);
    }
    
    public boolean isPending() {
        return this.status.equals(SyncStatus.PENDING);
    }
    
    public boolean isInProgress() {
        return this.status.equals(SyncStatus.IN_PROGRESS);
    }
    
    public boolean isRecent(int minutes) {
        long minutesInMs = minutes * 60 * 1000;
        return (System.currentTimeMillis() - this.timestamp) < minutesInMs;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SyncResult that = (SyncResult) o;
        return Objects.equals(actionId, that.actionId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(actionId);
    }
    
    @Override
    public String toString() {
        return "SyncResult{" +
                "actionId='" + actionId + '\'' +
                ", status=" + status +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}