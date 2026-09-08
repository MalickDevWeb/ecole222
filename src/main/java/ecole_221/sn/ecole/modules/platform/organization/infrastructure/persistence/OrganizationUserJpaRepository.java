package ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationUserJpaRepository extends JpaRepository<OrganizationUserJpaEntity, String> {
    
    @Query("SELECT ou FROM OrganizationUserJpaEntity ou WHERE ou.userId = :userId AND ou.organizationId = :organizationId")
    Optional<OrganizationUserJpaEntity> findByUserIdAndOrganizationId(@Param("userId") String userId, @Param("organizationId") String organizationId);
    
    @Query("SELECT ou FROM OrganizationUserJpaEntity ou WHERE ou.userId = :userId")
    List<OrganizationUserJpaEntity> findByUserId(@Param("userId") String userId);
    
    @Query("SELECT ou FROM OrganizationUserJpaEntity ou WHERE ou.organizationId = :organizationId")
    List<OrganizationUserJpaEntity> findByOrganizationId(@Param("organizationId") String organizationId);
    
    @Query("SELECT ou FROM OrganizationUserJpaEntity ou WHERE ou.organizationId = :organizationId AND ou.status = :status")
    List<OrganizationUserJpaEntity> findByOrganizationIdAndStatus(@Param("organizationId") String organizationId, @Param("status") String status);
    
    @Query("SELECT ou FROM OrganizationUserJpaEntity ou WHERE ou.organizationId = :organizationId AND ou.role = :role")
    List<OrganizationUserJpaEntity> findByOrganizationIdAndRole(@Param("organizationId") String organizationId, @Param("role") String role);
    
    @Query("SELECT COUNT(ou) FROM OrganizationUserJpaEntity ou WHERE ou.organizationId = :organizationId")
    long countByOrganizationId(@Param("organizationId") String organizationId);
    
    @Query("SELECT CASE WHEN COUNT(ou) > 0 THEN true ELSE false END FROM OrganizationUserJpaEntity ou WHERE ou.userId = :userId AND ou.organizationId = :organizationId")
    boolean existsByUserIdAndOrganizationId(@Param("userId") String userId, @Param("organizationId") String organizationId);
}
