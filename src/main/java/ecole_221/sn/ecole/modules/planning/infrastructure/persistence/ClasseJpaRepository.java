package ecole_221.sn.ecole.modules.planning.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClasseJpaRepository extends JpaRepository<ClasseJpaEntity, String> {
    /**
     * Récupère les classes par filière.
     */
    List<ClasseJpaEntity> findByFiliereId(String filiereId);
    
    /**
     * Récupère les classes par niveau.
     */
    List<ClasseJpaEntity> findByLevel(String level);
}
