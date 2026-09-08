package ecole_221.sn.ecole.modules.student.domain.exception;

/**
 * Exception métier lancée lorsqu'une présence n'est pas trouvée.
 */
public class AttendanceNotFoundException extends RuntimeException {
    
    public AttendanceNotFoundException(String message) {
        super(message);
    }
}