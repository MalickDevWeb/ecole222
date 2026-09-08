package ecole_221.sn.ecole.modules.platform.organization.domain.port.repository;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUser;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.MembershipStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.ScopeType;

import java.util.List;
import java.util.Optional;

/**
 * Port repository pour l'agrégat OrganizationUser.
 * Suit les principes DDD en définissant le contrat sans implémentation.
 */
public interface OrganizationUserRepositoryPort {
    
    /**
     * Sauvegarde un membership.
     */
    OrganizationUser save(OrganizationUser organizationUser);
    
    /**
     * Trouve un membership par ID.
     */
    Optional<OrganizationUser> findById(String id);
    
    /**
     * Trouve un membership par utilisateur et organization.
     */
    Optional<OrganizationUser> findByUserIdAndOrganizationId(String userId, String organizationId);
    
    /**
     * Trouve tous les memberships d'un utilisateur.
     */
    List<OrganizationUser> findByUserId(String userId);
    
    /**
     * Trouve tous les memberships d'une organization.
     */
    List<OrganizationUser> findByOrganizationId(String organizationId);
    
    /**
     * Trouve les memberships d'une organization par statut.
     */
    List<OrganizationUser> findByOrganizationIdAndStatus(String organizationId, MembershipStatus status);
    
    /**
     * Trouve les memberships d'une organization par rôle.
     */
    List<OrganizationUser> findByOrganizationIdAndRole(String organizationId, String role);
    
    /**
     * Compte le nombre de memberships d'une organization.
     */
    long countByOrganizationId(String organizationId);
    
    /**
     * Vérifie si un membership existe pour un utilisateur et une organization.
     */
    boolean existsByUserIdAndOrganizationId(String userId, String organizationId);
    
    /**
     * Supprime un membership par ID.
     */
    void deleteById(String id);
    
    /**
     * Supprime tous les memberships d'un utilisateur.
     */
    void deleteByUserId(String userId);
    
    /**
     * Supprime tous les memberships d'une organization.
     */
    void deleteByOrganizationId(String organizationId);
}
