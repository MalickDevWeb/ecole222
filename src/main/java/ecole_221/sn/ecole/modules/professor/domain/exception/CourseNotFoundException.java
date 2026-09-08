package ecole_221.sn.ecole.modules.professor.domain.exception;

/**
 * Exception métier lancée lorsqu'un cours n'est pas trouvé.
 */
public class CourseNotFoundException extends RuntimeException {
    
    public CourseNotFoundException(String message) {
        super(message);
    }
}