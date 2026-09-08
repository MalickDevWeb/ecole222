package ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "role_permissions")
public class RolePermissionJpaEntity {

    @Id
    private String id;
    
    @Column(name = "role_id", nullable = false)
    private String roleId;
    
    @Column(name = "permission_id", nullable = false)
    private String permissionId;
    
    @Column(columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> conditions;

    public RolePermissionJpaEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }

    public String getPermissionId() { return permissionId; }
    public void setPermissionId(String permissionId) { this.permissionId = permissionId; }

    public Map<String, Object> getConditions() { return conditions; }
    public void setConditions(Map<String, Object> conditions) { this.conditions = conditions; }
}

