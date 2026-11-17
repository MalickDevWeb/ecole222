package ecole_221.sn.ecole.modules.admin.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointagePersonnelJpaRepository extends JpaRepository<PointagePersonnelJpaEntity, String> {
    List<PointagePersonnelJpaEntity> findAllByOrderByCreatedAtDesc();
}
