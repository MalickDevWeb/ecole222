package ecole_221.sn.ecole.modules.collaboration.domain.model;

import ecole_221.sn.ecole.modules.collaboration.domain.valueobject.Permission;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Agrégat représentant les droits d'accès d'un utilisateur à une ressource de collaboration.
 * Constructeur privé pour garantir l'invariance - utiliser AccessControlFactory.
 */
public class AccessControl {
    
    private final String id;
    private final String resourceId; // Peut être workgroupId, meetId, etc.
    private final String resourceType; // "WORKGROUP", "MEET", "CHANNEL"
    private final String userId;
    private final Set<Permission> permissions;
    private final LocalDateTime grantedAt;
    private LocalDateTime revokedAt;
    private final String grantedBy; // ID de l'utilisateur qui a accordé la permission
    
    // Constructeur package-private pour permettre aux Factories d'y accéder
    AccessControl(String id, String resourceId, String resourceType, String userId,
                        Set<Permission> permissions, LocalDateTime grantedAt, LocalDateTime revokedAt,
                        String grantedBy) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.resourceId = Objects.requireNonNull(resourceId, "L'ID de la ressource ne peut pas être null");
        this.resourceType = Objects.requireNonNull(resourceType, "Le type de ressource ne peut pas être null");
        this.userId = Objects.requireNonNull(userId, "L'ID utilisateur ne peut pas être null");
        this.permissions = new HashSet<>(Objects.requireNonNull(permissions, "Les permissions ne peuvent pas être null"));
        this.grantedAt = Objects.requireNonNull(grantedAt, "La date d'attribution ne peut pas être null");
        this.revokedAt = revokedAt;
        this.grantedBy = grantedBy;
    }
    
    // Getters
    public String getId() { return id; }
    public String getResourceId() { return resourceId; }
    public String getResourceType() { return resourceType; }
    public String getUserId() { return userId; }
    public Set<Permission> getPermissions() { return new HashSet<>(permissions); }
    public LocalDateTime getGrantedAt() { return grantedAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public String getGrantedBy() { return grantedBy; }
    
    // Comportements métier
    public boolean hasPermission(Permission permission) {
        return !isRevoked() && permissions.contains(permission);
    }
    
    public boolean hasAnyPermission(Set<Permission> requiredPermissions) {
        if (isRevoked()) return false;
        
        return permissions.stream()
                .anyMatch(requiredPermissions::contains);
    }
    
    public boolean hasAllPermissions(Set<Permission> requiredPermissions) {
        if (isRevoked()) return false;
        
        return permissions.containsAll(requiredPermissions);
    }
    
    public void grantPermission(Permission permission) {
        if (isRevoked()) {
            throw new IllegalStateException("Impossible d'ajouter une permission à un accès révoqué");
        }
        permissions.add(permission);
    }
    
    public void revokePermission(Permission permission) {
        permissions.remove(permission);
    }
    
    public void revoke() {
        this.revokedAt = LocalDateTime.now();
    }
    
    public boolean isRevoked() {
        return revokedAt != null;
    }
    
    public boolean isActive() {
        return revokedAt == null;
    }
    
    public boolean isForResource(String resourceId, String resourceType) {
        return this.resourceId.equals(resourceId) && this.resourceType.equals(resourceType);
    }
    
    public boolean isOwner() {
        return hasPermission(Permission.ADMIN);
    }
    
    public boolean canViewMessages() {
        return hasPermission(Permission.VIEW_MESSAGES) || hasPermission(Permission.READ);
    }
    
    public boolean canSendMessages() {
        return hasPermission(Permission.SEND_MESSAGES) || hasPermission(Permission.WRITE);
    }
    
    public boolean canJoinMeet() {
        return hasPermission(Permission.JOIN_MEET) || hasPermission(Permission.READ);
    }
    
    public boolean canHostMeet() {
        return hasPermission(Permission.HOST_MEET) || hasPermission(Permission.WRITE);
    }
    
    public boolean canManageParticipants() {
        return hasPermission(Permission.MANAGE_PARTICIPANTS) || hasPermission(Permission.ADMIN);
    }
    
    public boolean canDelete() {
        return hasPermission(Permission.DELETE) || hasPermission(Permission.ADMIN);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccessControl that = (AccessControl) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "AccessControl{" +
                "id='" + id + '\'' +
                ", resourceId='" + resourceId + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", userId='" + userId + '\'' +
                ", permissionCount=" + permissions.size() +
                ", active=" + isActive() +
                '}';
    }
}