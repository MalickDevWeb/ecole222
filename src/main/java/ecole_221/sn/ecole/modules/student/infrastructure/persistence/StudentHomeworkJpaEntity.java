package ecole_221.sn.ecole.modules.student.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_homeworks")
public class StudentHomeworkJpaEntity {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "organization_id")
    private String organizationId;

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }


    @Id
    private String id;
    
    @Column(name = "student_id")
    private String studentId;
    
    @Column(name = "course_id")
    private String courseId;
    
    @Column(name = "statut")
    private String statut;
    
    @Column(name = "titre")
    private String titre;
    
    @Column(name = "deadline")
    private LocalDateTime deadline;
    
    @Column(name = "priority")
    private String priority;
    
    @Column(name = "progress")
    private int progress;
    
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @Column(name = "file_name")
    private String fileName;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public StudentHomeworkJpaEntity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.progress = 0;
    }

    public StudentHomeworkJpaEntity(String id, String studentId, String courseId, String statut, 
                                    String titre, LocalDateTime deadline, String priority) {
        this();
        this.id = id;
        this.studentId = studentId;
        this.courseId = courseId;
        this.statut = statut;
        this.titre = titre;
        this.deadline = deadline;
        this.priority = priority;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    @jakarta.persistence.PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }
}