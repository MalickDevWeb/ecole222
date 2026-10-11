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
    private String nom;
    private String code;
    private String filiereId;
    private String level;
    private String niveau;
    private int capacity;
    private int currentSize;

    @jakarta.persistence.PrePersist
    @jakarta.persistence.PreUpdate
    public void syncFields() {
        if (this.nom == null) this.nom = this.name;
        if (this.name == null) this.name = this.nom;
        if (this.niveau == null) this.niveau = this.level;
        if (this.level == null) this.level = this.niveau;
    }

    public ClasseJpaEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; this.nom = name; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; this.name = nom; }
    
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
