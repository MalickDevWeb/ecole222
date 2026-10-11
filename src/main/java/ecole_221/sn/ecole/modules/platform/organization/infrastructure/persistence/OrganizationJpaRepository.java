package ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationJpaRepository extends JpaRepository<OrganizationJpaEntity, String> {
    
    @Query("SELECT o FROM OrganizationJpaEntity o WHERE o.email = :email")
    Optional<OrganizationJpaEntity> findByEmail(@Param("email") String email);
    
    @Query("SELECT o FROM OrganizationJpaEntity o WHERE o.status = :status")
    List<OrganizationJpaEntity> findByStatus(@Param("status") OrganizationStatusJpa status);
    
    @Query("SELECT o FROM OrganizationJpaEntity o WHERE o.type = :type")
    List<OrganizationJpaEntity> findByType(@Param("type") OrganizationTypeJpa type);
    
    @Query("SELECT COUNT(o) FROM OrganizationJpaEntity o WHERE o.status = :status")
    long countByStatus(@Param("status") OrganizationStatusJpa status);
    
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM OrganizationJpaEntity o WHERE o.email = :email")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT o FROM OrganizationJpaEntity o WHERE LOWER(o.slug) = LOWER(:slug)")
    Optional<OrganizationJpaEntity> findBySlug(@Param("slug") String slug);

    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM OrganizationJpaEntity o WHERE LOWER(o.slug) = LOWER(:slug)")
    boolean existsBySlug(@Param("slug") String slug);

    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM OrganizationJpaEntity o WHERE o.phone = :phone")
    boolean existsByPhone(@Param("phone") String phone);
    
    // Pour la résolution par subdomain/custom domain (à implémenter avec la table domains)
    // @Query("SELECT o FROM OrganizationJpaEntity o JOIN Domain d ON o.primaryDomainId = d.id WHERE d.name = :domainName")
    // Optional<OrganizationJpaEntity> findByDomainName(@Param("domainName") String domainName);
}
