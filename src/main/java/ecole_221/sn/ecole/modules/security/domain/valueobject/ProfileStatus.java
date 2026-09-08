package ecole_221.sn.ecole.modules.security.domain.valueobject;

public record ProfileStatus(String status) {
    public static final ProfileStatus ACTIVE = new ProfileStatus("ACTIVE");
    public static final ProfileStatus INACTIVE = new ProfileStatus("INACTIVE");
    public static final ProfileStatus SUSPENDED = new ProfileStatus("SUSPENDED");
    
    public ProfileStatus {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Le statut ne peut pas être vide");
        }
    }
    
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
    
    public boolean isInactive() {
        return "INACTIVE".equals(status);
    }
    
    public boolean isSuspended() {
        return "SUSPENDED".equals(status);
    }
}