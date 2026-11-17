package ecole_221.sn.ecole.modules.notifications.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Historique des annonces et alertes d'une école (V18, décision du Tech Lead du 15/11). Chaque annonce a produit une
 * notification par compte visé (notifications.action_type = « annonce », action_link = id de l'annonce).
 */
@Entity
@Table(name = "annonces_ecole")
public class AnnonceEcoleJpaEntity {

    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id")
    private String organizationId;

    @Id
    @Column(length = 64)
    private String id;
    @Column(nullable = false, length = 200)
    private String titre;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    /** ANNONCE ou URGENCE. */
    @Column(nullable = false, length = 20)
    private String nature;
    /** TOUTE_ECOLE, ELEVES, PARENTS, PROFESSEURS ou PERSONNEL. */
    @Column(nullable = false, length = 20)
    private String destinataires;
    @Column(name = "destinataires_touches", nullable = false)
    private int destinatairesTouches;
    @Column(name = "auteur_id")
    private String auteurId;
    /** Nom de l'auteur au moment de l'envoi. */
    private String auteur;
    @Column(name = "envoyee_le", nullable = false)
    private LocalDateTime envoyeeLe;

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getNature() { return nature; }
    public void setNature(String nature) { this.nature = nature; }
    public String getDestinataires() { return destinataires; }
    public void setDestinataires(String destinataires) { this.destinataires = destinataires; }
    public int getDestinatairesTouches() { return destinatairesTouches; }
    public void setDestinatairesTouches(int destinatairesTouches) { this.destinatairesTouches = destinatairesTouches; }
    public String getAuteurId() { return auteurId; }
    public void setAuteurId(String auteurId) { this.auteurId = auteurId; }
    public String getAuteur() { return auteur; }
    public void setAuteur(String auteur) { this.auteur = auteur; }
    public LocalDateTime getEnvoyeeLe() { return envoyeeLe; }
    public void setEnvoyeeLe(LocalDateTime envoyeeLe) { this.envoyeeLe = envoyeeLe; }
}
