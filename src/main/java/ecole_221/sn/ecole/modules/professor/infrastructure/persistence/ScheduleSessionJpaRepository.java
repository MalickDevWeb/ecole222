package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleSessionJpaRepository extends JpaRepository<ScheduleSessionJpaEntity, String> {
    List<ScheduleSessionJpaEntity> findByProfessorId(String professorId);
    List<ScheduleSessionJpaEntity> findByProfessorIdAndStatus(String professorId, String status);
    List<ScheduleSessionJpaEntity> findByCourseId(String courseId);
}