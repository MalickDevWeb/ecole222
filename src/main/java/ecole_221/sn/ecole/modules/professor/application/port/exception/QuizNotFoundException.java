package ecole_221.sn.ecole.modules.professor.application.port.exception;

/**
 * Exception levée lorsque qu'un quiz n'est pas trouvé.
 */
public class QuizNotFoundException extends RuntimeException {
    
    private final String quizId;
    
    public QuizNotFoundException(String quizId) {
        super("Quiz non trouvé avec l'identifiant: " + quizId);
        this.quizId = quizId;
    }
    
    public String getQuizId() {
        return quizId;
    }
}
