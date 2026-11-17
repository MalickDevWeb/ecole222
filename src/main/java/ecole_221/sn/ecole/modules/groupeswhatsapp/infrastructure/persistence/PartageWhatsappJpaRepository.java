package ecole_221.sn.ecole.modules.groupeswhatsapp.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Partages des annonces dans les groupes WhatsApp de l'école courante. */
public interface PartageWhatsappJpaRepository extends JpaRepository<PartageWhatsappJpaEntity, String> {
    List<PartageWhatsappJpaEntity> findTop100ByOrderByPartageLeDesc();
}
