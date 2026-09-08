package ecole_221.sn.ecole.modules.collaboration.domain.exception;

/**
 * Exception métier lancée lorsqu'une tâche n'est pas trouvée.
 */
public class TaskNotFoundException extends RuntimeException {
    
    public TaskNotFoundException(String message) {
        super(message);
    }
}