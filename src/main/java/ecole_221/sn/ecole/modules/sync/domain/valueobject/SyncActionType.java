package ecole_221.sn.ecole.modules.sync.domain.valueobject;

/**
 * Value Object représentant le type d'action de synchronisation.
 * Immutabilité garantie par le mot-clé record.
 */
public record SyncActionType(String value) {
    
    public SyncActionType {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Le type d'action ne peut pas être vide");
        }
        
        // Normalisation et validation
        String normalized = value.trim().toUpperCase();
        if (!isValidType(normalized)) {
            throw new IllegalArgumentException("Type d'action invalide: " + value + 
                ". Types autorisés: CREATE, UPDATE, DELETE, SYNC");
        }
    }
    
    private boolean isValidType(String type) {
        return type.equals("CREATE") || 
               type.equals("UPDATE") || 
               type.equals("DELETE") || 
               type.equals("SYNC");
    }
    
    // Types prédéfinis
    public static final SyncActionType CREATE = new SyncActionType("CREATE");
    public static final SyncActionType UPDATE = new SyncActionType("UPDATE");
    public static final SyncActionType DELETE = new SyncActionType("DELETE");
    public static final SyncActionType SYNC = new SyncActionType("SYNC");

    public static SyncActionType valueOf(String value) {
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "CREATE" -> CREATE;
            case "UPDATE" -> UPDATE;
            case "DELETE" -> DELETE;
            case "SYNC" -> SYNC;
            default -> throw new IllegalArgumentException("Valeur invalide: " + value);
        };
    }

    public String name() {
        return value();
    }
}