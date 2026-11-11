package ecole_221.sn.ecole.modules.referential.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReferenceValueJpaRepository extends JpaRepository<ReferenceValueJpaEntity, String> {

    /** Valeurs plateforme + valeurs de l'école (isolation multi-tenant explicite). */
    @Query("SELECT r FROM ReferenceValueJpaEntity r WHERE r.listCode = :list "
         + "AND (r.organizationId IS NULL OR r.organizationId = :org)")
    List<ReferenceValueJpaEntity> findVisible(@Param("list") String list, @Param("org") String organizationId);

    Optional<ReferenceValueJpaEntity> findByListCodeAndCodeAndOrganizationIdIsNull(String listCode, String code);

    Optional<ReferenceValueJpaEntity> findByListCodeAndCodeAndOrganizationId(String listCode, String code,
                                                                           String organizationId);
}
