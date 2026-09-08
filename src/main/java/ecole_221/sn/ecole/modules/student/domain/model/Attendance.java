package ecole_221.sn.ecole.modules.student.domain.model;

import ecole_221.sn.ecole.modules.student.domain.valueobject.AttendanceStatus;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Agrégat racine représentant une présence.
 * Constructeur privé pour garantir l'invariance - utiliser AttendanceFactory.
 */
public class Attendance {
    
    private final String id;
    private final LocalDate date;
    private final String studentId;
    private final String studentName;
    private AttendanceStatus status;
    private final String courseId;
    
    // Constructeur privé
    private Attendance(String id, LocalDate date, String studentId, String studentName,
                     AttendanceStatus status, String courseId) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.date = Objects.requireNonNull(date, "La date ne peut pas être null");
        this.studentId = Objects.requireNonNull(studentId, "L'ID étudiant ne peut pas être null");
        this.studentName = Objects.requireNonNull(studentName, "Le nom de l'étudiant ne peut pas être null");
        this.status = Objects.requireNonNull(status, "Le statut ne peut pas être null");
        this.courseId = courseId;
    }

    // Méthode package-private pour la Factory
    static Attendance createInternal(String id, LocalDate date, String studentId, String studentName,
                                    AttendanceStatus status, String courseId) {
        return new Attendance(id, date, studentId, studentName, status, courseId);
    }
    
    // Getters
    public String getId() { return id; }
    public LocalDate getDate() { return date; }
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public AttendanceStatus getStatus() { return status; }
    public String getCourseId() { return courseId; }
    
    // Comportements métier
    public void markAsPresent() {
        this.status = AttendanceStatus.PRESENT;
    }
    
    public void markAsAbsent() {
        this.status = AttendanceStatus.ABSENT;
    }
    
    public void markAsLate() {
        this.status = AttendanceStatus.LATE;
    }
    
    public void markAsExcused() {
        this.status = AttendanceStatus.EXCUSED;
    }
    
    public boolean isPresent() {
        return this.status.equals(AttendanceStatus.PRESENT);
    }
    
    public boolean isAbsent() {
        return this.status.equals(AttendanceStatus.ABSENT);
    }
    
    public boolean isLate() {
        return this.status.equals(AttendanceStatus.LATE);
    }
    
    public boolean isExcused() {
        return this.status.equals(AttendanceStatus.EXCUSED);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Attendance attendance = (Attendance) o;
        return Objects.equals(id, attendance.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Attendance{" +
                "id='" + id + '\'' +
                ", date=" + date +
                ", studentId='" + studentId + '\'' +
                ", studentName='" + studentName + '\'' +
                ", status=" + status +
                ", courseId='" + courseId + '\'' +
                '}';
    }
}