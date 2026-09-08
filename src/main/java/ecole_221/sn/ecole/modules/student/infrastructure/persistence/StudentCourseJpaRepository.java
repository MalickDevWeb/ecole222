package ecole_221.sn.ecole.modules.student.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentCourseJpaRepository extends JpaRepository<StudentCourseJpaEntity, String> {
    List<StudentCourseJpaEntity> findByStudentId(String studentId);
    List<StudentCourseJpaEntity> findByCourseId(String courseId);
    List<StudentCourseJpaEntity> findByStatut(String statut);
    List<StudentCourseJpaEntity> findBySemester(String semester);
    List<StudentCourseJpaEntity> findByStudentIdAndSemester(String studentId, String semester);
    List<StudentCourseJpaEntity> findByStudentIdAndStatut(String studentId, String statut);
}