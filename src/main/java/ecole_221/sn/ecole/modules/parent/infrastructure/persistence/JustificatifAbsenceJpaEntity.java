package ecole_221.sn.ecole.modules.parent.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Justificatif d'absence déposé par un parent pour une présence notée par un professeur (lot A).
 * Cycle : EN_ATTENTE → ACCEPTE (l'absence devient « excusée ») ou REFUSE (avec motif).
 */
@Entity
@Table(name = "justificatifs_absence", indexes = {
        @Index(name = "idx_justif_presence", columnList = "presence_id"),
        @Index(name = "idx_justif_eleve", columnList = "eleve_id")})
public class JustificatifAbsenceJpaEntity {

    public static final String EN_ATTENTE = "EN_ATTENTE";
    public static final String ACCEPTE = "ACCEPTE";
    public static final String REFUSE = "REFUSE";

    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id")
    private String organizationId;

    @Id
    private String id;

    @Column(name = "presence_id", nullable = false)
    private String presenceId;

    @Column(name = "eleve_id", nullable = false)
    private String eleveId;

    @Column(name = "parent_id", nullable = false)
    private String parentId;

    @Column(name = "motif", nullable = false, length = 500)
    private String motif;

    @Column(name = "fichier_url", length = 1000)
    private String fichierUrl;

    @Column(name = "statut", nullable = false, length = 20)
    private String statut;

    @Column(name = "motif_refus", length = 500)
    private String motifRefus;

    /** Statut de la présence au moment du dépôt (ABSENT ou RETARD), conservé pour l'historique. */
    @Column(name = "statut_initial", length = 20)
    private String statutInitial;

    @Column(name = "depose_le", nullable = false)
    private LocalDateTime deposeLe;

    @Column(name = "traite_le")
    private LocalDateTime traiteLe;

    @Column(name = "traite_par")
    private String traitePar;

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPresenceId() { return presenceId; }
    public void setPresenceId(String presenceId) { this.presenceId = presenceId; }
    public String getEleveId() { return eleveId; }
    public void setEleveId(String eleveId) { this.eleveId = eleveId; }
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    public String getFichierUrl() { return fichierUrl; }
    public void setFichierUrl(String fichierUrl) { this.fichierUrl = fichierUrl; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getMotifRefus() { return motifRefus; }
    public void setMotifRefus(String motifRefus) { this.motifRefus = motifRefus; }
    public String getStatutInitial() { return statutInitial; }
    public void setStatutInitial(String statutInitial) { this.statutInitial = statutInitial; }
    public LocalDateTime getDeposeLe() { return deposeLe; }
    public void setDeposeLe(LocalDateTime deposeLe) { this.deposeLe = deposeLe; }
    public LocalDateTime getTraiteLe() { return traiteLe; }
    public void setTraiteLe(LocalDateTime traiteLe) { this.traiteLe = traiteLe; }
    public String getTraitePar() { return traitePar; }
    public void setTraitePar(String traitePar) { this.traitePar = traitePar; }
}
