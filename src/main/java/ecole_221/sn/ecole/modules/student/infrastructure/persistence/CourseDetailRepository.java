package ecole_221.sn.ecole.modules.student.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseDetailRepository extends JpaRepository<CourseDetail, String> {
}
