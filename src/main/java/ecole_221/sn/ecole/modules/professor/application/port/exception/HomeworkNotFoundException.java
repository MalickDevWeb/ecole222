package ecole_221.sn.ecole.modules.professor.application.port.exception;

/**
 * Exception levée lorsque qu'un devoir n'est pas trouvé.
 */
public class HomeworkNotFoundException extends RuntimeException {
    
    private final String homeworkId;
    
    public HomeworkNotFoundException(String homeworkId) {
        super("Devoir non trouvé avec l'identifiant: " + homeworkId);
        this.homeworkId = homeworkId;
    }
    
    public String getHomeworkId() {
        return homeworkId;
    }
}
