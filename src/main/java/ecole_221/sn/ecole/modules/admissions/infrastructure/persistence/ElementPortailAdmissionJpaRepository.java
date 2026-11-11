package ecole_221.sn.ecole.modules.admissions.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Requêtes filtrées automatiquement sur l'école courante (@TenantId). */
@Repository
public interface ElementPortailAdmissionJpaRepository extends JpaRepository<ElementPortailAdmissionJpaEntity, String> {
    List<ElementPortailAdmissionJpaEntity> findByNatureOrderByPositionAsc(String nature);
}
