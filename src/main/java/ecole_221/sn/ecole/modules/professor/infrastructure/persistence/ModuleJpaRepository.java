package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuleJpaRepository extends JpaRepository<ModuleJpaEntity, String> {
    Page<ModuleJpaEntity> findByCourseId(String courseId, Pageable pageable);
}
