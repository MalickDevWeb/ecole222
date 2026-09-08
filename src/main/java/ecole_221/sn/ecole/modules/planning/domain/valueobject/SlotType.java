package ecole_221.sn.ecole.modules.planning.domain.valueobject;

/**
 * Value Object représentant le type de créneau horaire.
 * Immutabilité garantie par le mot-clé record.
 */
public record SlotType(String value) {
    
    public SlotType {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Le type de créneau ne peut pas être vide");
        }
        
        // Normalisation et validation
        String normalized = value.trim().toUpperCase();
        if (!isValidType(normalized)) {
            throw new IllegalArgumentException("Type de créneau invalide: " + value + 
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
    public static final SlotType CM = new SlotType("CM");
    public static final SlotType TD = new SlotType("TD");
    public static final SlotType TP = new SlotType("TP");
    public static final SlotType EXAM = new SlotType("EXAM");
    public static final SlotType PROJECT = new SlotType("PROJECT");
}