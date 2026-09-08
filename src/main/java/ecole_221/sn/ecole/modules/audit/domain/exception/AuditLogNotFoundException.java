package ecole_221.sn.ecole.modules.audit.domain.exception;

/**
 * Exception métier lancée lorsqu'un log d'audit n'est pas trouvé.
 */
public class AuditLogNotFoundException extends RuntimeException {
    
    public AuditLogNotFoundException(String message) {
        super(message);
    }
}