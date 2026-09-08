package ecole_221.sn.ecole.modules.sync.domain.valueobject;

/**
 * Value Object représentant le statut de synchronisation.
 * Immutabilité garantie par le mot-clé record.
 */
public record SyncStatus(String value) {
    
    public SyncStatus {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Le statut de synchronisation ne peut pas être vide");
        }
        
        // Normalisation et validation
        String normalized = value.trim().toUpperCase();
        if (!isValidStatus(normalized)) {
            throw new IllegalArgumentException("Statut de synchronisation invalide: " + value + 
                ". Statuts autorisés: PENDING, IN_PROGRESS, SUCCESS, FAILED");
        }
    }
    
    private boolean isValidStatus(String status) {
        return status.equals("PENDING") || 
               status.equals("IN_PROGRESS") || 
               status.equals("SUCCESS") || 
               status.equals("FAILED");
    }
    
    // Statuts prédéfinis
    public static final SyncStatus PENDING = new SyncStatus("PENDING");
    public static final SyncStatus IN_PROGRESS = new SyncStatus("IN_PROGRESS");
    public static final SyncStatus SUCCESS = new SyncStatus("SUCCESS");
    public static final SyncStatus FAILED = new SyncStatus("FAILED");

    public static SyncStatus valueOf(String value) {
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "PENDING" -> PENDING;
            case "IN_PROGRESS" -> IN_PROGRESS;
            case "SUCCESS" -> SUCCESS;
            case "FAILED" -> FAILED;
            default -> throw new IllegalArgumentException("Valeur invalide: " + value);
        };
    }

    public String name() {
        return value();
    }
}