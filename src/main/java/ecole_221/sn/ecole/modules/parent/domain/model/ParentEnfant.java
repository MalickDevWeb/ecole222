package ecole_221.sn.ecole.modules.parent.domain.model;

import java.time.LocalDateTime;

/**
 * Modèle de domaine représentant la relation entre un parent et un enfant.
 */
public class ParentEnfant {
    private final String id;
    private final String parentId;
    private final String enfantId;
    private final String relation; // PERE, MERE, TUTEUR, etc.
    private final LocalDateTime createdAt;

    private ParentEnfant(String id, String parentId, String enfantId, String relation, LocalDateTime createdAt) {
        this.id = id;
        this.parentId = parentId;
        this.enfantId = enfantId;
        this.relation = relation;
        this.createdAt = createdAt;
    }

    // Factory method
    public static ParentEnfant create(String parentId, String enfantId, String relation) {
        return new ParentEnfant(
            java.util.UUID.randomUUID().toString(),
            parentId, enfantId, relation,
            LocalDateTime.now()
        );
    }

    // Factory method for reconstituting
    public static ParentEnfant reconstitute(String id, String parentId, String enfantId, 
                                          String relation, LocalDateTime createdAt) {
        return new ParentEnfant(id, parentId, enfantId, relation, createdAt);
    }

    // Getters
    public String getId() { return id; }
    public String getParentId() { return parentId; }
    public String getEnfantId() { return enfantId; }
    public String getRelation() { return relation; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
