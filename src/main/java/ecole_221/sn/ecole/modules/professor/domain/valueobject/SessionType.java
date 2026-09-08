package ecole_221.sn.ecole.modules.professor.domain.valueobject;

/**
 * Value Object représentant le type d'une session de cours.
 * Immutabilité garantie par le mot-clé record.
 */
public record SessionType(String value) {
    
    public SessionType {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Le type de session ne peut pas être vide");
        }
        
        // Normalisation et validation
        String normalized = value.trim().toUpperCase();
        if (!isValidType(normalized)) {
            throw new IllegalArgumentException("Type de session invalide: " + value + 
                ". Types autorisés: CM, TD, TP, EXAM, PROJECT");
        }
    }
    
    private boolean isValidType(String type) {
        return type.equals("CM") || 
               type.equals("TD") || 
               type.equals("TP") || 
               type.equals("EXAM") || 
               type.equals("PROJECT");
    }
    
    // Types prédéfinis
    public static final SessionType CM = new SessionType("CM");
    public static final SessionType TD = new SessionType("TD");
    public static final SessionType TP = new SessionType("TP");
    public static final SessionType EXAM = new SessionType("EXAM");
    public static final SessionType PROJECT = new SessionType("PROJECT");
    
    // Méthode de conversion String → SessionType
    public static SessionType valueOf(String type) {
        String normalized = type.trim().toUpperCase();
        return switch (normalized) {
            case "CM" -> CM;
            case "TD" -> TD;
            case "TP" -> TP;
            case "EXAM" -> EXAM;
            case "PROJECT" -> PROJECT;
            default -> throw new IllegalArgumentException("Type de session invalide: " + type);
        };
    }
    
    // Méthode de conversion SessionType → String
    public String name() {
        return value();
    }
}