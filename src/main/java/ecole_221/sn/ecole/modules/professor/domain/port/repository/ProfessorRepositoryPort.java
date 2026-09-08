package ecole_221.sn.ecole.modules.professor.domain.port.repository;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.professor.domain.model.Course;
import ecole_221.sn.ecole.partage.PageResult;
import java.util.List;
import java.util.Map;

public interface ProfessorRepositoryPort {
    PageResult<Course> getCoursesByProfessor(String profId, int page, int size);
    List<User> getStudentsByClass(String classe);
    
    List<Map<String, Object>> getGradesByCourse(String courseId);
    void saveGrade(String studentId, String courseId, Double cc, Double exam);
    Map<String, Object> getGrade(String studentId, String courseId);
    
    void saveHomework(String courseId, String title, String desc, String prio, String deadline);
    PageResult<Map<String, Object>> getHomeworksByCourse(String courseId, int page, int size);
    
    void saveLesson(String courseId, String title, int duration, String date);
    PageResult<Map<String, Object>> getLessonsByCourse(String courseId, int page, int size);
    
    void saveModule(String courseId, String name);
    PageResult<Map<String, Object>> getModulesByCourse(String courseId, int page, int size);
    
    void saveQuiz(String courseId, String title, int duration);
    PageResult<Map<String, Object>> getQuizzesByCourse(String courseId, int page, int size);
    
    void saveAttendance(String date, String studentId, String studentName, String status, String courseId);
    PageResult<Map<String, Object>> getAttendancesByCourse(String courseId, int page, int size);
}
