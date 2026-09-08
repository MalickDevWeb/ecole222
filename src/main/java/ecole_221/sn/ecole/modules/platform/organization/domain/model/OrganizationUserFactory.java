package ecole_221.sn.ecole.modules.platform.organization.domain.model;

import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.MembershipStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.ScopeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Factory pour la création d'instances OrganizationUser.
 * Suit le pattern Factory pour garantir la création cohérente des agrégats.
 */
public class OrganizationUserFactory {
    
    /**
     * Crée un nouveau membership avec statut ACTIVE.
     */
    public static OrganizationUser create(String userId, String organizationId, String role) {
        return create(
            userId,
            organizationId,
            role,
            null, // permissions
            null, // scopeType
            null, // scopeId
            MembershipStatus.ACTIVE,
            null  // expiresAt
        );
    }
    
    /**
     * Crée un nouveau membership avec permissions.
     */
    public static OrganizationUser create(String userId, String organizationId, String role,
                                         List<String> permissions) {
        return create(
            userId,
            organizationId,
            role,
            permissions,
            null, // scopeType
            null, // scopeId
            MembershipStatus.ACTIVE,
            null  // expiresAt
        );
    }
    
    /**
     * Crée un nouveau membership avec scope.
     */
    public static OrganizationUser create(String userId, String organizationId, String role,
                                         ScopeType scopeType, String scopeId) {
        return create(
            userId,
            organizationId,
            role,
            null, // permissions
            scopeType,
            scopeId,
            MembershipStatus.ACTIVE,
            null  // expiresAt
        );
    }
    
    /**
     * Crée un nouveau membership avec tous les détails.
     */
    public static OrganizationUser create(String userId, String organizationId, String role,
                                         List<String> permissions, ScopeType scopeType, String scopeId,
                                         MembershipStatus status, LocalDateTime expiresAt) {
        
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        
        return OrganizationUser.createInternal(
            id,
            userId,
            organizationId,
            role,
            permissions,
            scopeType,
            scopeId,
            status,
            now,
            expiresAt,
            false, // isDelegated
            null   // delegationId
        );
    }
    
    /**
     * Crée un membership invité.
     */
    public static OrganizationUser createInvited(String userId, String organizationId, String role) {
        return create(
            userId,
            organizationId,
            role,
            null, // permissions
            null, // scopeType
            null, // scopeId
            MembershipStatus.INVITED,
            null  // expiresAt
        );
    }
    
    /**
     * Reconstruit un membership depuis la base de données.
     */
    public static OrganizationUser reconstruct(String id, String userId, String organizationId, String role,
                                             String[] permissions, String scopeType, String scopeId,
                                             String status, LocalDateTime joinedAt, LocalDateTime expiresAt,
                                             Boolean isDelegated, String delegationId) {
        
        return OrganizationUser.createInternal(
            id,
            userId,
            organizationId,
            role,
            permissions != null ? List.of(permissions) : null,
            scopeType != null ? ScopeType.fromString(scopeType) : null,
            scopeId,
            MembershipStatus.fromString(status),
            joinedAt,
            expiresAt,
            isDelegated,
            delegationId
        );
    }
}
