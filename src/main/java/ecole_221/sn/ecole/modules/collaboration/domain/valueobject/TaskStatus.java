package ecole_221.sn.ecole.modules.collaboration.domain.valueobject;

/**
 * Value Object représentant le statut d'une tâche.
 * Immutabilité garantie par le mot-clé record.
 */
public record TaskStatus(String value) {
    
    public TaskStatus {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Le statut de tâche ne peut pas être vide");
        }
        
        // Normalisation et validation
        String normalized = value.trim().toUpperCase();
        if (!isValidStatus(normalized)) {
            throw new IllegalArgumentException("Statut de tâche invalide: " + value + 
                ". Statuts autorisés: TODO, IN_PROGRESS, DONE, CANCELLED");
        }
    }
    
    private boolean isValidStatus(String status) {
        return status.equals("TODO") || 
               status.equals("IN_PROGRESS") || 
               status.equals("DONE") || 
               status.equals("CANCELLED");
    }
    
    // Statuts prédéfinis
    public static final TaskStatus TODO = new TaskStatus("TODO");
    public static final TaskStatus IN_PROGRESS = new TaskStatus("IN_PROGRESS");
    public static final TaskStatus DONE = new TaskStatus("DONE");
    public static final TaskStatus CANCELLED = new TaskStatus("CANCELLED");
    
    // Méthode de conversion String → TaskStatus
    public static TaskStatus valueOf(String status) {
        String normalized = status.trim().toUpperCase();
        return switch (normalized) {
            case "TODO" -> TODO;
            case "IN_PROGRESS" -> IN_PROGRESS;
            case "DONE" -> DONE;
            case "CANCELLED" -> CANCELLED;
            default -> throw new IllegalArgumentException("Statut de tâche invalide: " + status);
        };
    }
    
    // Méthode de conversion TaskStatus → String
    public String name() {
        return value();
    }
}