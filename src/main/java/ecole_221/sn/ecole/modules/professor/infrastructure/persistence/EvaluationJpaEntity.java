package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Évaluation d'un cours (devoir surveillé, examen, TP, oral…) : plusieurs par cours, chacune avec son
 * coefficient, son barème (noteSur) et sa session (NORMALE ou RATTRAPAGE). Filtrée par école (@TenantId).
 * Coexiste avec l'ancien couple CC / Examen (GradeJpaEntity, table « grades ») sans le modifier.
 */
@Entity
@Table(name = "evaluations_cours", indexes = @Index(name = "idx_evaluations_cours_cours", columnList = "course_id"))
public class EvaluationJpaEntity {

    @org.hibernate.annotations.TenantId
    @Column(name = "organization_id")
    private String organizationId;

    @Id
    private String id;
    @Column(name = "course_id", nullable = false)
    private String courseId;
    @Column(nullable = false, length = 150)
    private String titre;
    /** DEVOIR_SURVEILLE | EXAMEN | TP | ORAL | AUTRE */
    @Column(nullable = false, length = 30)
    private String type;
    @Column(name = "date_evaluation", nullable = false)
    private LocalDate dateEvaluation;
    @Column(nullable = false)
    private double coefficient;
    @Column(name = "note_sur", nullable = false)
    private double noteSur;
    /** NORMALE | RATTRAPAGE */
    @Column(nullable = false, length = 20)
    private String session;
    /** Pour une évaluation de RATTRAPAGE : l'évaluation normale qu'elle rattrape. */
    @Column(name = "evaluation_rattrapee_id")
    private String evaluationRattrapeeId;
    @Column(nullable = false)
    private boolean verrouillee;
    @Column(name = "verrouillee_par")
    private String verrouilleePar;
    @Column(name = "verrouillee_le")
    private LocalDateTime verrouilleeLe;
    @Column(name = "cree_par")
    private String creePar;
    @Column(name = "cree_le")
    private LocalDateTime creeLe;

    public EvaluationJpaEntity() {}

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public LocalDate getDateEvaluation() { return dateEvaluation; }
    public void setDateEvaluation(LocalDate dateEvaluation) { this.dateEvaluation = dateEvaluation; }
    public double getCoefficient() { return coefficient; }
    public void setCoefficient(double coefficient) { this.coefficient = coefficient; }
    public double getNoteSur() { return noteSur; }
    public void setNoteSur(double noteSur) { this.noteSur = noteSur; }
    public String getSession() { return session; }
    public void setSession(String session) { this.session = session; }
    public String getEvaluationRattrapeeId() { return evaluationRattrapeeId; }
    public void setEvaluationRattrapeeId(String evaluationRattrapeeId) { this.evaluationRattrapeeId = evaluationRattrapeeId; }
    public boolean isVerrouillee() { return verrouillee; }
    public void setVerrouillee(boolean verrouillee) { this.verrouillee = verrouillee; }
    public String getVerrouilleePar() { return verrouilleePar; }
    public void setVerrouilleePar(String verrouilleePar) { this.verrouilleePar = verrouilleePar; }
    public LocalDateTime getVerrouilleeLe() { return verrouilleeLe; }
    public void setVerrouilleeLe(LocalDateTime verrouilleeLe) { this.verrouilleeLe = verrouilleeLe; }
    public String getCreePar() { return creePar; }
    public void setCreePar(String creePar) { this.creePar = creePar; }
    public LocalDateTime getCreeLe() { return creeLe; }
    public void setCreeLe(LocalDateTime creeLe) { this.creeLe = creeLe; }
}
