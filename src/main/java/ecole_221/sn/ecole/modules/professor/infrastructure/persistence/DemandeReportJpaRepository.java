package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Filtré par école (@TenantId sur l'entité). */
public interface DemandeReportJpaRepository extends JpaRepository<DemandeReportJpaEntity, String> {
    List<DemandeReportJpaEntity> findByProfessorIdOrderByCreatedAtDesc(String professorId);
    List<DemandeReportJpaEntity> findAllByOrderByCreatedAtDesc();
    List<DemandeReportJpaEntity> findByStatutOrderByCreatedAtDesc(String statut);
    boolean existsBySessionIdAndStatut(String sessionId, String statut);
}
