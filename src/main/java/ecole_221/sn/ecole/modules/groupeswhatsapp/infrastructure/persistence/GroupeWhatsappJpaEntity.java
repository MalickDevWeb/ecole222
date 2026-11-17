package ecole_221.sn.ecole.modules.groupeswhatsapp.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Groupe WhatsApp déclaré par l'école (V27, FEAT-007). */
@Entity
@Table(name = "groupes_whatsapp")
public class GroupeWhatsappJpaEntity {

    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id")
    private String organizationId;

    @Id
    private String id;
    private String nom;
    /** TOUTE_ECOLE, ELEVES, PARENTS, PROFESSEURS ou PERSONNEL. */
    @Column(name = "public_vise")
    private String publicVise;
    /** Classe visée (groupe « Élèves » ou « Parents » d'une classe), sinon null. */
    @Column(name = "classe_id")
    private String classeId;
    @Column(name = "classe_nom")
    private String classeNom;
    /** Lien d'invitation https://chat.whatsapp.com/… */
    private String lien;
    private boolean actif;
    @Column(name = "cree_le")
    private LocalDateTime creeLe;
    @Column(name = "cree_par")
    private String creePar;

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String v) { this.organizationId = v; }
    public String getId() { return id; }
    public void setId(String v) { this.id = v; }
    public String getNom() { return nom; }
    public void setNom(String v) { this.nom = v; }
    public String getPublicVise() { return publicVise; }
    public void setPublicVise(String v) { this.publicVise = v; }
    public String getClasseId() { return classeId; }
    public void setClasseId(String v) { this.classeId = v; }
    public String getClasseNom() { return classeNom; }
    public void setClasseNom(String v) { this.classeNom = v; }
    public String getLien() { return lien; }
    public void setLien(String v) { this.lien = v; }
    public boolean isActif() { return actif; }
    public void setActif(boolean v) { this.actif = v; }
    public LocalDateTime getCreeLe() { return creeLe; }
    public void setCreeLe(LocalDateTime v) { this.creeLe = v; }
    public String getCreePar() { return creePar; }
    public void setCreePar(String v) { this.creePar = v; }
}
