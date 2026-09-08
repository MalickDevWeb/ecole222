package ecole_221.sn.ecole.modules.student.application.port.exception;

/**
 * Exception levée lorsque qu'un étudiant n'est pas trouvé.
 */
public class StudentNotFoundException extends RuntimeException {
    
    private final String studentId;
    
    public StudentNotFoundException(String studentId) {
        super("Étudiant non trouvé avec l'identifiant: " + studentId);
        this.studentId = studentId;
    }
    
    public String getStudentId() {
        return studentId;
    }
}
