package ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Membre d'un groupe de travail (V10). */
@Entity
@Table(name = "membres_groupes_travail")
public class MembreGroupeJpaEntity {

    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id")
    private String organizationId;

    @Id
    private String id;
    @Column(name = "groupe_id")
    private String groupeId;
    @Column(name = "utilisateur_id")
    private String utilisateurId;
    private boolean chef;
    @Column(name = "ajoute_le")
    private LocalDateTime ajouteLe;

    public MembreGroupeJpaEntity() {}

    public MembreGroupeJpaEntity(String id, String groupeId, String utilisateurId, boolean chef) {
        this.id = id;
        this.groupeId = groupeId;
        this.utilisateurId = utilisateurId;
        this.chef = chef;
        this.ajouteLe = LocalDateTime.now();
    }

    public String getOrganizationId() { return organizationId; }
    public String getId() { return id; }
    public String getGroupeId() { return groupeId; }
    public String getUtilisateurId() { return utilisateurId; }
    public boolean isChef() { return chef; }
    public void setChef(boolean chef) { this.chef = chef; }
    public LocalDateTime getAjouteLe() { return ajouteLe; }
}
