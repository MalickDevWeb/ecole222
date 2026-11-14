package ecole_221.sn.ecole.modules.admin.periodes;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PeriodeScolaireJpaRepository extends JpaRepository<PeriodeScolaireJpaEntity, String> {

    List<PeriodeScolaireJpaEntity> findAllByOrderByDateDebutAsc();
}
