package ecole_221.sn.ecole.modules.planning.domain.exception;

/**
 * Exception métier lancée lorsqu'une filière n'est pas trouvée.
 */
public class FiliereNotFoundException extends RuntimeException {
    
    public FiliereNotFoundException(String message) {
        super(message);
    }
}