package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "grades")
public class GradeJpaEntity {

    @Id
    private String id;
    private String studentId;
    private String courseId;
    private Double cc;
    private Double examen;
    private Double average;

    public GradeJpaEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public Double getCc() { return cc; }
    public void setCc(Double cc) { this.cc = cc; }
    public Double getExamen() { return examen; }
    public void setExamen(Double examen) { this.examen = examen; }
    public Double getAverage() { return average; }
    public void setAverage(Double average) { this.average = average; }
}
