package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HomeworkJpaRepository extends JpaRepository<HomeworkJpaEntity, String> {
    Page<HomeworkJpaEntity> findByCourseId(String courseId, Pageable pageable);
    List<HomeworkJpaEntity> findByCourseId(String courseId);
    List<HomeworkJpaEntity> findByPriority(String priority);
    List<HomeworkJpaEntity> findByDeadlineBefore(String deadline);
}
