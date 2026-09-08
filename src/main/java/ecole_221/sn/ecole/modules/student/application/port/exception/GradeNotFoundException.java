package ecole_221.sn.ecole.modules.student.application.port.exception;

/**
 * Exception levée lorsque qu'une note n'est pas trouvée.
 */
public class GradeNotFoundException extends RuntimeException {
    
    private final String gradeId;
    
    public GradeNotFoundException(String gradeId) {
        super("Note non trouvée avec l'identifiant: " + gradeId);
        this.gradeId = gradeId;
    }
    
    public String getGradeId() {
        return gradeId;
    }
}
