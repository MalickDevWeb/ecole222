package ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Domaine propre déclaré par une école (V17, décision du Tech Lead du 15/11 : pointage + déclaration).
 * Table GLOBALE : pas de @TenantId, la résolution d'un hôte a lieu avant que l'école de la requête soit connue.
 */
@Entity
@Table(name = "domaines_ecoles")
public class DomaineEcoleJpaEntity {

    @Id
    @Column(length = 64)
    private String id;
    @Column(name = "organization_id", nullable = false)
    private String organizationId;
    /** A-label minuscule (punycode), unique : un hostname = une seule école. */
    @Column(nullable = false, unique = true, length = 253)
    private String hostname;
    /** EN_ATTENTE_POINTAGE, ACTIF ou DESACTIVE. */
    @Column(nullable = false, length = 30)
    private String statut;
    @Column(name = "echecs_consecutifs", nullable = false)
    private int echecsConsecutifs;
    /** Dernière vérification RÉUSSIE du pointage. */
    @Column(name = "verifie_le")
    private LocalDateTime verifieLe;
    /** Dernière vérification, réussie ou non. */
    @Column(name = "derniere_verification")
    private LocalDateTime derniereVerification;
    @Column(name = "cree_par")
    private String creePar;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public int getEchecsConsecutifs() { return echecsConsecutifs; }
    public void setEchecsConsecutifs(int echecsConsecutifs) { this.echecsConsecutifs = echecsConsecutifs; }
    public LocalDateTime getVerifieLe() { return verifieLe; }
    public void setVerifieLe(LocalDateTime verifieLe) { this.verifieLe = verifieLe; }
    public LocalDateTime getDerniereVerification() { return derniereVerification; }
    public void setDerniereVerification(LocalDateTime derniereVerification) { this.derniereVerification = derniereVerification; }
    public String getCreePar() { return creePar; }
    public void setCreePar(String creePar) { this.creePar = creePar; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
