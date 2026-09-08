package ecole_221.sn.ecole.shared.infrastructure.tenant;

/**
 * Exception levée lorsqu'un tenant n'est pas trouvé.
 */
public class TenantNotFoundException extends RuntimeException {
    
    public TenantNotFoundException(String message) {
        super(message);
    }
    
    public TenantNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
