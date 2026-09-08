package ecole_221.sn.ecole.modules.student.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentGradeJpaRepository extends JpaRepository<StudentGradeJpaEntity, String> {
    List<StudentGradeJpaEntity> findByStudentId(String studentId);
    List<StudentGradeJpaEntity> findByModule(String module);
    List<StudentGradeJpaEntity> findBySemester(String semester);
    List<StudentGradeJpaEntity> findByStudentIdAndSemester(String studentId, String semester);
}