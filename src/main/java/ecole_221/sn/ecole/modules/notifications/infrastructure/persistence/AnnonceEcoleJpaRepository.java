package ecole_221.sn.ecole.modules.notifications.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Historique des annonces (V18) ; filtré par école (@TenantId). */
public interface AnnonceEcoleJpaRepository extends JpaRepository<AnnonceEcoleJpaEntity, String> {

    List<AnnonceEcoleJpaEntity> findAllByOrderByEnvoyeeLeDesc();
}
