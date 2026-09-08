package ecole_221.sn.ecole.modules.sync.domain.exception;

/**
 * Exception métier lancée lorsqu'une action de synchronisation n'est pas trouvée.
 */
public class SyncActionNotFoundException extends RuntimeException {
    
    public SyncActionNotFoundException(String message) {
        super(message);
    }
}