package ecole_221.sn.ecole.modules.student.domain.exception;

/**
 * Exception métier lancée lorsqu'une note d'étudiant n'est pas trouvée.
 */
public class StudentGradeNotFoundException extends RuntimeException {
    
    public StudentGradeNotFoundException(String message) {
        super(message);
    }
}