package ecole_221.sn.ecole.modules.superadmin.domain.valueobject;

/**
 * Value Object représentant le rôle d'un utilisateur global.
 * N'accepte que les rôles de gestion globale (SUPER_ADMIN, PLATFORM_ADMIN, ADMIN).
 */
public record GlobalUserRole(String value) {

    public GlobalUserRole {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Le rôle ne peut pas être vide");
        }
        
        String normalized = value.trim().toUpperCase();
        if (!isValidGlobalRole(normalized)) {
            throw new IllegalArgumentException(
                "Rôle global invalide: " + value + 
                ". Rôles autorisés pour ce module: SUPER_ADMIN, PLATFORM_ADMIN, ADMIN"
            );
        }
        
        value = normalized;
    }

    private boolean isValidGlobalRole(String role) {
        return role.equals("SUPER_ADMIN") || 
               role.equals("PLATFORM_ADMIN") || 
               role.equals("ADMIN") ||
               role.equals("ADMINISTRATOR");
    }
}
