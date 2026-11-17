package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Filtré par école (@TenantId sur l'entité). */
@Repository
public interface EvaluationJpaRepository extends JpaRepository<EvaluationJpaEntity, String> {
    List<EvaluationJpaEntity> findByCourseIdOrderByDateEvaluationAscCreeLeAsc(String courseId);
    boolean existsByEvaluationRattrapeeId(String evaluationRattrapeeId);
}
