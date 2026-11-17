package ecole_221.sn.ecole.modules.groupeswhatsapp.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Partage d'une annonce dans un groupe WhatsApp : « ouvert dans WhatsApp le … à … » (V27, FEAT-007, BR-05). */
@Entity
@Table(name = "partages_whatsapp")
public class PartageWhatsappJpaEntity {

    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id")
    private String organizationId;

    @Id
    private String id;
    @Column(name = "groupe_id")
    private String groupeId;
    /** Nom du groupe au moment du partage (conservé si le groupe est supprimé ensuite). */
    @Column(name = "groupe_nom")
    private String groupeNom;
    private String titre;
    @Column(name = "auteur_id")
    private String auteurId;
    @Column(name = "auteur_nom")
    private String auteurNom;
    @Column(name = "partage_le")
    private LocalDateTime partageLe;

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String v) { this.organizationId = v; }
    public String getId() { return id; }
    public void setId(String v) { this.id = v; }
    public String getGroupeId() { return groupeId; }
    public void setGroupeId(String v) { this.groupeId = v; }
    public String getGroupeNom() { return groupeNom; }
    public void setGroupeNom(String v) { this.groupeNom = v; }
    public String getTitre() { return titre; }
    public void setTitre(String v) { this.titre = v; }
    public String getAuteurId() { return auteurId; }
    public void setAuteurId(String v) { this.auteurId = v; }
    public String getAuteurNom() { return auteurNom; }
    public void setAuteurNom(String v) { this.auteurNom = v; }
    public LocalDateTime getPartageLe() { return partageLe; }
    public void setPartageLe(LocalDateTime v) { this.partageLe = v; }
}
