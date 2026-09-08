package ecole_221.sn.ecole.modules.planning.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "classes")
public class ClasseJpaEntity {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "organization_id")
    private String organizationId;

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }


    @Id
    private String id;
    private String name;
    private String code;
    private String filiereId;
    private String level;
    private int capacity;
    private int currentSize;

    public ClasseJpaEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public String getFiliereId() { return filiereId; }
    public void setFiliereId(String filiereId) { this.filiereId = filiereId; }
    
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    
    public int getCurrentSize() { return currentSize; }
    public void setCurrentSize(int currentSize) { this.currentSize = currentSize; }
}
