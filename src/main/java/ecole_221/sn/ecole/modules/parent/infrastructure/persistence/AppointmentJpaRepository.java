package ecole_221.sn.ecole.modules.parent.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AppointmentJpaRepository extends JpaRepository<AppointmentJpaEntity, String> {
    List<AppointmentJpaEntity> findByParentIdOrderByCreatedAtDesc(String parentId);
    List<AppointmentJpaEntity> findByProfesseurIdOrderByCreatedAtDesc(String professeurId);
    List<AppointmentJpaEntity> findAllByOrderByCreatedAtDesc();
}

