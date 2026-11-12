package ecole_221.sn.ecole.modules.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Demande de réinitialisation du mot de passe (APP-FEAT-02) : seul le haché du jeton est conservé. */
@Entity
@Table(name = "reinitialisations_mot_de_passe")
public class ReinitialisationMotDePasseJpaEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "jeton_hache", nullable = false, unique = true, length = 64)
    private String jetonHache;

    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe;

    @Column(name = "expire_le", nullable = false)
    private LocalDateTime expireLe;

    @Column(name = "utilise_le")
    private LocalDateTime utiliseLe;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getJetonHache() { return jetonHache; }
    public void setJetonHache(String jetonHache) { this.jetonHache = jetonHache; }
    public LocalDateTime getCreeLe() { return creeLe; }
    public void setCreeLe(LocalDateTime creeLe) { this.creeLe = creeLe; }
    public LocalDateTime getExpireLe() { return expireLe; }
    public void setExpireLe(LocalDateTime expireLe) { this.expireLe = expireLe; }
    public LocalDateTime getUtiliseLe() { return utiliseLe; }
    public void setUtiliseLe(LocalDateTime utiliseLe) { this.utiliseLe = utiliseLe; }
}
