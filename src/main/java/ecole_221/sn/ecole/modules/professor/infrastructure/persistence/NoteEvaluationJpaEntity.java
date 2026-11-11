package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Note d'un élève à une évaluation : une valeur (0..noteSur) OU une absence (ABS non justifiée,
 * EXCUSE justifiée), avec un commentaire facultatif. Une seule note par élève et par évaluation.
 */
@Entity
@Table(name = "notes_evaluations",
        uniqueConstraints = @UniqueConstraint(name = "uk_note_evaluation_eleve", columnNames = {"organization_id", "evaluation_id", "student_id"}),
        indexes = {@Index(name = "idx_notes_evaluations_eval", columnList = "evaluation_id"),
                   @Index(name = "idx_notes_evaluations_cours", columnList = "course_id")})
public class NoteEvaluationJpaEntity {

    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id")
    private String organizationId;

    @Id
    private String id;
    @Column(name = "evaluation_id", nullable = false)
    private String evaluationId;
    @Column(name = "course_id", nullable = false)
    private String courseId;
    @Column(name = "student_id", nullable = false)
    private String studentId;
    private Double valeur;
    /** ABS | EXCUSE | null */
    @Column(length = 10)
    private String absence;
    @Column(length = 500)
    private String commentaire;
    @Column(name = "modifie_par")
    private String modifiePar;
    @Column(name = "modifie_le")
    private LocalDateTime modifieLe;

    public NoteEvaluationJpaEntity() {}

    public String getOrganizationId() { return organizationId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEvaluationId() { return evaluationId; }
    public void setEvaluationId(String evaluationId) { this.evaluationId = evaluationId; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public Double getValeur() { return valeur; }
    public void setValeur(Double valeur) { this.valeur = valeur; }
    public String getAbsence() { return absence; }
    public void setAbsence(String absence) { this.absence = absence; }
    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
    public String getModifiePar() { return modifiePar; }
    public void setModifiePar(String modifiePar) { this.modifiePar = modifiePar; }
    public LocalDateTime getModifieLe() { return modifieLe; }
    public void setModifieLe(LocalDateTime modifieLe) { this.modifieLe = modifieLe; }
}
