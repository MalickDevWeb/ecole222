package ecole_221.sn.ecole.modules.student.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendanceJpaRepository extends JpaRepository<AttendanceJpaEntity, String> {
    Page<AttendanceJpaEntity> findByStudentId(String studentId, Pageable pageable);
    Page<AttendanceJpaEntity> findByCourseId(String courseId, Pageable pageable);
    
    List<AttendanceJpaEntity> findByStudentId(String studentId);
    List<AttendanceJpaEntity> findByCourseId(String courseId);
    List<AttendanceJpaEntity> findByDate(String date);
    List<AttendanceJpaEntity> findByStatus(String status);
    List<AttendanceJpaEntity> findByStudentIdAndCourseId(String studentId, String courseId);
}
