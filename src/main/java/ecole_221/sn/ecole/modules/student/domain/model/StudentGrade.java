package ecole_221.sn.ecole.modules.student.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Agrégat racine représentant une note d'étudiant.
 * Constructeur privé pour garantir l'invariance - utiliser StudentGradeFactory.
 */
public class StudentGrade {
    
    private final String id;
    private final String studentId;
    private String module;
    private String professor;
    private final int ects;
    private double ccGrade;
    private double examGrade;
    private double promoAverage;
    private final String semester;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructeur privé
    private StudentGrade(String id, String studentId, String module, String professor,
                        int ects, double ccGrade, double examGrade, double promoAverage,
                        String semester, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.studentId = Objects.requireNonNull(studentId, "L'ID étudiant ne peut pas être null");
        this.module = module;
        this.professor = professor;
        if (ects <= 0) {
            throw new IllegalArgumentException("Les ECTS doivent être positifs");
        }
        this.ects = ects;
        this.ccGrade = ccGrade;
        this.examGrade = examGrade;
        this.promoAverage = promoAverage;
        this.semester = Objects.requireNonNull(semester, "Le semestre ne peut pas être null");
        this.createdAt = Objects.requireNonNull(createdAt, "La date de création ne peut pas être null");
        this.updatedAt = updatedAt;

        // Validation des notes
        if (ccGrade < 0 || ccGrade > 20) {
            throw new IllegalArgumentException("La note CC doit être entre 0 et 20");
        }
        if (examGrade < 0 || examGrade > 20) {
            throw new IllegalArgumentException("La note d'examen doit être entre 0 et 20");
        }
    }

    // Méthode package-private pour la Factory
    static StudentGrade createInternal(String id, String studentId, String module, String professor,
                                      int ects, double ccGrade, double examGrade, double promoAverage,
                                      String semester, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new StudentGrade(id, studentId, module, professor, ects, ccGrade, examGrade, promoAverage, semester, createdAt, updatedAt);
    }
    
    // Getters
    public String getId() { return id; }
    public String getStudentId() { return studentId; }
    public String getModule() { return module; }
    public String getProfessor() { return professor; }
    public int getEcts() { return ects; }
    public double getCcGrade() { return ccGrade; }
    public double getExamGrade() { return examGrade; }
    public double getPromoAverage() { return promoAverage; }
    public String getSemester() { return semester; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    
    // Comportements métier
    public void updateModule(String newModule) {
        this.module = newModule;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateProfessor(String newProfessor) {
        this.professor = newProfessor;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateCcGrade(double newCcGrade) {
        if (newCcGrade < 0 || newCcGrade > 20) {
            throw new IllegalArgumentException("La note CC doit être entre 0 et 20");
        }
        this.ccGrade = newCcGrade;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateExamGrade(double newExamGrade) {
        if (newExamGrade < 0 || newExamGrade > 20) {
            throw new IllegalArgumentException("La note d'examen doit être entre 0 et 20");
        }
        this.examGrade = newExamGrade;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updatePromoAverage(double newPromoAverage) {
        this.promoAverage = newPromoAverage;
        this.updatedAt = LocalDateTime.now();
    }
    
    public double calculateFinalGrade() {
        // Formule : 40% CC + 60% Examen
        return (ccGrade * 0.4) + (examGrade * 0.6);
    }
    
    public boolean hasPassed() {
        return calculateFinalGrade() >= 10;
    }
    
    public boolean hasHonors() {
        return calculateFinalGrade() >= 16;
    }
    
    public boolean hasMention() {
        double finalGrade = calculateFinalGrade();
        return finalGrade >= 12 && finalGrade < 16;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StudentGrade that = (StudentGrade) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "StudentGrade{" +
                "id='" + id + '\'' +
                ", studentId='" + studentId + '\'' +
                ", module='" + module + '\'' +
                ", finalGrade=" + calculateFinalGrade() +
                ", semester='" + semester + '\'' +
                '}';
    }
}