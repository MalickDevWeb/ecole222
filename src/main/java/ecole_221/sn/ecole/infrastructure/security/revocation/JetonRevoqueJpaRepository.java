package ecole_221.sn.ecole.infrastructure.security.revocation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface JetonRevoqueJpaRepository extends JpaRepository<JetonRevoqueJpaEntity, String> {

    @Modifying
    @Transactional
    @Query("delete from JetonRevoqueJpaEntity j where j.expireLe < :limite")
    int supprimerExpires(Instant limite);
}
