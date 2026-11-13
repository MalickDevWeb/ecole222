package ecole_221.sn.ecole.infrastructure.security.revocation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Jeton révoqué à la déconnexion (empreinte SHA-256 seulement), gardé jusqu'à son expiration (V11). */
@Entity
@Table(name = "jetons_revoques")
public class JetonRevoqueJpaEntity {

    @Id
    @Column(name = "empreinte", length = 64, nullable = false)
    private String empreinte;

    @Column(name = "expire_le", nullable = false)
    private Instant expireLe;

    protected JetonRevoqueJpaEntity() {
    }

    public JetonRevoqueJpaEntity(String empreinte, Instant expireLe) {
        this.empreinte = empreinte;
        this.expireLe = expireLe;
    }

    public String getEmpreinte() { return empreinte; }
    public Instant getExpireLe() { return expireLe; }
}
