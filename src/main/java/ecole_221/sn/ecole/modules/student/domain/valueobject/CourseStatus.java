package ecole_221.sn.ecole.modules.student.domain.valueobject;

/**
 * Value Object représentant le statut d'un cours.
 * Immutabilité garantie par le mot-clé record.
 */
public record CourseStatus(String value) {
    
    public CourseStatus {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Le statut du cours ne peut pas être vide");
        }
        
        // Normalisation et validation
        String normalized = value.trim().toUpperCase();
        if (!isValidStatus(normalized)) {
            throw new IllegalArgumentException("Statut du cours invalide: " + value + 
                ". Statuts autorisés: EN_COURS, TERMINE, A_COMMENCER, ABANDONNE");
        }
    }
    
    private boolean isValidStatus(String status) {
        return status.equals("EN_COURS") || 
               status.equals("TERMINE") || 
               status.equals("A_COMMENCER") || 
               status.equals("ABANDONNE");
    }
    
    // Statuts prédéfinis
    public static final CourseStatus EN_COURS = new CourseStatus("EN_COURS");
    public static final CourseStatus TERMINE = new CourseStatus("TERMINE");
    public static final CourseStatus A_COMMENCER = new CourseStatus("A_COMMENCER");
    public static final CourseStatus ABANDONNE = new CourseStatus("ABANDONNE");

    public static CourseStatus valueOf(String value) {
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "EN_COURS" -> EN_COURS;
            case "TERMINE" -> TERMINE;
            case "A_COMMENCER" -> A_COMMENCER;
            case "ABANDONNE" -> ABANDONNE;
            default -> throw new IllegalArgumentException("Valeur invalide: " + value);
        };
    }

    public String name() {
        return value();
    }
}