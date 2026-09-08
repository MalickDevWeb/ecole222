package ecole_221.sn.ecole.modules.auth.domain.exception;

/**
 * Exception métier lancée lorsqu'un utilisateur n'est pas trouvé.
 */
public class UserNotFoundException extends RuntimeException {
    
    public UserNotFoundException(String message) {
        super(message);
    }
    
    public UserNotFoundException(String email, Throwable cause) {
        super("Utilisateur non trouvé avec l'email: " + email, cause);
    }
}