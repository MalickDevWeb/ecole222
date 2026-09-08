package ecole_221.sn.ecole.modules.finance.domain.valueobject;

/**
 * Value Object représentant le statut d'une dépense.
 * Immutabilité garantie par le mot-clé record.
 */
public record ExpenseStatus(String value) {
    
    public ExpenseStatus {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Le statut ne peut pas être vide");
        }
        
        // Validation des statuts autorisés
        if (!isValidStatus(value)) {
            throw new IllegalArgumentException("Statut invalide: " + value + 
                ". Statuts autorisés: Payé, En Attente, Annulé");
        }
    }
    
    private boolean isValidStatus(String status) {
        return status.equals("Payé") || 
               status.equals("En Attente") || 
               status.equals("Annulé");
    }
    
    // Statuts prédéfinis
    public static final ExpenseStatus PAYE = new ExpenseStatus("Payé");
    public static final ExpenseStatus EN_ATTENTE = new ExpenseStatus("En Attente");
    public static final ExpenseStatus ANNULE = new ExpenseStatus("Annulé");
}