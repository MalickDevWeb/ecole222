package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Filtré par école (@TenantId sur l'entité). */
public interface RenduDevoirJpaRepository extends JpaRepository<RenduDevoirJpaEntity, String> {
    List<RenduDevoirJpaEntity> findByHomeworkId(String homeworkId);
    Optional<RenduDevoirJpaEntity> findByHomeworkIdAndStudentId(String homeworkId, String studentId);
    List<RenduDevoirJpaEntity> findByStudentId(String studentId);
}
