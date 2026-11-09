package ecole_221.sn.ecole.modules.admin.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Document rattaché au dossier d'un élève (référence du fichier déposé via /upload).
 * Avant : carte en mémoire indexée par l'identifiant de l'élève SANS école → visible depuis une autre école.
 */
@Entity
@Table(name = "documents_eleves")
public class DocumentEleveJpaEntity {

    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id")
    private String organizationId;

    @Id
    private String id;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(nullable = false)
    private String name;
    private String size;
    @Column(length = 2000)
    private String url;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public DocumentEleveJpaEntity() {}

    public DocumentEleveJpaEntity(String id, String userId, String name, String size, String url) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.size = size;
        this.url = url;
        this.createdAt = LocalDateTime.now();
    }

    public String getOrganizationId() { return organizationId; }
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getSize() { return size; }
    public String getUrl() { return url; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
