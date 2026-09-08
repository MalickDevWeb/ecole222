package ecole_221.sn.ecole.modules.student.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_grades")
public class StudentGradeJpaEntity {

    @Id
    private String id;
    
    @Column(name = "student_id")
    private String studentId;
    
    @Column(name = "module")
    private String module;
    
    @Column(name = "professor")
    private String professor;
    
    @Column(name = "ects")
    private int ects;
    
    @Column(name = "cc_grade")
    private double ccGrade;
    
    @Column(name = "exam_grade")
    private double examGrade;
    
    @Column(name = "promo_average")
    private double promoAverage;
    
    @Column(name = "semester")
    private String semester;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public StudentGradeJpaEntity() {
    }

    public StudentGradeJpaEntity(String id, String studentId, String module, String professor, 
                                  int ects, double ccGrade, double examGrade, double promoAverage, String semester) {
        this();
        this.id = id;
        this.studentId = studentId;
        this.module = module;
        this.professor = professor;
        this.ects = ects;
        this.ccGrade = ccGrade;
        this.examGrade = examGrade;
        this.promoAverage = promoAverage;
        this.semester = semester;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    
    public String getProfessor() { return professor; }
    public void setProfessor(String professor) { this.professor = professor; }
    
    public int getEcts() { return ects; }
    public void setEcts(int ects) { this.ects = ects; }
    
    public double getCcGrade() { return ccGrade; }
    public void setCcGrade(double ccGrade) { this.ccGrade = ccGrade; }
    
    public double getExamGrade() { return examGrade; }
    public void setExamGrade(double examGrade) { this.examGrade = examGrade; }
    
    public double getPromoAverage() { return promoAverage; }
    public void setPromoAverage(double promoAverage) { this.promoAverage = promoAverage; }
    
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @jakarta.persistence.PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @jakarta.persistence.PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}