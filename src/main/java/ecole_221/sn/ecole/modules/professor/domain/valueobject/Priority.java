package ecole_221.sn.ecole.modules.professor.domain.valueobject;

/**
 * Value Object représentant le niveau de priorité.
 * Immutabilité garantie par le mot-clé record.
 */
public record Priority(String value) {
    
    public Priority {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Le niveau de priorité ne peut pas être vide");
        }
        
        // Normalisation et validation
        String normalized = value.trim().toUpperCase();
        if (!isValidPriority(normalized)) {
            throw new IllegalArgumentException("Niveau de priorité invalide: " + value + 
                ". Niveaux autorisés: LOW, MEDIUM, HIGH, URGENT");
        }
    }
    
    private boolean isValidPriority(String priority) {
        return priority.equals("LOW") || 
               priority.equals("MEDIUM") || 
               priority.equals("HIGH") || 
               priority.equals("URGENT");
    }
    
    // Niveaux prédéfinis
    public static final Priority LOW = new Priority("LOW");
    public static final Priority MEDIUM = new Priority("MEDIUM");
    public static final Priority HIGH = new Priority("HIGH");
    public static final Priority URGENT = new Priority("URGENT");
    
    // Méthode de conversion String → Priority
    public static Priority valueOf(String priority) {
        String normalized = priority.trim().toUpperCase();
        return switch (normalized) {
            case "LOW" -> LOW;
            case "MEDIUM" -> MEDIUM;
            case "HIGH" -> HIGH;
            case "URGENT" -> URGENT;
            default -> throw new IllegalArgumentException("Niveau de priorité invalide: " + priority);
        };
    }
    
    // Méthode de conversion Priority → String
    public String name() {
        return value();
    }
}