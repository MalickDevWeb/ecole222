package ecole_221.sn.ecole.modules.admin.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Limité à l'école courante par @TenantId. */
public interface ObservationEleveJpaRepository extends JpaRepository<ObservationEleveJpaEntity, String> {
    List<ObservationEleveJpaEntity> findByEleveIdOrderByCreeLeDesc(String eleveId);
}
