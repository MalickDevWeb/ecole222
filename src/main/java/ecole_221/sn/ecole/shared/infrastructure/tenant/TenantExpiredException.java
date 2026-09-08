package ecole_221.sn.ecole.shared.infrastructure.tenant;

/**
 * Exception levée lorsqu'un tenant a expiré.
 */
public class TenantExpiredException extends RuntimeException {
    
    public TenantExpiredException(String message) {
        super(message);
    }
    
    public TenantExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
