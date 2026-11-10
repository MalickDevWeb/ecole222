package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Tentative d'un élève à un quiz QCM : réponses données (index par question), score calculé PAR LE SERVEUR.
 * Les tentatives sont des données d'élèves : elles ne sont jamais supprimées sans demande explicite.
 */
@Entity
@Table(name = "quiz_tentatives", indexes = {
        @Index(name = "idx_quiz_tentatives_quiz", columnList = "quiz_id"),
        @Index(name = "idx_quiz_tentatives_quiz_eleve", columnList = "quiz_id, student_id")
})
public class TentativeQuizJpaEntity {
    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id")
    private String organizationId;

    @Id
    private String id;
    @Column(name = "quiz_id", nullable = false)
    private String quizId;
    @Column(name = "course_id")
    private String courseId;
    @Column(name = "student_id", nullable = false)
    private String studentId;
    /** Index choisi pour chaque question, dans l'ordre des questions du quiz (JSON : [1,0,2]). */
    @Column(name = "reponses_json", columnDefinition = "TEXT", nullable = false)
    private String reponsesJson;
    /** Nombre de bonnes réponses. */
    @Column(nullable = false)
    private int score;
    /** Nombre de questions au moment de la tentative. */
    @Column(nullable = false)
    private int total;
    @Column(name = "date_tentative", nullable = false)
    private LocalDateTime dateTentative;

    public TentativeQuizJpaEntity() {}

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getQuizId() { return quizId; }
    public void setQuizId(String quizId) { this.quizId = quizId; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getReponsesJson() { return reponsesJson; }
    public void setReponsesJson(String reponsesJson) { this.reponsesJson = reponsesJson; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public LocalDateTime getDateTentative() { return dateTentative; }
    public void setDateTentative(LocalDateTime dateTentative) { this.dateTentative = dateTentative; }
}
