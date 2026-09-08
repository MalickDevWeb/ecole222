package ecole_221.sn.ecole.modules.collaboration.domain.valueobject;

public record MeetStatus(String status) {
    public static final MeetStatus SCHEDULED = new MeetStatus("SCHEDULED");
    public static final MeetStatus ACTIVE = new MeetStatus("ACTIVE");
    public static final MeetStatus ENDED = new MeetStatus("ENDED");
    public static final MeetStatus CANCELLED = new MeetStatus("CANCELLED");
    
    public MeetStatus {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Le statut de la réunion ne peut pas être vide");
        }
    }
    
    public boolean isScheduled() {
        return "SCHEDULED".equals(status);
    }
    
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
    
    public boolean isEnded() {
        return "ENDED".equals(status);
    }
    
    public boolean isCancelled() {
        return "CANCELLED".equals(status);
    }
    
    public boolean canJoin() {
        return isActive() || isScheduled();
    }
}