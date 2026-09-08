package ecole_221.sn.ecole.modules.collaboration.domain.model;

import java.util.Objects;

/**
 * Agrégat racine représentant un groupe de travail.
 * Constructeur privé pour garantir l'invariance - utiliser WorkgroupFactory.
 */
public class Workgroup {
    
    private final String id;
    private String name;
    private String description;
    private final String creatorId;
    private int memberCount;
    
    // Constructeur package-private pour permettre aux Factories d'y accéder
    Workgroup(String id, String name, String description, String creatorId, int memberCount) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.name = Objects.requireNonNull(name, "Le nom ne peut pas être null");
        this.description = description;
        this.creatorId = creatorId; // Peut être null pour compatibilité avec base de données existante
        if (memberCount < 0) {
            throw new IllegalArgumentException("Le nombre de membres ne peut pas être négatif");
        }
        this.memberCount = memberCount;
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCreatorId() { return creatorId; }
    public int getMemberCount() { return memberCount; }
    
    // Comportements métier
    public void updateName(String newName) {
        this.name = Objects.requireNonNull(newName, "Le nom ne peut pas être null");
    }
    
    public void updateDescription(String newDescription) {
        this.description = newDescription;
    }
    
    public void addMember() {
        this.memberCount++;
    }
    
    public void removeMember() {
        if (this.memberCount > 0) {
            this.memberCount--;
        }
    }
    
    public boolean isEmpty() {
        return this.memberCount == 0;
    }
    
    public boolean hasMembers() {
        return this.memberCount > 0;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Workgroup workgroup = (Workgroup) o;
        return Objects.equals(id, workgroup.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Workgroup{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", creatorId='" + creatorId + '\'' +
                ", memberCount=" + memberCount +
                '}';
    }
}