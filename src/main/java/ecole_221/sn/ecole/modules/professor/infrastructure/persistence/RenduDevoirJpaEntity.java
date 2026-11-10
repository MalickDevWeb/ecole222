package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * Rendu d'un élève pour un devoir de classe (table {@code homeworks}) et sa correction par le professeur.
 *
 * <p>Avant (lot B, 2026-11-10) : le dépôt élève ne conservait que le NOM du fichier, sur une ligne
 * {@code student_homeworks} que rien ne créait pour les devoirs du professeur ; le professeur ne voyait aucun rendu
 * et ne pouvait rien corriger. Une ligne par (école, devoir, élève) ; la correction (note /20 + commentaire)
 * est portée par la même ligne (elle peut exister sans fichier : devoir rendu sur papier ou noté non rendu).</p>
 */
@Entity
@Table(name = "rendus_devoirs",
        uniqueConstraints = @UniqueConstraint(name = "uk_rendu_devoir_eleve", columnNames = {"organization_id", "homework_id", "student_id"}),
        indexes = {@Index(name = "idx_rendu_devoir", columnList = "homework_id"), @Index(name = "idx_rendu_eleve", columnList = "student_id")})
public class RenduDevoirJpaEntity {

    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id")
    private String organizationId;

    @Id
    private String id;

    @Column(name = "homework_id", nullable = false)
    private String homeworkId;

    @Column(name = "course_id", nullable = false)
    private String courseId;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(name = "fichier_nom", length = 255)
    private String fichierNom;

    @Column(name = "fichier_url", length = 1000)
    private String fichierUrl;

    @Column(name = "fichier_type", length = 150)
    private String fichierType;

    @Column(name = "fichier_taille")
    private Long fichierTaille;

    @Column(name = "depose_le")
    private LocalDateTime deposeLe;

    @Column(name = "note")
    private Double note;

    @Column(name = "commentaire", length = 2000)
    private String commentaire;

    @Column(name = "corrige_le")
    private LocalDateTime corrigeLe;

    @Column(name = "corrige_par")
    private String corrigePar;

    public RenduDevoirJpaEntity() {}

    public RenduDevoirJpaEntity(String id, String homeworkId, String courseId, String studentId) {
        this.id = id;
        this.homeworkId = homeworkId;
        this.courseId = courseId;
        this.studentId = studentId;
    }

    public boolean estDepose() { return deposeLe != null; }
    public boolean estCorrige() { return note != null; }

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getHomeworkId() { return homeworkId; }
    public void setHomeworkId(String homeworkId) { this.homeworkId = homeworkId; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getFichierNom() { return fichierNom; }
    public void setFichierNom(String fichierNom) { this.fichierNom = fichierNom; }
    public String getFichierUrl() { return fichierUrl; }
    public void setFichierUrl(String fichierUrl) { this.fichierUrl = fichierUrl; }
    public String getFichierType() { return fichierType; }
    public void setFichierType(String fichierType) { this.fichierType = fichierType; }
    public Long getFichierTaille() { return fichierTaille; }
    public void setFichierTaille(Long fichierTaille) { this.fichierTaille = fichierTaille; }
    public LocalDateTime getDeposeLe() { return deposeLe; }
    public void setDeposeLe(LocalDateTime deposeLe) { this.deposeLe = deposeLe; }
    public Double getNote() { return note; }
    public void setNote(Double note) { this.note = note; }
    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
    public LocalDateTime getCorrigeLe() { return corrigeLe; }
    public void setCorrigeLe(LocalDateTime corrigeLe) { this.corrigeLe = corrigeLe; }
    public String getCorrigePar() { return corrigePar; }
    public void setCorrigePar(String corrigePar) { this.corrigePar = corrigePar; }
}
