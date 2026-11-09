package ecole_221.sn.ecole.modules.admin.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Paramètres de la campagne de candidature d'UNE école (une ligne par école, id = identifiant de l'école).
 * Avant : map statique commune à toutes les écoles, pré-remplie (ouverte du 2026-06-01 au 2026-08-31,
 * quatre pièces exigées) et perdue au redémarrage. Tant que l'école n'a rien enregistré : fermée, sans dates.
 */
@Entity
@Table(name = "parametres_candidatures")
public class ParametresCandidaturesJpaEntity {

    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id")
    private String organizationId;

    @Id
    private String id;
    private boolean ouvert;
    @Column(name = "date_ouverture")
    private String dateOuverture;
    @Column(name = "date_fermeture")
    private String dateFermeture;
    @Column(name = "message_avis", length = 2000)
    private String messageAvis;
    @Column(name = "show_register_button")
    private boolean showRegisterButton;
    /** Pièces exigées, JSON {"cni":true,…} tel que saisi par l'école (null si jamais saisi). */
    @Column(name = "docs_requis", length = 2000)
    private String docsRequisJson;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ParametresCandidaturesJpaEntity() {}

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public boolean isOuvert() { return ouvert; }
    public void setOuvert(boolean ouvert) { this.ouvert = ouvert; }
    public String getDateOuverture() { return dateOuverture; }
    public void setDateOuverture(String dateOuverture) { this.dateOuverture = dateOuverture; }
    public String getDateFermeture() { return dateFermeture; }
    public void setDateFermeture(String dateFermeture) { this.dateFermeture = dateFermeture; }
    public String getMessageAvis() { return messageAvis; }
    public void setMessageAvis(String messageAvis) { this.messageAvis = messageAvis; }
    public boolean isShowRegisterButton() { return showRegisterButton; }
    public void setShowRegisterButton(boolean showRegisterButton) { this.showRegisterButton = showRegisterButton; }
    public String getDocsRequisJson() { return docsRequisJson; }
    public void setDocsRequisJson(String docsRequisJson) { this.docsRequisJson = docsRequisJson; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
