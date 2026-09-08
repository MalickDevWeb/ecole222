package ecole_221.sn.ecole.modules.sync.domain.model;

import ecole_221.sn.ecole.modules.sync.domain.valueobject.SyncActionType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * Agrégat racine représentant une action de synchronisation.
 * Constructeur privé pour garantir l'invariance - utiliser SyncActionFactory.
 */
public class SyncAction {
    
    private final String id;
    private final SyncActionType type;
    private final LocalDateTime timestamp;
    private final Map<String, Object> payload;
    
    // Constructeur privé
    private SyncAction(String id, SyncActionType type, LocalDateTime timestamp, Map<String, Object> payload) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.type = Objects.requireNonNull(type, "Le type ne peut pas être null");
        this.timestamp = Objects.requireNonNull(timestamp, "L'horodatage ne peut pas être null");
        this.payload = Objects.requireNonNull(payload, "Le payload ne peut pas être null");
    }

    // Méthode package-private pour la Factory
    static SyncAction createInternal(String id, SyncActionType type, LocalDateTime timestamp, Map<String, Object> payload) {
        return new SyncAction(id, type, timestamp, payload);
    }
    
    // Getters
    public String getId() { return id; }
    public SyncActionType getType() { return type; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Map<String, Object> getPayload() { return payload; }
    
    // Comportements métier
    public boolean isCreate() {
        return this.type.equals(SyncActionType.CREATE);
    }
    
    public boolean isUpdate() {
        return this.type.equals(SyncActionType.UPDATE);
    }
    
    public boolean isDelete() {
        return this.type.equals(SyncActionType.DELETE);
    }
    
    public boolean isSync() {
        return this.type.equals(SyncActionType.SYNC);
    }
    
    public boolean isRecent(int minutes) {
        return this.timestamp.isAfter(LocalDateTime.now().minusMinutes(minutes));
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SyncAction syncAction = (SyncAction) o;
        return Objects.equals(id, syncAction.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "SyncAction{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", timestamp=" + timestamp +
                ", payloadSize=" + (payload != null ? payload.size() : 0) +
                '}';
    }
}