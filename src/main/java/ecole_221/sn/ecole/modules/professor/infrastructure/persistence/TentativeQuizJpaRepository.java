package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Tentatives de quiz (filtrées par école via @TenantId). */
public interface TentativeQuizJpaRepository extends JpaRepository<TentativeQuizJpaEntity, String> {
    List<TentativeQuizJpaEntity> findByQuizIdAndStudentIdOrderByDateTentativeAsc(String quizId, String studentId);
    long countByQuizIdAndStudentId(String quizId, String studentId);
    List<TentativeQuizJpaEntity> findByQuizId(String quizId);
    long countByQuizId(String quizId);
}
