package ecole_221.sn.ecole.modules.notifications.domain.valueobject;

/**
 * Value Object représentant le type de notification.
 * Immutabilité garantie par le mot-clé record.
 */
public record NotificationType(String value) {
    
    public NotificationType {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Le type de notification ne peut pas être vide");
        }
        
        // Normalisation et validation
        String normalized = value.trim().toUpperCase();
        if (!isValidType(normalized)) {
            throw new IllegalArgumentException("Type de notification invalide: " + value + 
                ". Types autorisés: INFO, WARNING, ERROR, SUCCESS");
        }
    }
    
    private boolean isValidType(String type) {
        return type.equals("INFO") || 
               type.equals("WARNING") || 
               type.equals("ERROR") || 
               type.equals("SUCCESS");
    }
    
    // Types prédéfinis
    public static final NotificationType INFO = new NotificationType("INFO");
    public static final NotificationType WARNING = new NotificationType("WARNING");
    public static final NotificationType ERROR = new NotificationType("ERROR");
    public static final NotificationType SUCCESS = new NotificationType("SUCCESS");
}