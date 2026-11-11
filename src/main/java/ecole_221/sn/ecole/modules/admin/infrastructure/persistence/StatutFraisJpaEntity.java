package ecole_221.sn.ecole.modules.admin.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Statut des frais d'un élève, saisi par l'administration de SON école.
 * Avant : carte en mémoire (perdue au redémarrage) avec « En Règle » par défaut pour tout élève inconnu.
 * Désormais : persisté ; aucune ligne = statut non renseigné (jamais présumé « En Règle »).
 */
@Entity
@Table(name = "statuts_frais_eleves")
public class StatutFraisJpaEntity {

    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id")
    private String organizationId;

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(name = "statut", nullable = false)
    private String statut;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public StatutFraisJpaEntity() {}

    public StatutFraisJpaEntity(String userId, String statut) {
        this.userId = userId;
        this.statut = statut;
        this.updatedAt = LocalDateTime.now();
    }

    public String getOrganizationId() { return organizationId; }
    public String getUserId() { return userId; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; this.updatedAt = LocalDateTime.now(); }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
