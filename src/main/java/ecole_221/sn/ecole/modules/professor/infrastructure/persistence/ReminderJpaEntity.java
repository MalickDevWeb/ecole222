package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "reminders")
public class ReminderJpaEntity {

    @Id
    private String id;
    
    @Column(name = "content")
    private String content;
    
    @Column(name = "is_urgent")
    private Boolean isUrgent;
    
    @Column(name = "course_id")
    private String courseId;
    
    @Column(name = "professor_id")
    private String professorId;
    
    @Column(name = "due_date")
    private LocalDateTime dueDate;
    
    @Column(name = "is_completed")
    private Boolean isCompleted;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ReminderJpaEntity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isUrgent = false;
        this.isCompleted = false;
    }

    public ReminderJpaEntity(String id, String content, Boolean isUrgent, String courseId, String professorId, LocalDateTime dueDate) {
        this();
        this.id = id;
        this.content = content;
        this.isUrgent = isUrgent;
        this.courseId = courseId;
        this.professorId = professorId;
        this.dueDate = dueDate;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public Boolean getIsUrgent() { return isUrgent; }
    public void setIsUrgent(Boolean isUrgent) { this.isUrgent = isUrgent; }
    
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    
    public String getProfessorId() { return professorId; }
    public void setProfessorId(String professorId) { this.professorId = professorId; }
    
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    
    public Boolean getIsCompleted() { return isCompleted; }
    public void setIsCompleted(Boolean isCompleted) { this.isCompleted = isCompleted; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @jakarta.persistence.PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}