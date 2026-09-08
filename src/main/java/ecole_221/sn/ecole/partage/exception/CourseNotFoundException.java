package ecole_221.sn.ecole.partage.exception;

/**
 * Exception levée lorsqu'un cours n'est pas trouvé.
 * Cette exception est partagée entre tous les modules pour assurer la cohérence.
 */
public class CourseNotFoundException extends RuntimeException {
    
    private final String courseId;
    
    public CourseNotFoundException(String courseId) {
        super("Cours non trouvé avec l'identifiant: " + courseId);
        this.courseId = courseId;
    }
    
    public String getCourseId() {
        return courseId;
    }
}