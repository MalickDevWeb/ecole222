package ecole_221.sn.ecole.modules.auth.domain.valueobject;

import java.util.Objects;

/**
 * Value Object représentant le profil (rôle) d'un utilisateur dans un établissement spécifique.
 */
public class TenantProfile {
    
    private final String tenantId;
    private final RoleUtilisateur role;
    
    public TenantProfile(String tenantId, RoleUtilisateur role) {
        this.tenantId = Objects.requireNonNull(tenantId, "L'ID du tenant ne peut pas être null");
        this.role = Objects.requireNonNull(role, "Le rôle ne peut pas être null");
    }

    public String getTenantId() {
        return tenantId;
    }

    public RoleUtilisateur getRole() {
        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TenantProfile that = (TenantProfile) o;
        return Objects.equals(tenantId, that.tenantId) && role == that.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, role);
    }
}
