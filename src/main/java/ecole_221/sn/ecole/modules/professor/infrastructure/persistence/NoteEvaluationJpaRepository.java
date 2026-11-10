package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Filtré par école (@TenantId sur l'entité). */
@Repository
public interface NoteEvaluationJpaRepository extends JpaRepository<NoteEvaluationJpaEntity, String> {
    List<NoteEvaluationJpaEntity> findByEvaluationId(String evaluationId);
    List<NoteEvaluationJpaEntity> findByCourseId(String courseId);
    long countByEvaluationId(String evaluationId);
}
