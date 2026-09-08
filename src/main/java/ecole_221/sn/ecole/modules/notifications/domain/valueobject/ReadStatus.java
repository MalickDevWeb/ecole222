package ecole_221.sn.ecole.modules.notifications.domain.valueobject;

/**
 * Value Object représentant le statut de lecture d'une notification.
 * Immutabilité garantie par le mot-clé record.
 */
public record ReadStatus(String value) {
    
    public ReadStatus {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Le statut de lecture ne peut pas être vide");
        }
        
        // Normalisation et validation
        String normalized = value.trim().toUpperCase();
        if (!isValidStatus(normalized)) {
            throw new IllegalArgumentException("Statut de lecture invalide: " + value + 
                ". Statuts autorisés: READ, UNREAD");
        }
    }
    
    private boolean isValidStatus(String status) {
        return status.equals("READ") || 
               status.equals("UNREAD");
    }
    
    // Statuts prédéfinis
    public static final ReadStatus READ = new ReadStatus("READ");
    public static final ReadStatus UNREAD = new ReadStatus("UNREAD");
}