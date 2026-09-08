package ecole_221.sn.ecole.modules.professor.domain.model;

import ecole_221.sn.ecole.modules.professor.domain.valueobject.Priority;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Agrégat racine représentant un devoir.
 * Constructeur privé pour garantir l'invariance - utiliser HomeworkFactory.
 */
public class Homework {
    
    private final String id;
    private final String courseId;
    private String title;
    private String description;
    private final Priority priority;
    private final LocalDate deadline;
    
    // Constructeur privé
    private Homework(String id, String courseId, String title, String description, 
                    Priority priority, LocalDate deadline) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.courseId = Objects.requireNonNull(courseId, "L'ID cours ne peut pas être null");
        this.title = Objects.requireNonNull(title, "Le titre ne peut pas être null");
        this.description = description;
        this.priority = Objects.requireNonNull(priority, "La priorité ne peut pas être null");
        this.deadline = Objects.requireNonNull(deadline, "La date limite ne peut pas être null");
    }

    // Méthode package-private pour la Factory
    static Homework createInternal(String id, String courseId, String title, String description, 
                                  Priority priority, LocalDate deadline) {
        return new Homework(id, courseId, title, description, priority, deadline);
    }
    
    // Getters
    public String getId() { return id; }
    public String getCourseId() { return courseId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Priority getPriority() { return priority; }
    public LocalDate getDeadline() { return deadline; }
    
    // Comportements métier
    public void updateTitle(String newTitle) {
        this.title = Objects.requireNonNull(newTitle, "Le titre ne peut pas être null");
    }
    
    public void updateDescription(String newDescription) {
        this.description = newDescription;
    }
    
    public boolean isOverdue() {
        return this.deadline.isBefore(LocalDate.now());
    }
    
    public boolean isDueToday() {
        return this.deadline.equals(LocalDate.now());
    }
    
    public boolean isUrgent() {
        return this.priority.equals(Priority.URGENT) || 
               (this.priority.equals(Priority.HIGH) && this.deadline.isBefore(LocalDate.now().plusDays(3)));
    }
    
    public boolean isHighPriority() {
        return this.priority.equals(Priority.HIGH) || this.priority.equals(Priority.URGENT);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Homework homework = (Homework) o;
        return Objects.equals(id, homework.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Homework{" +
                "id='" + id + '\'' +
                ", courseId='" + courseId + '\'' +
                ", title='" + title + '\'' +
                ", priority=" + priority +
                ", deadline=" + deadline +
                '}';
    }
}