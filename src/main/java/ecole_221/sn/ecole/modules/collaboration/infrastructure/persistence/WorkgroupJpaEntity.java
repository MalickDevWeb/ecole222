package ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "workgroups")
public class WorkgroupJpaEntity {

    @Id
    private String id;
    private String name;
    private String description;
    private String creatorId;
    private int membersCount;

    public WorkgroupJpaEntity() {}

    public WorkgroupJpaEntity(String id, String name, String description, String creatorId, int membersCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.creatorId = creatorId;
        this.membersCount = membersCount;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    public int getMembersCount() { return membersCount; }
    public void setMembersCount(int membersCount) { this.membersCount = membersCount; }
}
