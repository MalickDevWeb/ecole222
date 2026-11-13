package ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ElementCollaboratifJpaRepository extends JpaRepository<ElementCollaboratifJpaEntity, String> {
    List<ElementCollaboratifJpaEntity> findByGenreOrderByCreeLeAsc(String genre);
    List<ElementCollaboratifJpaEntity> findByGenreAndGroupeIdOrderByCreeLeAsc(String genre, String groupeId);
    Optional<ElementCollaboratifJpaEntity> findByGenreAndCle(String genre, String cle);
}
