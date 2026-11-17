package ecole_221.sn.ecole.infrastructure.security.revocation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Moment à partir duquel les jetons d'un compte sont acceptés (changement ou réinitialisation du mot de passe, V11). */
@Entity
@Table(name = "invalidations_jetons")
public class InvalidationJetonsJpaEntity {

    @Id
    @Column(name = "utilisateur_id", nullable = false)
    private String utilisateurId;

    @Column(name = "valides_apres", nullable = false)
    private Instant validesApres;

    protected InvalidationJetonsJpaEntity() {
    }

    public InvalidationJetonsJpaEntity(String utilisateurId, Instant validesApres) {
        this.utilisateurId = utilisateurId;
        this.validesApres = validesApres;
    }

    public String getUtilisateurId() { return utilisateurId; }
    public Instant getValidesApres() { return validesApres; }
    public void setValidesApres(Instant validesApres) { this.validesApres = validesApres; }
}
