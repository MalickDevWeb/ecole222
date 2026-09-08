package ecole_221.sn.ecole.modules.finance.domain.exception;

/**
 * Exception métier lancée lorsqu'une dépense n'est pas trouvée.
 */
public class ExpenseNotFoundException extends RuntimeException {
    
    public ExpenseNotFoundException(String message) {
        super(message);
    }
    
    public ExpenseNotFoundException(String id, Throwable cause) {
        super("Dépense non trouvée avec l'ID: " + id, cause);
    }
}