package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonJpaRepository extends JpaRepository<LessonJpaEntity, String> {
    Page<LessonJpaEntity> findByCourseId(String courseId, Pageable pageable);
    List<LessonJpaEntity> findByCourseId(String courseId);
    List<LessonJpaEntity> findByDate(String date);
    List<LessonJpaEntity> findByDateBetween(String startDate, String endDate);
}
