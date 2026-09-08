package ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetJpaRepository extends JpaRepository<MeetJpaEntity, String> {
    // Les requêtes par défaut peuvent être ajoutées ici (ex: findByCourseId)
}
