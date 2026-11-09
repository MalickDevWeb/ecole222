package ecole_221.sn.ecole.modules.admin.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Affectation du portail d'accès d'une école (portail + vigile).
 * Avant : valeur en mémoire PARTAGÉE par toutes les écoles, « Porte Principale » par défaut.
 */
@Entity
@Table(name = "affectations_portail")
public class AffectationPortailJpaEntity {

    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id")
    private String organizationId;

    @Id
    private String id;
    private String portail;
    private String vigile;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public AffectationPortailJpaEntity() {}

    public AffectationPortailJpaEntity(String id) {
        this.id = id;
    }

    public String getOrganizationId() { return organizationId; }
    public String getId() { return id; }
    public String getPortail() { return portail; }
    public void setPortail(String portail) { this.portail = portail; }
    public String getVigile() { return vigile; }
    public void setVigile(String vigile) { this.vigile = vigile; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
