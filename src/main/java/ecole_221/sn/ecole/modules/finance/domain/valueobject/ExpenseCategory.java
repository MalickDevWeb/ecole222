package ecole_221.sn.ecole.modules.finance.domain.valueobject;

/**
 * Value Object représentant la catégorie d'une dépense.
 * Immutabilité garantie par le mot-clé record.
 */
public record ExpenseCategory(String value) {
    
    public ExpenseCategory {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("La catégorie ne peut pas être vide");
        }
        
        // Validation des catégories autorisées
        if (!isValidCategory(value)) {
            throw new IllegalArgumentException("Catégorie invalide: " + value + 
                ". Catégories autorisées: RH, Loyer/Charges, Matériel, Autre");
        }
    }
    
    private boolean isValidCategory(String category) {
        return category.equals("RH") || 
               category.equals("Loyer/Charges") || 
               category.equals("Matériel") || 
               category.equals("Autre");
    }
    
    // Catégories prédéfinies
    public static final ExpenseCategory RH = new ExpenseCategory("RH");
    public static final ExpenseCategory LOYER_CHARGES = new ExpenseCategory("Loyer/Charges");
    public static final ExpenseCategory MATERIEL = new ExpenseCategory("Matériel");
    public static final ExpenseCategory AUTRE = new ExpenseCategory("Autre");
}