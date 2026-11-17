package ecole_221.sn.ecole.modules.groupeswhatsapp.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Groupes WhatsApp de l'école courante (filtre d'école : @TenantId). */
public interface GroupeWhatsappJpaRepository extends JpaRepository<GroupeWhatsappJpaEntity, String> {
    List<GroupeWhatsappJpaEntity> findAllByOrderByNomAsc();
    boolean existsByLien(String lien);
    boolean existsByLienAndIdNot(String lien, String id);
}
