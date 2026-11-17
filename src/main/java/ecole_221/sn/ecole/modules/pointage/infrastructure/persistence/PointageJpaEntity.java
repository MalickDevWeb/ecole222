package ecole_221.sn.ecole.modules.pointage.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Pointage d'arrivée ou de départ d'un élève ou d'un membre du personnel (V25). */
@Entity
@Table(name = "pointages")
public class PointageJpaEntity {

    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id")
    private String organizationId;

    @Id
    private String id;
    @Column(name = "utilisateur_id")
    private String utilisateurId;
    private String nom;
    /** Rôle du membre dans l'école au moment du pointage (ETUDIANT, PROFESSEUR…). */
    private String role;
    /** ARRIVEE ou DEPART, décidé par le serveur. */
    private String type;
    private LocalDateTime horodatage;

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(String utilisateurId) { this.utilisateurId = utilisateurId; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public LocalDateTime getHorodatage() { return horodatage; }
    public void setHorodatage(LocalDateTime horodatage) { this.horodatage = horodatage; }
}
