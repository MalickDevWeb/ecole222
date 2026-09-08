package ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkgroupJpaRepository extends JpaRepository<WorkgroupJpaEntity, String> {
    // Les requêtes par défaut peuvent être ajoutées ici (ex: findByCourseId)
}
