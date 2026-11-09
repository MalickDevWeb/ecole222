package ecole_221.sn.ecole.modules.admin.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Affectation d'un élève à une classe de SON école (une classe par élève et par école).
 * Avant : l'identifiant de classe envoyé à la création de l'élève était ignoré ; la page d'une classe
 * n'affichait donc jamais ses élèves après rechargement.
 */
@Entity
@Table(name = "inscriptions_classes")
public class InscriptionClasseJpaEntity {

    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id")
    private String organizationId;

    @Id
    private String id;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(name = "classe_id", nullable = false)
    private String classeId;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public InscriptionClasseJpaEntity() {}

    public InscriptionClasseJpaEntity(String id, String userId, String classeId) {
        this.id = id;
        this.userId = userId;
        this.classeId = classeId;
        this.updatedAt = LocalDateTime.now();
    }

    public String getOrganizationId() { return organizationId; }
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getClasseId() { return classeId; }
    public void setClasseId(String classeId) { this.classeId = classeId; this.updatedAt = LocalDateTime.now(); }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
