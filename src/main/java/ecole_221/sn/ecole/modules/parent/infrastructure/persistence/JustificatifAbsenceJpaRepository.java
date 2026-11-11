package ecole_221.sn.ecole.modules.parent.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Lectures filtrées par école (@TenantId de l'entité). */
public interface JustificatifAbsenceJpaRepository extends JpaRepository<JustificatifAbsenceJpaEntity, String> {
    List<JustificatifAbsenceJpaEntity> findByPresenceId(String presenceId);
    List<JustificatifAbsenceJpaEntity> findByEleveId(String eleveId);
    List<JustificatifAbsenceJpaEntity> findByParentIdOrderByDeposeLeDesc(String parentId);
    List<JustificatifAbsenceJpaEntity> findAllByOrderByDeposeLeDesc();
    List<JustificatifAbsenceJpaEntity> findByStatutOrderByDeposeLeDesc(String statut);
}
