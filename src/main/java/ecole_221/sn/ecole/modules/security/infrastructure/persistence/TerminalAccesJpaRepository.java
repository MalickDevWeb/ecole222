package ecole_221.sn.ecole.modules.security.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TerminalAccesJpaRepository extends JpaRepository<TerminalAccesJpaEntity, String> {
    List<TerminalAccesJpaEntity> findAllByOrderByCreeLeAsc();
    boolean existsByNomIgnoreCase(String nom);
    List<TerminalAccesJpaEntity> findByVigileId(String vigileId);
    Optional<TerminalAccesJpaEntity> findFirstByVigileIdAndStatutNot(String vigileId, String statut);
}
