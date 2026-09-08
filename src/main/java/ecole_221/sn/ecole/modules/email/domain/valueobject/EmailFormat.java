package ecole_221.sn.ecole.modules.email.domain.valueobject;

/**
 * Value Object représentant le format d'email.
 * Immutabilité garantie par le mot-clé record.
 */
public record EmailFormat(String value) {
    
    public EmailFormat {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Le format d'email ne peut pas être vide");
        }
        
        // Normalisation et validation
        String normalized = value.trim().toUpperCase();
        if (!isValidFormat(normalized)) {
            throw new IllegalArgumentException("Format d'email invalide: " + value + 
                ". Formats autorisés: TEXT, HTML");
        }
    }
    
    private boolean isValidFormat(String format) {
        return format.equals("TEXT") || 
               format.equals("HTML");
    }
    
    // Formats prédéfinis
    public static final EmailFormat TEXT = new EmailFormat("TEXT");
    public static final EmailFormat HTML = new EmailFormat("HTML");
}