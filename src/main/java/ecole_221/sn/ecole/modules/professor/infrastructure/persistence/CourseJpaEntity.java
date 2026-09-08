package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "courses")
public class CourseJpaEntity {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "organization_id")
    private String organizationId;

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }


    @Id
    private String id;
    private String name;
    private String classe;
    private int totalHours;
    private String profId;
    private String code;
    private String description;

    public CourseJpaEntity() {}

    public CourseJpaEntity(String id, String name, String classe, int totalHours, String profId) {
        this.id = id;
        this.name = name;
        this.classe = classe;
        this.totalHours = totalHours;
        this.profId = profId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getClasse() { return classe; }
    public void setClasse(String classe) { this.classe = classe; }
    public int getTotalHours() { return totalHours; }
    public void setTotalHours(int totalHours) { this.totalHours = totalHours; }
    public String getProfId() { return profId; }
    public void setProfId(String profId) { this.profId = profId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
