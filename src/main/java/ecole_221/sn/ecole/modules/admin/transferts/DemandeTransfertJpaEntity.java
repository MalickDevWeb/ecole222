package ecole_221.sn.ecole.modules.admin.transferts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Demande de transfert d'un élève d'une école vers une autre (V12). Lue par les deux écoles. */
@Entity
@Table(name = "demandes_transfert")
public class DemandeTransfertJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;
    @Column(name = "eleve_id", nullable = false)
    private String eleveId;
    @Column(name = "ecole_source_id", nullable = false)
    private String ecoleSourceId;
    @Column(name = "ecole_destination_id", nullable = false)
    private String ecoleDestinationId;
    @Column(name = "motif", length = 1000)
    private String motif;
    @Column(name = "statut", length = 20, nullable = false)
    private String statut;
    @Column(name = "cree_le", nullable = false)
    private Instant creeLe;
    @Column(name = "cree_par", nullable = false)
    private String creePar;
    @Column(name = "decide_le")
    private Instant decideLe;
    @Column(name = "decide_par")
    private String decidePar;
    @Column(name = "motif_decision", length = 1000)
    private String motifDecision;
    @Column(name = "dossier", columnDefinition = "text")
    private String dossier;

    protected DemandeTransfertJpaEntity() {
    }

    public DemandeTransfertJpaEntity(String id, String eleveId, String ecoleSourceId, String ecoleDestinationId, String motif, String creePar) {
        this.id = id;
        this.eleveId = eleveId;
        this.ecoleSourceId = ecoleSourceId;
        this.ecoleDestinationId = ecoleDestinationId;
        this.motif = motif;
        this.statut = TransfertsElevesService.EN_ATTENTE;
        this.creeLe = Instant.now();
        this.creePar = creePar;
    }

    void decider(String statut, String par, String motifDecision) {
        this.statut = statut;
        this.decideLe = Instant.now();
        this.decidePar = par;
        this.motifDecision = motifDecision;
    }

    void joindreDossier(String dossier) { this.dossier = dossier; }

    public String getId() { return id; }
    public String getEleveId() { return eleveId; }
    public String getEcoleSourceId() { return ecoleSourceId; }
    public String getEcoleDestinationId() { return ecoleDestinationId; }
    public String getMotif() { return motif; }
    public String getStatut() { return statut; }
    public Instant getCreeLe() { return creeLe; }
    public String getCreePar() { return creePar; }
    public Instant getDecideLe() { return decideLe; }
    public String getDecidePar() { return decidePar; }
    public String getMotifDecision() { return motifDecision; }
    public String getDossier() { return dossier; }
}
