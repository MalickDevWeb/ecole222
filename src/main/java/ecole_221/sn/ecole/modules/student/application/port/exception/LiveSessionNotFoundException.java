package ecole_221.sn.ecole.modules.student.application.port.exception;

/**
 * Exception levée lorsque qu'une session live n'est pas trouvée.
 */
public class LiveSessionNotFoundException extends RuntimeException {
    
    private final String sessionId;
    
    public LiveSessionNotFoundException(String sessionId) {
        super("Session live non trouvée avec l'identifiant: " + sessionId);
        this.sessionId = sessionId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
}
