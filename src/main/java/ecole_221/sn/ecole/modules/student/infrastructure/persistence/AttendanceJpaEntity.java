package ecole_221.sn.ecole.modules.student.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "attendances")
public class AttendanceJpaEntity {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "organization_id")
    private String organizationId;

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }


    @Id
    private String id;
    private String date;
    private String studentId;
    private String studentName;
    private String status;
    private String courseId;

    public AttendanceJpaEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    // --- Appel par séance (lot A, 2026-11-10) : colonnes nullables, les présences existantes restent valides ---
    /** Séance (schedule_sessions.id) à laquelle l'appel est rattaché ; null = appel « au jour » (ancien chemin). */
    @jakarta.persistence.Column(name = "session_id")
    private String sessionId;
    /** Observation facultative du professeur (ex. motif du retard). */
    @jakarta.persistence.Column(name = "note", length = 500)
    private String note;
    /** Horodatage serveur de la saisie (null pour les présences antérieures). */
    @jakarta.persistence.Column(name = "recorded_at")
    private java.time.LocalDateTime recordedAt;
    /** Compte qui a fait l'appel. */
    @jakarta.persistence.Column(name = "recorded_by")
    private String recordedBy;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public java.time.LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(java.time.LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
}
