package ecole_221.sn.ecole.modules.admin.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Créneau de l'emploi du temps saisi par l'administration d'une école (matière, enseignant, salle, classe).
 * Avant : liste en mémoire pré-remplie de créneaux inventés et partagée par toutes les écoles.
 * Désormais persisté et cloisonné par école (@TenantId). La table « slots » du module planning ne
 * convient pas : elle n'a ni classe, ni enseignant, ni salle, et n'accepte que les types CM/TD/TP/EXAM/PROJECT.
 */
@Entity
@Table(name = "creneaux_planning")
public class CreneauPlanningJpaEntity {

    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id")
    private String organizationId;

    @Id
    private String id;
    private String jour;
    @Column(name = "heure_debut")
    private String heureDebut;
    @Column(name = "heure_fin")
    private String heureFin;
    private String nom;
    private String type;
    private String professeur;
    private String salle;
    @Column(length = 1000)
    private String description;
    @Column(name = "classe_id")
    private String classeId;

    public CreneauPlanningJpaEntity() {}

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getJour() { return jour; }
    public void setJour(String jour) { this.jour = jour; }
    public String getHeureDebut() { return heureDebut; }
    public void setHeureDebut(String heureDebut) { this.heureDebut = heureDebut; }
    public String getHeureFin() { return heureFin; }
    public void setHeureFin(String heureFin) { this.heureFin = heureFin; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getProfesseur() { return professeur; }
    public void setProfesseur(String professeur) { this.professeur = professeur; }
    public String getSalle() { return salle; }
    public void setSalle(String salle) { this.salle = salle; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getClasseId() { return classeId; }
    public void setClasseId(String classeId) { this.classeId = classeId; }
}
