package ecole_221.sn.ecole.modules.platform.organization.domain.model;

import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.MembershipStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.ScopeType;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Agrégat représentant la relation User ↔ Organization (Membership).
 * Un utilisateur peut avoir plusieurs memberships dans différentes organizations.
 */
public class OrganizationUser {
    
    private final String id;
    private final String userId;
    private final String organizationId;
    private String role;
    private List<String> permissions;
    private ScopeType scopeType;
    private String scopeId;
    private MembershipStatus status;
    private final LocalDateTime joinedAt;
    private LocalDateTime expiresAt;
    private Boolean isDelegated;
    private String delegationId;
    
    // Constructeur privé
    private OrganizationUser(String id, String userId, String organizationId, String role, 
                           List<String> permissions, ScopeType scopeType, String scopeId,
                           MembershipStatus status, LocalDateTime joinedAt, LocalDateTime expiresAt,
                           Boolean isDelegated, String delegationId) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.userId = Objects.requireNonNull(userId, "L'ID utilisateur ne peut pas être null");
        this.organizationId = Objects.requireNonNull(organizationId, "L'ID organization ne peut pas être null");
        this.role = Objects.requireNonNull(role, "Le rôle ne peut pas être null");
        this.permissions = permissions != null ? permissions : List.of();
        this.scopeType = scopeType;
        this.scopeId = scopeId;
        this.status = Objects.requireNonNull(status, "Le statut ne peut pas être null");
        this.joinedAt = Objects.requireNonNull(joinedAt, "La date d'adhésion ne peut pas être null");
        this.expiresAt = expiresAt;
        this.isDelegated = isDelegated != null ? isDelegated : false;
        this.delegationId = delegationId;
    }
    
    // Méthode package-private pour la Factory
    static OrganizationUser createInternal(String id, String userId, String organizationId, String role,
                                        List<String> permissions, ScopeType scopeType, String scopeId,
                                        MembershipStatus status, LocalDateTime joinedAt, LocalDateTime expiresAt,
                                        Boolean isDelegated, String delegationId) {
        return new OrganizationUser(id, userId, organizationId, role, permissions, scopeType, scopeId,
                                   status, joinedAt, expiresAt, isDelegated, delegationId);
    }
    
    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getOrganizationId() { return organizationId; }
    public String getRole() { return role; }
    public List<String> getPermissions() { return permissions; }
    public ScopeType getScopeType() { return scopeType; }
    public String getScopeId() { return scopeId; }
    public MembershipStatus getStatus() { return status; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public Boolean getIsDelegated() { return isDelegated; }
    public String getDelegationId() { return delegationId; }
    
    // Setters (pour comportements métier)
    public void setRole(String role) { 
        this.role = Objects.requireNonNull(role, "Le rôle ne peut pas être null");
    }
    
    public void setPermissions(List<String> permissions) { 
        this.permissions = permissions != null ? permissions : List.of();
    }
    
    public void setScopeType(ScopeType scopeType) { 
        this.scopeType = scopeType;
    }
    
    public void setScopeId(String scopeId) { 
        this.scopeId = scopeId;
    }
    
    public void setStatus(MembershipStatus status) { 
        this.status = Objects.requireNonNull(status, "Le statut ne peut pas être null");
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) { 
        this.expiresAt = expiresAt;
    }
    
    public void setIsDelegated(Boolean isDelegated) { 
        this.isDelegated = isDelegated;
    }
    
    public void setDelegationId(String delegationId) { 
        this.delegationId = delegationId;
    }
    
    // Comportements métier
    public void activate() {
        if (MembershipStatus.ACTIVE.equals(this.status)) {
            return;
        }
        if (!MembershipStatus.INVITED.equals(this.status) && !MembershipStatus.SUSPENDED.equals(this.status)) {
            throw new IllegalStateException("Seuls les memberships INVITED ou SUSPENDED peuvent être activés");
        }
        this.status = MembershipStatus.ACTIVE;
    }
    
    public void suspend() {
        if (!MembershipStatus.ACTIVE.equals(this.status)) {
            throw new IllegalStateException("Seuls les memberships ACTIVE peuvent être suspendus");
        }
        this.status = MembershipStatus.SUSPENDED;
    }
    
    public void expire() {
        if (!MembershipStatus.ACTIVE.equals(this.status)) {
            throw new IllegalStateException("Seuls les memberships ACTIVE peuvent expirer");
        }
        this.status = MembershipStatus.EXPIRED;
    }
    
    public boolean isActive() {
        return MembershipStatus.ACTIVE.equals(this.status);
    }
    
    public boolean isExpired() {
        return MembershipStatus.EXPIRED.equals(this.status) || 
               (this.expiresAt != null && this.expiresAt.isBefore(LocalDateTime.now()));
    }
    
    public boolean hasPermission(String permission) {
        return permissions.contains(permission) || permissions.contains("*");
    }
    
    public boolean hasAnyPermission(String... requiredPermissions) {
        return Arrays.stream(requiredPermissions)
            .anyMatch(this::hasPermission);
    }
    
    public boolean hasAllPermissions(String... requiredPermissions) {
        return Arrays.stream(requiredPermissions)
            .allMatch(this::hasPermission);
    }
    
    public void addPermission(String permission) {
        if (!permissions.contains(permission)) {
            this.permissions = new java.util.ArrayList<>(this.permissions);
            this.permissions.add(permission);
        }
    }
    
    public void removePermission(String permission) {
        if (permissions.contains(permission)) {
            this.permissions = new java.util.ArrayList<>(this.permissions);
            this.permissions.remove(permission);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrganizationUser that = (OrganizationUser) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "OrganizationUser{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", organizationId='" + organizationId + '\'' +
                ", role='" + role + '\'' +
                ", status=" + status +
                ", scopeType=" + scopeType +
                '}';
    }
}
