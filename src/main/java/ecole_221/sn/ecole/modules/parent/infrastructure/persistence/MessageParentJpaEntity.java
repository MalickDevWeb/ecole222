package ecole_221.sn.ecole.modules.parent.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Message échangé entre un parent et l'école : un professeur, ou l'administration
 * (interlocuteurId = {@value #ADMINISTRATION}, boîte partagée par les administrateurs de l'école).
 */
@Entity
@Table(name = "messages_parents", indexes = {
        @Index(name = "idx_msg_parents_parent", columnList = "organization_id, parent_id"),
        @Index(name = "idx_msg_parents_interlocuteur", columnList = "organization_id, interlocuteur_id")})
public class MessageParentJpaEntity {

    public static final String ADMINISTRATION = "ADMINISTRATION";

    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id", nullable = false)
    private String organizationId;

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "parent_id", nullable = false)
    private String parentId;

    @Column(name = "interlocuteur_id", nullable = false)
    private String interlocuteurId;

    @Column(name = "auteur_id", nullable = false)
    private String auteurId;

    @Column(name = "de_parent", nullable = false)
    private boolean deParent;

    @Column(name = "sujet", nullable = false, length = 200)
    private String sujet;

    @Column(name = "contenu", nullable = false, columnDefinition = "text")
    private String contenu;

    @Column(name = "envoye_le", nullable = false)
    private LocalDateTime envoyeLe;

    @Column(name = "lu_le")
    private LocalDateTime luLe;

    @Column(name = "repond_a", length = 64)
    private String repondA;

    public String getOrganizationId() { return organizationId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public String getInterlocuteurId() { return interlocuteurId; }
    public void setInterlocuteurId(String interlocuteurId) { this.interlocuteurId = interlocuteurId; }
    public String getAuteurId() { return auteurId; }
    public void setAuteurId(String auteurId) { this.auteurId = auteurId; }
    public boolean isDeParent() { return deParent; }
    public void setDeParent(boolean deParent) { this.deParent = deParent; }
    public String getSujet() { return sujet; }
    public void setSujet(String sujet) { this.sujet = sujet; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public LocalDateTime getEnvoyeLe() { return envoyeLe; }
    public void setEnvoyeLe(LocalDateTime envoyeLe) { this.envoyeLe = envoyeLe; }
    public LocalDateTime getLuLe() { return luLe; }
    public void setLuLe(LocalDateTime luLe) { this.luLe = luLe; }
    public String getRepondA() { return repondA; }
    public void setRepondA(String repondA) { this.repondA = repondA; }
}
