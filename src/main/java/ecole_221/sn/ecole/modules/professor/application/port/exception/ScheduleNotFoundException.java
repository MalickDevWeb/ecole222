package ecole_221.sn.ecole.modules.professor.application.port.exception;

/**
 * Exception levée lorsque qu'une session planifiée n'est pas trouvée.
 */
public class ScheduleNotFoundException extends RuntimeException {
    
    private final String scheduleId;
    
    public ScheduleNotFoundException(String scheduleId) {
        super("Session planifiée non trouvée avec l'identifiant: " + scheduleId);
        this.scheduleId = scheduleId;
    }
    
    public String getScheduleId() {
        return scheduleId;
    }
}
