package ecole_221.sn.ecole.modules.admin.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Pointage (émargement) d'un membre du personnel, saisi par l'administration de son école.
 * Avant : liste statique en mémoire, partagée par toutes les écoles, pré-remplie de 5 personnes inventées
 * (photos Unsplash). Désormais : persisté et cloisonné par école (@TenantId).
 */
@Entity
@Table(name = "pointages_personnel")
public class PointagePersonnelJpaEntity {

    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id")
    private String organizationId;

    @Id
    private String id;
    @Column(name = "user_id")
    private String userId;
    private String name;
    private String role;
    @Column(name = "check_in")
    private String checkIn;
    @Column(name = "check_out")
    private String checkOut;
    private Double hours;
    private String status;
    @Column(length = 1000)
    private String note;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public PointagePersonnelJpaEntity() {}

    public String getOrganizationId() { return organizationId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getCheckIn() { return checkIn; }
    public void setCheckIn(String checkIn) { this.checkIn = checkIn; }
    public String getCheckOut() { return checkOut; }
    public void setCheckOut(String checkOut) { this.checkOut = checkOut; }
    public Double getHours() { return hours; }
    public void setHours(Double hours) { this.hours = hours; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
