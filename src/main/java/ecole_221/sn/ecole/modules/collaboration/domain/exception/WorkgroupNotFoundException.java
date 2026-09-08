package ecole_221.sn.ecole.modules.collaboration.domain.exception;

/**
 * Exception métier lancée lorsqu'un groupe de travail n'est pas trouvé.
 */
public class WorkgroupNotFoundException extends RuntimeException {
    
    public WorkgroupNotFoundException(String message) {
        super(message);
    }
}