package ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MembreGroupeJpaRepository extends JpaRepository<MembreGroupeJpaEntity, String> {
    List<MembreGroupeJpaEntity> findByGroupeIdOrderByAjouteLeAsc(String groupeId);
    List<MembreGroupeJpaEntity> findAllByOrderByAjouteLeAsc();
}
