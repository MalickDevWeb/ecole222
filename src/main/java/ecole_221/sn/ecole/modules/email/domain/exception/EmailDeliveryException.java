package ecole_221.sn.ecole.modules.email.domain.exception;

/**
 * Exception métier lancée lorsqu'un échec d'envoi d'email survient.
 */
public class EmailDeliveryException extends RuntimeException {
    
    public EmailDeliveryException(String message) {
        super(message);
    }
    
    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public EmailDeliveryException(String recipient, String subject) {
        super("Échec de l'envoi d'email à " + recipient + " avec sujet: " + subject);
    }
}