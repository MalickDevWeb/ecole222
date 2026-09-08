package ecole_221.sn.ecole.modules.audit.domain.exception;

/**
 * Exception métier lancée lorsqu'un événement de sécurité n'est pas trouvé.
 */
public class SecurityEventNotFoundException extends RuntimeException {
    
    public SecurityEventNotFoundException(String message) {
        super(message);
    }
}