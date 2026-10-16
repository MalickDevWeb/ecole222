package ecole_221.sn.ecole.modules.parent.infrastructure.persistence;

import ecole_221.sn.ecole.modules.parent.domain.model.Appointment;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "parent_appointments")
public class AppointmentJpaEntity {
    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id")
    private String organizationId;

    @Id
    private String id;

    @Column(name = "parent_id", nullable = false)
    private String parentId;

    @Column(name = "professeur_id")
    private String professeurId;

    @Column(name = "professeur_nom")
    private String professeurNom;

    @Column(name = "date_souhaitee")
    private String dateSouhaitee;

    @Column(name = "motif", length = 1000)
    private String motif;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "meet_url")
    private String meetUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public AppointmentJpaEntity() {}

    public static AppointmentJpaEntity fromDomain(Appointment d) {
        AppointmentJpaEntity e = new AppointmentJpaEntity();
        e.id = d.getId();
        e.parentId = d.getParentId();
        e.professeurId = d.getProfesseurId();
        e.professeurNom = d.getProfesseurNom();
        e.dateSouhaitee = d.getDateSouhaitee();
        e.motif = d.getMotif();
        e.status = d.getStatus();
        e.meetUrl = d.getMeetUrl();
        e.createdAt = d.getCreatedAt();
        return e;
    }

    public Appointment toDomain() {
        return new Appointment(id, parentId, professeurId, professeurNom, dateSouhaitee, motif, status, meetUrl, createdAt);
    }

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public String getProfesseurId() { return professeurId; }
    public void setProfesseurId(String professeurId) { this.professeurId = professeurId; }
    public String getProfesseurNom() { return professeurNom; }
    public void setProfesseurNom(String professeurNom) { this.professeurNom = professeurNom; }
    public String getDateSouhaitee() { return dateSouhaitee; }
    public void setDateSouhaitee(String dateSouhaitee) { this.dateSouhaitee = dateSouhaitee; }
    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMeetUrl() { return meetUrl; }
    public void setMeetUrl(String meetUrl) { this.meetUrl = meetUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

