package ecole_221.sn.ecole.modules.professor.application.port.exception;

/**
 * Exception levée lorsque qu'une leçon n'est pas trouvée.
 */
public class LessonNotFoundException extends RuntimeException {
    
    private final String lessonId;
    
    public LessonNotFoundException(String lessonId) {
        super("Leçon non trouvée avec l'identifiant: " + lessonId);
        this.lessonId = lessonId;
    }
    
    public String getLessonId() {
        return lessonId;
    }
}
