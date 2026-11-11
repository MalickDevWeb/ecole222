package ecole_221.sn.ecole.modules.admin.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Observation consignée par l'administration sur un élève de l'école (suivi individuel). */
@Entity
@Table(name = "observations_eleves", indexes = @Index(name = "idx_observations_eleve", columnList = "organization_id, eleve_id"))
public class ObservationEleveJpaEntity {

    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id", nullable = false)
    private String organizationId;

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "eleve_id", nullable = false)
    private String eleveId;

    @Column(name = "categorie", nullable = false, length = 40)
    private String categorie;

    @Column(name = "texte", nullable = false, length = 1000)
    private String texte;

    @Column(name = "auteur_id", nullable = false)
    private String auteurId;

    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEleveId() { return eleveId; }
    public void setEleveId(String eleveId) { this.eleveId = eleveId; }
    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }
    public String getTexte() { return texte; }
    public void setTexte(String texte) { this.texte = texte; }
    public String getAuteurId() { return auteurId; }
    public void setAuteurId(String auteurId) { this.auteurId = auteurId; }
    public LocalDateTime getCreeLe() { return creeLe; }
    public void setCreeLe(LocalDateTime creeLe) { this.creeLe = creeLe; }
}
