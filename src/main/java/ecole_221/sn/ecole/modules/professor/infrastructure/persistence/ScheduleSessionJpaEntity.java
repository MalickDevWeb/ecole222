package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "schedule_sessions")
public class ScheduleSessionJpaEntity {

    @Id
    private String id;
    
    @Column(name = "course_title")
    private String courseTitle;
    
    @Column(name = "day")
    private String day;
    
    @Column(name = "time")
    private String time;
    
    @Column(name = "room")
    private String room;
    
    @Column(name = "course_id")
    private String courseId;
    
    @Column(name = "type")
    private String type; // 'CM', 'TD', 'TP'
    
    @Column(name = "date_str")
    private String dateStr;
    
    @Column(name = "status")
    private String status; // 'a_venir', 'annule', 'reporte', 'termine'
    
    @Column(name = "classe")
    private String classe;
    
    @Column(name = "professor_id")
    private String professorId;
    
    @Column(name = "cancellation_reason")
    private String cancellationReason;
    
    @Column(name = "original_date")
    private LocalDateTime originalDate;
    
    @Column(name = "rescheduled_to")
    private LocalDateTime rescheduledTo;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ScheduleSessionJpaEntity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "a_venir";
    }

    public ScheduleSessionJpaEntity(String id, String courseTitle, String day, String time, String room, 
                                    String courseId, String type, String dateStr, String classe, String professorId) {
        this();
        this.id = id;
        this.courseTitle = courseTitle;
        this.day = day;
        this.time = time;
        this.room = room;
        this.courseId = courseId;
        this.type = type;
        this.dateStr = dateStr;
        this.classe = classe;
        this.professorId = professorId;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
    
    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }
    
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getDateStr() { return dateStr; }
    public void setDateStr(String dateStr) { this.dateStr = dateStr; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getClasse() { return classe; }
    public void setClasse(String classe) { this.classe = classe; }
    
    public String getProfessorId() { return professorId; }
    public void setProfessorId(String professorId) { this.professorId = professorId; }
    
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    
    public LocalDateTime getOriginalDate() { return originalDate; }
    public void setOriginalDate(LocalDateTime originalDate) { this.originalDate = originalDate; }
    
    public LocalDateTime getRescheduledTo() { return rescheduledTo; }
    public void setRescheduledTo(LocalDateTime rescheduledTo) { this.rescheduledTo = rescheduledTo; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @jakarta.persistence.PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}