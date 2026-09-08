package ecole_221.sn.ecole.modules.professor.application.port.usecase;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.professor.application.port.dto.GradeDto;
import ecole_221.sn.ecole.modules.professor.domain.model.Course;
import ecole_221.sn.ecole.partage.PageResult;
import java.util.List;
import java.util.Map;

public interface ProfessorUseCase {
    PageResult<Course> getCourses(String profId, int page, int size);
    List<User> getStudents(String classe);
    List<GradeDto> getCourseGrades(String courseId);
    void saveGrade(String studentId, String courseId, Double cc, Double exam);
    Map<String, Object> getGrade(String studentId, String courseId);
    void saveHomework(String courseId, String title, String desc, String prio, String deadline);
    PageResult<Map<String, Object>> getHomeworks(String courseId, int page, int size);
    void saveLesson(String courseId, String title, int duration, String date);
    PageResult<Map<String, Object>> getLessons(String courseId, int page, int size);
    void saveModule(String courseId, String name);
    PageResult<Map<String, Object>> getModules(String courseId, int page, int size);
    void saveQuiz(String courseId, String title, int duration);
    PageResult<Map<String, Object>> getQuizzes(String courseId, int page, int size);
    void submitAttendance(String date, String studentId, String studentName, String status, String courseId);
    PageResult<Map<String, Object>> getAttendances(String courseId, int page, int size);
    
    // New methods for reminders and schedule
    List<Map<String, Object>> getReminders(String courseId);
    void saveReminder(String courseId, String content, Boolean isUrgent, String professorId);
    List<Map<String, Object>> getSchedule(String professorId);
    void cancelScheduleSession(String sessionId, String reason);
    void rescheduleScheduleSession(String sessionId, String day, String time, String room);
}
