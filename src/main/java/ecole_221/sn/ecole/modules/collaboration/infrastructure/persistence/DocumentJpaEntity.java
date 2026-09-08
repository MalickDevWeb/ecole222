package ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "documents")
public class DocumentJpaEntity {

    @Id
    private String id;
    private String groupId;
    private String name;
    private String type;
    private String size;
    private String url;
    private String addedBy;

    public DocumentJpaEntity() {}

    public DocumentJpaEntity(String id, String groupId, String name, String type, String size, String url, String addedBy) {
        this.id = id;
        this.groupId = groupId;
        this.name = name;
        this.type = type;
        this.size = size;
        this.url = url;
        this.addedBy = addedBy;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getAddedBy() { return addedBy; }
    public void setAddedBy(String addedBy) { this.addedBy = addedBy; }
}
