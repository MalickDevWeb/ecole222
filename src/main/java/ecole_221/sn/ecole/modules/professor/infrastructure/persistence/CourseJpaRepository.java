package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseJpaRepository extends JpaRepository<CourseJpaEntity, String> {
    Page<CourseJpaEntity> findByProfId(String profId, Pageable pageable);
    List<CourseJpaEntity> findByProfId(String profId);
    List<CourseJpaEntity> findByClasse(String classe);
    Optional<CourseJpaEntity> findByCode(String code);
}
