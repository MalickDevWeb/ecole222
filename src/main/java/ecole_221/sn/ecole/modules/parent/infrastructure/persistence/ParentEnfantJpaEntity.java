package ecole_221.sn.ecole.modules.parent.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "parent_enfant")
public class ParentEnfantJpaEntity {

    @Id
    private String id;

    @Column(name = "parent_id")
    private String parentId;

    @Column(name = "enfant_id")
    private String enfantId;

    @Column(name = "relation")
    private String relation; // "PERE", "MERE", "TUTEUR", etc.

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public ParentEnfantJpaEntity() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getEnfantId() { return enfantId; }
    public void setEnfantId(String enfantId) { this.enfantId = enfantId; }

    public String getRelation() { return relation; }
    public void setRelation(String relation) { this.relation = relation; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
