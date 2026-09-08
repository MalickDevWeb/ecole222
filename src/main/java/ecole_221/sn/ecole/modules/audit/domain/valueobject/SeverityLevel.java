package ecole_221.sn.ecole.modules.audit.domain.valueobject;

/**
 * Value Object représentant le niveau de gravité d'un événement de sécurité.
 * Immutabilité garantie par le mot-clé record.
 */
public record SeverityLevel(String value) {
    
    public SeverityLevel {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Le niveau de gravité ne peut pas être vide");
        }
        
        // Validation (la normalisation est gérée par la factory method)
        String normalized = value.trim().toUpperCase();
        if (!isValidSeverity(normalized)) {
            throw new IllegalArgumentException("Niveau de gravité invalide: " + value + 
                ". Niveaux autorisés: LOW, MEDIUM, HIGH, CRITICAL");
        }
    }
    
    /**
     * Factory method pour créer un SeverityLevel avec normalisation automatique.
     * Utilisez cette méthode au lieu du constructeur direct.
     */
    public static SeverityLevel of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Le niveau de gravité ne peut pas être vide");
        }
        String normalized = value.trim().toUpperCase();
        if (!isValidSeverity(normalized)) {
            throw new IllegalArgumentException("Niveau de gravité invalide: " + value + 
                ". Niveaux autorisés: LOW, MEDIUM, HIGH, CRITICAL");
        }
        return new SeverityLevel(normalized);
    }
    
    private static boolean isValidSeverity(String severity) {
        return severity.equals("LOW") || 
               severity.equals("MEDIUM") || 
               severity.equals("HIGH") || 
               severity.equals("CRITICAL");
    }
    
    // Niveaux prédéfinis
    public static final SeverityLevel LOW = new SeverityLevel("LOW");
    public static final SeverityLevel MEDIUM = new SeverityLevel("MEDIUM");
    public static final SeverityLevel HIGH = new SeverityLevel("HIGH");
    public static final SeverityLevel CRITICAL = new SeverityLevel("CRITICAL");
}