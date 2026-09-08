package ecole_221.sn.ecole.modules.platform.organization.domain.port.repository;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;

import java.util.List;
import java.util.Optional;

/**
 * Port repository pour l'agrégat Organization.
 * Suit les principes DDD en définissant le contrat sans implémentation.
 */
public interface OrganizationRepositoryPort {
    
    /**
     * Sauvegarde une organization.
     */
    Organization save(Organization organization);
    
    /**
     * Trouve une organization par ID.
     */
    Optional<Organization> findById(String id);
    
    /**
     * Trouve une organization par son email.
     */
    Optional<Organization> findByEmail(String email);
    
    /**
     * Trouve une organization par subdomain.
     */
    Optional<Organization> findBySubdomain(String subdomain);
    
    /**
     * Trouve une organization par custom domain.
     */
    Optional<Organization> findByCustomDomain(String customDomain);
    
    /**
     * Trouve toutes les organizations par statut.
     */
    List<Organization> findByStatus(OrganizationStatus status);
    
    /**
     * Trouve toutes les organizations par type.
     */
    List<Organization> findByType(OrganizationType type);
    
    /**
     * Trouve toutes les organizations.
     */
    List<Organization> findAll();
    
    /**
     * Supprime une organization par ID.
     */
    void deleteById(String id);
    
    /**
     * Compte le nombre d'organizations par statut.
     */
    long countByStatus(OrganizationStatus status);
    
    /**
     * Vérifie si une organization existe avec cet email.
     */
    boolean existsByEmail(String email);
}
