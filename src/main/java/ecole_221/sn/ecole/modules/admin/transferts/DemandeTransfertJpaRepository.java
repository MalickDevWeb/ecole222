package ecole_221.sn.ecole.modules.admin.transferts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemandeTransfertJpaRepository extends JpaRepository<DemandeTransfertJpaEntity, String> {

    List<DemandeTransfertJpaEntity> findByEcoleSourceIdOrderByCreeLeDesc(String ecoleSourceId);

    List<DemandeTransfertJpaEntity> findByEcoleDestinationIdOrderByCreeLeDesc(String ecoleDestinationId);

    boolean existsByEleveIdAndStatut(String eleveId, String statut);
}
