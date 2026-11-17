package ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Élément de collaboration d'une école (page de wiki, note, journal, présence, devoir de groupe) — V8. */
@Entity
@Table(name = "elements_collaboratifs")
public class ElementCollaboratifJpaEntity {

    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id")
    private String organizationId;

    @Id
    private String id;
    private String genre;
    /** Identifiant choisi par l'écran (unique par école et par genre). */
    private String cle;
    @Column(name = "groupe_id")
    private String groupeId;
    @Column(name = "auteur_id")
    private String auteurId;
    private boolean prive;
    /** Champs validés, en JSON. */
    @Column(columnDefinition = "text")
    private String contenu;
    @Column(name = "cree_le")
    private LocalDateTime creeLe;
    @Column(name = "modifie_le")
    private LocalDateTime modifieLe;

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public String getCle() { return cle; }
    public void setCle(String cle) { this.cle = cle; }
    public String getGroupeId() { return groupeId; }
    public void setGroupeId(String groupeId) { this.groupeId = groupeId; }
    public String getAuteurId() { return auteurId; }
    public void setAuteurId(String auteurId) { this.auteurId = auteurId; }
    public boolean isPrive() { return prive; }
    public void setPrive(boolean prive) { this.prive = prive; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public LocalDateTime getCreeLe() { return creeLe; }
    public void setCreeLe(LocalDateTime creeLe) { this.creeLe = creeLe; }
    public LocalDateTime getModifieLe() { return modifieLe; }
    public void setModifieLe(LocalDateTime modifieLe) { this.modifieLe = modifieLe; }
}
