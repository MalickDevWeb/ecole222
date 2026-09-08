package ecole_221.sn.ecole.modules.student.domain.model;

import ecole_221.sn.ecole.modules.student.domain.valueobject.CourseStatus;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Agrégat racine représentant un cours d'étudiant.
 * Constructeur privé pour garantir l'invariance - utiliser StudentCourseFactory.
 */
public class StudentCourse {
    
    private final String id;
    private final String studentId;
    private final String courseId;
    private String titre;
    private final int coefficient;
    private int progress;
    private CourseStatus statut;
    private final String semester;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructeur privé
    private StudentCourse(String id, String studentId, String courseId, String titre,
                        int coefficient, int progress, CourseStatus statut, String semester,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.studentId = Objects.requireNonNull(studentId, "L'ID étudiant ne peut pas être null");
        this.courseId = Objects.requireNonNull(courseId, "L'ID cours ne peut pas être null");
        this.titre = titre;
        if (coefficient <= 0) {
            throw new IllegalArgumentException("Le coefficient doit être positif");
        }
        this.coefficient = coefficient;
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("Le progrès doit être entre 0 et 100");
        }
        this.progress = progress;
        this.statut = Objects.requireNonNull(statut, "Le statut ne peut pas être null");
        this.semester = Objects.requireNonNull(semester, "Le semestre ne peut pas être null");
        this.createdAt = Objects.requireNonNull(createdAt, "La date de création ne peut pas être null");
        this.updatedAt = updatedAt;
    }

    // Méthode package-private pour la Factory
    static StudentCourse createInternal(String id, String studentId, String courseId, String titre,
                                       int coefficient, int progress, CourseStatus statut, String semester,
                                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new StudentCourse(id, studentId, courseId, titre, coefficient, progress, statut, semester, createdAt, updatedAt);
    }
    
    // Getters
    public String getId() { return id; }
    public String getStudentId() { return studentId; }
    public String getCourseId() { return courseId; }
    public String getTitre() { return titre; }
    public int getCoefficient() { return coefficient; }
    public int getProgress() { return progress; }
    public CourseStatus getStatut() { return statut; }
    public String getSemester() { return semester; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    
    // Comportements métier
    public void updateTitre(String newTitre) {
        this.titre = newTitre;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateProgress(int newProgress) {
        if (newProgress < 0 || newProgress > 100) {
            throw new IllegalArgumentException("Le progrès doit être entre 0 et 100");
        }
        this.progress = newProgress;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void incrementProgress(int amount) {
        int newProgress = this.progress + amount;
        if (newProgress > 100) {
            newProgress = 100;
        }
        updateProgress(newProgress);
    }
    
    public void markAsInProgress() {
        this.statut = CourseStatus.EN_COURS;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void markAsCompleted() {
        this.statut = CourseStatus.TERMINE;
        this.progress = 100;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void markAsNotStarted() {
        this.statut = CourseStatus.A_COMMENCER;
        this.progress = 0;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void markAsAbandoned() {
        this.statut = CourseStatus.ABANDONNE;
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isCompleted() {
        return this.statut.equals(CourseStatus.TERMINE) || this.progress == 100;
    }
    
    public boolean isInProgress() {
        return this.statut.equals(CourseStatus.EN_COURS);
    }
    
    public boolean isNotStarted() {
        return this.statut.equals(CourseStatus.A_COMMENCER) || this.progress == 0;
    }
    
    public boolean isAbandoned() {
        return this.statut.equals(CourseStatus.ABANDONNE);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StudentCourse that = (StudentCourse) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "StudentCourse{" +
                "id='" + id + '\'' +
                ", courseId='" + courseId + '\'' +
                ", titre='" + titre + '\'' +
                ", progress=" + progress +
                ", statut=" + statut +
                ", semester='" + semester + '\'' +
                '}';
    }
}