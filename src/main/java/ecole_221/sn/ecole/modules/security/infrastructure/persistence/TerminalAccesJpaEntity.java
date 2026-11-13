package ecole_221.sn.ecole.modules.security.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Terminal d'accès (borne, lecteur de portique) d'une école — V9, APP-BE-21. */
@Entity
@Table(name = "terminaux_acces")
public class TerminalAccesJpaEntity {

    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id")
    private String organizationId;

    @Id
    private String id;
    private String nom;
    private String emplacement;
    /** Actif, Inactif ou Révoqué (définitif). */
    private String statut;
    @Column(name = "vigile_id")
    private String vigileId;
    @Column(name = "cree_le")
    private LocalDateTime creeLe;
    @Column(name = "modifie_le")
    private LocalDateTime modifieLe;
    @Column(name = "revoque_le")
    private LocalDateTime revoqueLe;

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getEmplacement() { return emplacement; }
    public void setEmplacement(String emplacement) { this.emplacement = emplacement; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getVigileId() { return vigileId; }
    public void setVigileId(String vigileId) { this.vigileId = vigileId; }
    public LocalDateTime getCreeLe() { return creeLe; }
    public void setCreeLe(LocalDateTime creeLe) { this.creeLe = creeLe; }
    public LocalDateTime getModifieLe() { return modifieLe; }
    public void setModifieLe(LocalDateTime modifieLe) { this.modifieLe = modifieLe; }
    public LocalDateTime getRevoqueLe() { return revoqueLe; }
    public void setRevoqueLe(LocalDateTime revoqueLe) { this.revoqueLe = revoqueLe; }
}
