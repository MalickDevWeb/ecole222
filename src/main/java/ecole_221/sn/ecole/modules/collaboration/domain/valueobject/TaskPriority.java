package ecole_221.sn.ecole.modules.collaboration.domain.valueobject;

/**
 * Value Object représentant la priorité d'une tâche.
 * Immutabilité garantie par le mot-clé record.
 */
public record TaskPriority(String value) {
    
    public TaskPriority {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("La priorité de tâche ne peut pas être vide");
        }
        
        // Normalisation et validation
        String normalized = value.trim().toUpperCase();
        if (!isValidPriority(normalized)) {
            throw new IllegalArgumentException("Priorité de tâche invalide: " + value + 
                ". Priorités autorisées: LOW, MEDIUM, HIGH, URGENT");
        }
    }
    
    private boolean isValidPriority(String priority) {
        return priority.equals("LOW") || 
               priority.equals("MEDIUM") || 
               priority.equals("HIGH") || 
               priority.equals("URGENT");
    }
    
    // Priorités prédéfinies
    public static final TaskPriority LOW = new TaskPriority("LOW");
    public static final TaskPriority MEDIUM = new TaskPriority("MEDIUM");
    public static final TaskPriority HIGH = new TaskPriority("HIGH");
    public static final TaskPriority URGENT = new TaskPriority("URGENT");
    
    // Méthode de conversion String → TaskPriority
    public static TaskPriority valueOf(String priority) {
        String normalized = priority.trim().toUpperCase();
        return switch (normalized) {
            case "LOW" -> LOW;
            case "MEDIUM" -> MEDIUM;
            case "HIGH" -> HIGH;
            case "URGENT" -> URGENT;
            default -> throw new IllegalArgumentException("Priorité de tâche invalide: " + priority);
        };
    }
    
    // Méthode de conversion TaskPriority → String
    public String name() {
        return value();
    }
}