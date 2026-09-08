package ecole_221.sn.ecole.modules.professor.domain.valueobject;

/**
 * Value Object représentant le statut d'une session de cours.
 * Immutabilité garantie par le mot-clé record.
 */
public record SessionStatus(String value) {
    
    public SessionStatus {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Le statut de session ne peut pas être vide");
        }
        
        // Normalisation et validation
        String normalized = value.trim().toUpperCase();
        if (!isValidStatus(normalized)) {
            throw new IllegalArgumentException("Statut de session invalide: " + value + 
                ". Statuts autorisés: SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, RESCHEDULED");
        }
    }
    
    private boolean isValidStatus(String status) {
        return status.equals("SCHEDULED") || 
               status.equals("IN_PROGRESS") || 
               status.equals("COMPLETED") || 
               status.equals("CANCELLED") || 
               status.equals("RESCHEDULED");
    }
    
    // Statuts prédéfinis
    public static final SessionStatus SCHEDULED = new SessionStatus("SCHEDULED");
    public static final SessionStatus IN_PROGRESS = new SessionStatus("IN_PROGRESS");
    public static final SessionStatus COMPLETED = new SessionStatus("COMPLETED");
    public static final SessionStatus CANCELLED = new SessionStatus("CANCELLED");
    public static final SessionStatus RESCHEDULED = new SessionStatus("RESCHEDULED");

    public static SessionStatus valueOf(String value) {
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "SCHEDULED" -> SCHEDULED;
            case "IN_PROGRESS" -> IN_PROGRESS;
            case "COMPLETED" -> COMPLETED;
            case "CANCELLED" -> CANCELLED;
            case "RESCHEDULED" -> RESCHEDULED;
            default -> throw new IllegalArgumentException("Valeur invalide: " + value);
        };
    }

    public String name() {
        return value();
    }
}