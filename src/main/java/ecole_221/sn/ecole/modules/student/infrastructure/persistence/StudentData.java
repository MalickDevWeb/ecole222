package ecole_221.sn.ecole.modules.student.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "student_data")
public class StudentData {

    @Id
    private String studentId;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> profile;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> dashboard;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<Map<String, Object>> grades;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<Map<String, Object>> courses;

    public StudentData() {
    }

    public StudentData(String studentId, Map<String, Object> profile, Map<String, Object> dashboard, List<Map<String, Object>> grades, List<Map<String, Object>> courses) {
        this.studentId = studentId;
        this.profile = profile;
        this.dashboard = dashboard;
        this.grades = grades;
        this.courses = courses;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public Map<String, Object> getProfile() { return profile; }
    public void setProfile(Map<String, Object> profile) { this.profile = profile; }

    public Map<String, Object> getDashboard() { return dashboard; }
    public void setDashboard(Map<String, Object> dashboard) { this.dashboard = dashboard; }

    public List<Map<String, Object>> getGrades() { return grades; }
    public void setGrades(List<Map<String, Object>> grades) { this.grades = grades; }

    public List<Map<String, Object>> getCourses() { return courses; }
    public void setCourses(List<Map<String, Object>> courses) { this.courses = courses; }
}
