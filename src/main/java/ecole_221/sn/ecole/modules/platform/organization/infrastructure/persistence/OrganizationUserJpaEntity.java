package ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "organization_users")
public class OrganizationUserJpaEntity {
    
    @Id
    private String id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "organization_id", nullable = false)
    private String organizationId;
    
    @Column(nullable = false)
    private String role;
    
    @Column(name = "role_id")
    private String roleId;
    
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> permissions;
    
    @Column(name = "scope_type")
    private String scopeType;
    
    @Column(name = "scope_id")
    private String scopeId;
    
    @Column(nullable = false)
    private String status;
    
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "is_delegated")
    private Boolean isDelegated = false;
    
    @Column(name = "delegation_id")
    private String delegationId;
    
    public OrganizationUserJpaEntity() {
        this.joinedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }
    
    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
    
    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    
    public String getScopeId() { return scopeId; }
    public void setScopeId(String scopeId) { this.scopeId = scopeId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public Boolean getIsDelegated() { return isDelegated; }
    public void setIsDelegated(Boolean isDelegated) { this.isDelegated = isDelegated; }
    
    public String getDelegationId() { return delegationId; }
    public void setDelegationId(String delegationId) { this.delegationId = delegationId; }
}
