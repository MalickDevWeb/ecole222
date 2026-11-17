package ecole_221.sn.ecole.modules.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Demande de réinitialisation du mot de passe (APP-FEAT-02) : seul le haché du jeton est conservé. */
@Entity
@Table(name = "reinitialisations_mot_de_passe")
public class ReinitialisationMotDePasseJpaEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "jeton_hache", nullable = false, unique = true, length = 64)
    private String jetonHache;

    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe;

    @Column(name = "expire_le", nullable = false)
    private LocalDateTime expireLe;

    @Column(name = "utilise_le")
    private LocalDateTime utiliseLe;

    // ── V20 : liens d'accès décidés par l'école ─────────────────────────────────────────────────────────────────
    /** ACTIVATION (compte créé, 72 h) ou REINITIALISATION (e-mail libre-service 30 min, par l'école 2 h). */
    @Column(nullable = false, length = 20)
    private String type = "REINITIALISATION";
    /** École qui a émis le lien (null : demande en libre-service par e-mail). */
    @Column(name = "organization_id")
    private String organizationId;
    @Column(name = "cree_par")
    private String creePar;
    /** WHATSAPP, SMS ou EMAIL. */
    @Column(length = 20)
    private String canal;
    @Column(name = "numero_masque", length = 40)
    private String numeroMasque;
    /** Lien annulé (remplacé par un nouveau, ou trop d'essais du code oral). */
    @Column(name = "annule_le")
    private LocalDateTime annuleLe;
    /** Accord d'un second administrateur nécessaire avant de délivrer le lien. */
    @Column(name = "accord_requis", nullable = false)
    private boolean accordRequis;
    @Column(name = "accorde_par")
    private String accordePar;
    /** Code oral de 6 chiffres (comptes à privilèges), haché SHA-256. */
    @Column(name = "code_hache", length = 64)
    private String codeHache;
    @Column(name = "essais_code", nullable = false)
    private int essaisCode;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getJetonHache() { return jetonHache; }
    public void setJetonHache(String jetonHache) { this.jetonHache = jetonHache; }
    public LocalDateTime getCreeLe() { return creeLe; }
    public void setCreeLe(LocalDateTime creeLe) { this.creeLe = creeLe; }
    public LocalDateTime getExpireLe() { return expireLe; }
    public void setExpireLe(LocalDateTime expireLe) { this.expireLe = expireLe; }
    public LocalDateTime getUtiliseLe() { return utiliseLe; }
    public void setUtiliseLe(LocalDateTime utiliseLe) { this.utiliseLe = utiliseLe; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public String getCreePar() { return creePar; }
    public void setCreePar(String creePar) { this.creePar = creePar; }
    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }
    public String getNumeroMasque() { return numeroMasque; }
    public void setNumeroMasque(String numeroMasque) { this.numeroMasque = numeroMasque; }
    public LocalDateTime getAnnuleLe() { return annuleLe; }
    public void setAnnuleLe(LocalDateTime annuleLe) { this.annuleLe = annuleLe; }
    public boolean isAccordRequis() { return accordRequis; }
    public void setAccordRequis(boolean accordRequis) { this.accordRequis = accordRequis; }
    public String getAccordePar() { return accordePar; }
    public void setAccordePar(String accordePar) { this.accordePar = accordePar; }
    public String getCodeHache() { return codeHache; }
    public void setCodeHache(String codeHache) { this.codeHache = codeHache; }
    public int getEssaisCode() { return essaisCode; }
    public void setEssaisCode(int essaisCode) { this.essaisCode = essaisCode; }
}
