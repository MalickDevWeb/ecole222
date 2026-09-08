package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReminderJpaRepository extends JpaRepository<ReminderJpaEntity, String> {
    List<ReminderJpaEntity> findByCourseId(String courseId);
    List<ReminderJpaEntity> findByProfessorId(String professorId);
    List<ReminderJpaEntity> findByProfessorIdAndIsCompletedFalse(String professorId);
}