package ecole_221.sn.ecole.modules.planning.domain.exception;

/**
 * Exception métier lancée lorsqu'une classe n'est pas trouvée.
 */
public class ClasseNotFoundException extends RuntimeException {
    
    public ClasseNotFoundException(String message) {
        super(message);
    }
}