package ecole_221.sn.ecole.shared.infrastructure.tenant;

/**
 * Exception levée lorsqu'un tenant est suspendu.
 */
public class TenantSuspendedException extends RuntimeException {
    
    public TenantSuspendedException(String message) {
        super(message);
    }
    
    public TenantSuspendedException(String message, Throwable cause) {
        super(message, cause);
    }
}
