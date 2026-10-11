package ecole_221.sn.ecole.modules.planning.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rooms")
public class RoomJpaEntity {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "organization_id")
    private String organizationId;

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }


    @Id
    private String id;
    private String name;
    private String nom;
    private int capacity;
    private Integer capacite;
    private String equipment;
    private String location;

    @jakarta.persistence.PrePersist
    @jakarta.persistence.PreUpdate
    public void syncFields() {
        if (this.nom == null) this.nom = this.name;
        if (this.name == null) this.name = this.nom;
        if (this.capacite == null) this.capacite = this.capacity;
    }

    public RoomJpaEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; this.nom = name; }
    
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; this.name = nom; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; this.capacite = capacity; }
    
    public String getEquipment() { return equipment; }
    public void setEquipment(String equipment) { this.equipment = equipment; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
