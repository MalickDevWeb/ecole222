package ecole_221.sn.ecole.modules.student.application.port.usecase;

import java.util.Map;
import ecole_221.sn.ecole.partage.PageResult;

public interface StudentUseCase {
    Map<String, Object> getStudentProfile(String studentId);
    Map<String, Object> getStudentDashboard(String studentId);
    /** Badge QR de l'élève à partir de son matricule RÉEL (avant : matricule inventé « SN-2026-<hash> »). */
    Map<String, Object> getDigitalBadge(String studentId, String matricule, String nomComplet, String classe);
    boolean validateQrToken(String matricule, long minuteEpoch, String token);
    PageResult<Map<String, Object>> getStudentAttendances(String studentId, int page, int size);
    /** Enregistre l'auto-pointage et renvoie l'identifiant RÉEL de la présence créée. */
    String submitSelfAttendance(String studentId, String type, String method);
    
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
