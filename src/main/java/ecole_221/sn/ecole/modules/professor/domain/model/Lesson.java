package ecole_221.sn.ecole.modules.professor.domain.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Agrégat racine représentant une leçon.
 * Constructeur privé pour garantir l'invariance - utiliser LessonFactory.
 */
public class Lesson {
    
    private final String id;
    private final String courseId;
    private String title;
    private final int duration;
    private final LocalDate date;
    
    // Constructeur privé
    private Lesson(String id, String courseId, String title, int duration, LocalDate date) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.courseId = Objects.requireNonNull(courseId, "L'ID cours ne peut pas être null");
        this.title = Objects.requireNonNull(title, "Le titre ne peut pas être null");
        if (duration <= 0) {
            throw new IllegalArgumentException("La durée doit être positive");
        }
        this.duration = duration;
        this.date = Objects.requireNonNull(date, "La date ne peut pas être null");
    }

    // Méthode package-private pour la Factory
    static Lesson createInternal(String id, String courseId, String title, int duration, LocalDate date) {
        return new Lesson(id, courseId, title, duration, date);
    }
    
    // Getters
    public String getId() { return id; }
    public String getCourseId() { return courseId; }
    public String getTitle() { return title; }
    public int getDuration() { return duration; }
    public LocalDate getDate() { return date; }
    
    // Comportements métier
    public void updateTitle(String newTitle) {
        this.title = Objects.requireNonNull(newTitle, "Le titre ne peut pas être null");
    }
    
    public boolean isToday() {
        return this.date.equals(LocalDate.now());
    }
    
    public boolean isFuture() {
        return this.date.isAfter(LocalDate.now());
    }
    
    public boolean isPast() {
        return this.date.isBefore(LocalDate.now());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lesson lesson = (Lesson) o;
        return Objects.equals(id, lesson.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Lesson{" +
                "id='" + id + '\'' +
                ", courseId='" + courseId + '\'' +
                ", title='" + title + '\'' +
                ", duration=" + duration +
                ", date=" + date +
                '}';
    }
}