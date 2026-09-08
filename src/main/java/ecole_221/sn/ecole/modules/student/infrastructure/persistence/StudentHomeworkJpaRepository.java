package ecole_221.sn.ecole.modules.student.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentHomeworkJpaRepository extends JpaRepository<StudentHomeworkJpaEntity, String> {
    List<StudentHomeworkJpaEntity> findByStudentId(String studentId);
    List<StudentHomeworkJpaEntity> findByStudentIdAndStatut(String studentId, String statut);
    List<StudentHomeworkJpaEntity> findByCourseId(String courseId);
}