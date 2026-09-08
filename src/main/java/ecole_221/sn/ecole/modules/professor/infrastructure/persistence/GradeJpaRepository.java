package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GradeJpaRepository extends JpaRepository<GradeJpaEntity, String> {
    Optional<GradeJpaEntity> findByStudentIdAndCourseId(String studentId, String courseId);
    List<GradeJpaEntity> findByCourseId(String courseId);
}
