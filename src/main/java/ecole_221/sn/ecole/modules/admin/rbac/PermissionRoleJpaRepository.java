package ecole_221.sn.ecole.modules.admin.rbac;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PermissionRoleJpaRepository extends JpaRepository<PermissionRoleJpaEntity, String> {

    List<PermissionRoleJpaEntity> findByOrganizationId(String organizationId);

    List<PermissionRoleJpaEntity> findByOrganizationIdAndRole(String organizationId, String role);

    @Modifying
    @Query("delete from PermissionRoleJpaEntity p where p.organizationId = :organizationId and p.role = :role")
    int supprimerRole(String organizationId, String role);
}
