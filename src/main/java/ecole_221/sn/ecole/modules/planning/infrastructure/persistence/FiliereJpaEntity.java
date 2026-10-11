package ecole_221.sn.ecole.modules.planning.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "filieres")
public class FiliereJpaEntity {
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
    private String description;
    private String level;

    @jakarta.persistence.PrePersist
    @jakarta.persistence.PreUpdate
    public void syncFields() {
        if (this.nom == null) this.nom = this.name;
        if (this.name == null) this.name = this.nom;
    }

    public FiliereJpaEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; this.nom = name; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; this.name = nom; }
    
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
}
