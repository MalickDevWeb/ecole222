package ecole_221.sn.ecole.modules.security.domain.valueobject;

public record SecurityRole(String role) {
    public static final SecurityRole VIGIL = new SecurityRole("VIGIL");
    public static final SecurityRole GUARD = new SecurityRole("GUARD");
    public static final SecurityRole SUPERVISOR = new SecurityRole("SUPERVISOR");
    public static final SecurityRole ADMIN = new SecurityRole("ADMIN");
    
    public SecurityRole {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Le rôle ne peut pas être vide");
        }
    }
    
    public boolean isVigil() {
        return "VIGIL".equals(role);
    }
    
    public boolean isGuard() {
        return "GUARD".equals(role);
    }
    
    public boolean isSupervisor() {
        return "SUPERVISOR".equals(role);
    }
    
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
    
    public boolean hasElevatedPrivileges() {
        return isSupervisor() || isAdmin();
    }
}