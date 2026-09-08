package ecole_221.sn.ecole.modules.collaboration.domain.valueobject;

public record Permission(String permission) {
    // Permissions de base
    public static final Permission READ = new Permission("READ");
    public static final Permission WRITE = new Permission("WRITE");
    public static final Permission DELETE = new Permission("DELETE");
    public static final Permission ADMIN = new Permission("ADMIN");
    
    // Permissions spécifiques à la collaboration
    public static final Permission VIEW_MESSAGES = new Permission("VIEW_MESSAGES");
    public static final Permission SEND_MESSAGES = new Permission("SEND_MESSAGES");
    public static final Permission JOIN_MEET = new Permission("JOIN_MEET");
    public static final Permission HOST_MEET = new Permission("HOST_MEET");
    public static final Permission MANAGE_PARTICIPANTS = new Permission("MANAGE_PARTICIPANTS");
    public static final Permission SHARE_SCREEN = new Permission("SHARE_SCREEN");
    public static final Permission RECORD_MEET = new Permission("RECORD_MEET");
    
    public Permission {
        if (permission == null || permission.isBlank()) {
            throw new IllegalArgumentException("La permission ne peut pas être vide");
        }
    }
    
    public boolean isRead() {
        return "READ".equals(permission);
    }
    
    public boolean isWrite() {
        return "WRITE".equals(permission);
    }
    
    public boolean isDelete() {
        return "DELETE".equals(permission);
    }
    
    public boolean isAdmin() {
        return "ADMIN".equals(permission);
    }
    
    public boolean canViewMessages() {
        return "VIEW_MESSAGES".equals(permission) || isAdmin();
    }
    
    public boolean canSendMessages() {
        return "SEND_MESSAGES".equals(permission) || isWrite() || isAdmin();
    }
    
    public boolean canJoinMeet() {
        return "JOIN_MEET".equals(permission) || isRead() || isAdmin();
    }
    
    public boolean canHostMeet() {
        return "HOST_MEET".equals(permission) || isWrite() || isAdmin();
    }
    
    public boolean canManageParticipants() {
        return "MANAGE_PARTICIPANTS".equals(permission) || isAdmin();
    }
    
    public boolean canShareScreen() {
        return "SHARE_SCREEN".equals(permission) || isAdmin();
    }
    
    public boolean canRecordMeet() {
        return "RECORD_MEET".equals(permission) || isAdmin();
    }
}