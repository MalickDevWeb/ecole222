package ecole_221.sn.ecole.modules.admin.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Une permission accordée à un rôle du personnel dans une école (V12). Permission « * » : rôle configuré. */
@Entity
@Table(name = "permissions_roles_ecole")
public class PermissionRoleJpaEntity {

    @Id
    @Column(name = "id", length = 400, nullable = false)
    private String id;

    @Column(name = "organization_id", nullable = false)
    private String organizationId;

    @Column(name = "role", length = 50, nullable = false)
    private String role;

    @Column(name = "permission", length = 100, nullable = false)
    private String permission;

    @Column(name = "modifie_le", nullable = false)
    private Instant modifieLe;

    @Column(name = "modifie_par")
    private String modifiePar;

    protected PermissionRoleJpaEntity() {
    }

    public PermissionRoleJpaEntity(String organizationId, String role, String permission, String modifiePar) {
        this.id = organizationId + "|" + role + "|" + permission;
        this.organizationId = organizationId;
        this.role = role;
        this.permission = permission;
        this.modifieLe = Instant.now();
        this.modifiePar = modifiePar;
    }

    public String getOrganizationId() { return organizationId; }
    public String getRole() { return role; }
    public String getPermission() { return permission; }
    public Instant getModifieLe() { return modifieLe; }
}
