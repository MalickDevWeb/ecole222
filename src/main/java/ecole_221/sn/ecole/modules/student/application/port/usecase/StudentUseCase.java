package ecole_221.sn.ecole.modules.student.application.port.usecase;

import java.util.Map;
import ecole_221.sn.ecole.partage.PageResult;

public interface StudentUseCase {
    Map<String, Object> getStudentProfile(String studentId);
    Map<String, Object> getStudentDashboard(String studentId);
    Map<String, Object> getDigitalBadge(String studentId);
    boolean validateQrToken(String matricule, long minuteEpoch, String token);
    PageResult<Map<String, Object>> getStudentAttendances(String studentId, int page, int size);
    void submitSelfAttendance(String studentId, String type, String method);
    
    PageResult<Map<String, Object>> getActiveLiveSessions(int page, int size);
    void submitReaction(String sessionId, String reactionType);
    
    // New methods for student data
    java.util.List<Map<String, Object>> getStudentGrades(String studentId);
    java.util.List<Map<String, Object>> getStudentCourses(String studentId);
    java.util.List<Map<String, Object>> getStudentHomeworks(String studentId);
    java.util.List<Map<String, Object>> getLiveChatMessages(String sessionId);
    void saveLiveChatMessage(String sessionId, String userId, String userName, String userRole, String text);
    void updateHomeworkProgress(String homeworkId, int progress);
    void submitHomework(String homeworkId, String fileName);
}
