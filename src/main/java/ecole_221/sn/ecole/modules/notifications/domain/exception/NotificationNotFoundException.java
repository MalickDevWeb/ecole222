package ecole_221.sn.ecole.modules.notifications.domain.exception;

/**
 * Exception métier lancée lorsqu'une notification n'est pas trouvée.
 */
public class NotificationNotFoundException extends RuntimeException {
    
    public NotificationNotFoundException(String message) {
        super(message);
    }
}