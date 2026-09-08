package ecole_221.sn.ecole.modules.admissions.domain.exception;

/**
 * Exception métier lancée lorsqu'un candidat n'est pas trouvé.
 */
public class CandidateNotFoundException extends RuntimeException {
    
    public CandidateNotFoundException(String message) {
        super(message);
    }
}