package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Entrée du cahier de textes : ce qui a été fait lors d'une séance d'un cours et le travail à faire (lot E). */
@Entity
@Table(name = "cahier_de_textes")
public class CahierDeTextesJpaEntity {

    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id")
    private String organizationId;

    @Id
    private String id;
    @Column(name = "course_id", nullable = false)
    private String courseId;
    @Column(name = "session_id", nullable = false)
    private String sessionId;
    /** Date de la séance, ISO AAAA-MM-JJ. */
    @Column(name = "date_seance", nullable = false)
    private String date;
    @Column(name = "contenu", length = 4000, nullable = false)
    private String contenu;
    @Column(name = "travail_a_faire", length = 4000)
    private String travailAFaire;
    @Column(name = "auteur_id")
    private String auteurId;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public String getTravailAFaire() { return travailAFaire; }
    public void setTravailAFaire(String travailAFaire) { this.travailAFaire = travailAFaire; }
    public String getAuteurId() { return auteurId; }
    public void setAuteurId(String auteurId) { this.auteurId = auteurId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
