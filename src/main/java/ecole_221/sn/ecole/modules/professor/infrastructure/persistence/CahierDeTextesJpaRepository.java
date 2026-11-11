package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Filtré par école (@TenantId sur l'entité). */
public interface CahierDeTextesJpaRepository extends JpaRepository<CahierDeTextesJpaEntity, String> {
    List<CahierDeTextesJpaEntity> findByCourseIdOrderByDateDescCreatedAtDesc(String courseId);
    boolean existsBySessionIdAndDate(String sessionId, String date);
}
