package ecole_221.sn.ecole.modules.student.application.service;

import org.springframework.stereotype.Service;
import ecole_221.sn.ecole.modules.student.application.port.usecase.StudentUseCase;
import ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence.MeetJpaRepository;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaEntity;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaRepository;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.StudentDataRepository;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.StudentGradeJpaRepository;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.StudentCourseJpaRepository;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.StudentHomeworkJpaRepository;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.LiveChatMessageJpaRepository;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.StudentGradeJpaEntity;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.StudentCourseJpaEntity;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.StudentHomeworkJpaEntity;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.LiveChatMessageJpaEntity;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.*;
import ecole_221.sn.ecole.partage.PageResult;

@Service
public class StudentService implements StudentUseCase {

    private final StudentDataRepository studentDataRepository;
    private final AttendanceJpaRepository attendanceJpaRepository;
    private final MeetJpaRepository meetJpaRepository;
    private final StudentGradeJpaRepository studentGradeRepository;
    private final StudentCourseJpaRepository studentCourseRepository;
    private final StudentHomeworkJpaRepository studentHomeworkRepository;
    private final LiveChatMessageJpaRepository liveChatMessageRepository;

    public StudentService(
            StudentDataRepository studentDataRepository,
            AttendanceJpaRepository attendanceJpaRepository,
            MeetJpaRepository meetJpaRepository,
            StudentGradeJpaRepository studentGradeRepository,
            StudentCourseJpaRepository studentCourseRepository,
            StudentHomeworkJpaRepository studentHomeworkRepository,
            LiveChatMessageJpaRepository liveChatMessageRepository) {
        this.studentDataRepository = studentDataRepository;
        this.attendanceJpaRepository = attendanceJpaRepository;
        this.meetJpaRepository = meetJpaRepository;
        this.studentGradeRepository = studentGradeRepository;
        this.studentCourseRepository = studentCourseRepository;
        this.studentHomeworkRepository = studentHomeworkRepository;
        this.liveChatMessageRepository = liveChatMessageRepository;
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "studentProfile", key = "#studentId")
    public Map<String, Object> getStudentProfile(String studentId) {
        return studentDataRepository.findById(studentId)
            .map(data -> new HashMap<>(data.getProfile()))
            .orElseGet(HashMap::new);
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "studentDashboard", key = "#studentId")
    public Map<String, Object> getStudentDashboard(String studentId) {
        return studentDataRepository.findById(studentId)
            .map(data -> new HashMap<>(data.getDashboard()))
            .orElseGet(HashMap::new);
    }

    @Override
    public Map<String, Object> getDigitalBadge(String studentId) {
        long minuteEpoch = System.currentTimeMillis() / 60000;
        String matricule = "SN-2026-" + studentId.hashCode() % 10000;
        String token = generateHmacToken(matricule, minuteEpoch);
        String qrPayload = "STU_SECURE:" + matricule + ":" + minuteEpoch + ":" + token;

        Map<String, Object> badge = new HashMap<>();
        Map<String, Object> profile = getStudentProfile(studentId);
        badge.put("id", "badge-" + studentId);
        badge.put("studentId", studentId);
        badge.put("studentName", profile.getOrDefault("prenom", "") + " " + profile.getOrDefault("nom", ""));
        badge.put("matricule", matricule);
        Object classe = profile.get("classe");
        Object classeName = "Non assigné";
        if (classe instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> classeMap = (Map<String, Object>) classe;
            classeName = classeMap.getOrDefault("name", "Non assigné");
        }
        badge.put("classe", String.valueOf(classeName));
        badge.put("qrPayload", qrPayload);
        badge.put("token", token);
        badge.put("minuteEpoch", minuteEpoch);
        badge.put("expiresInSeconds", 60);
        badge.put("status", "ACTIF");
        return badge;
    }

    /**
     * Vérifie la validité d'un token QR (appelé par le vigile lors du scan).
     */
    @Override
    public boolean validateQrToken(String matricule, long minuteEpoch, String token) {
        // Tolérance : minute actuelle ± 1
        long now = System.currentTimeMillis() / 60000;
        for (long m = minuteEpoch - 1; m <= minuteEpoch + 1; m++) {
            if (generateHmacToken(matricule, m).equals(token)) {
                return Math.abs(now - m) <= 2; // max 2 minutes de décalage
            }
        }
        return false;
    }

    private String generateHmacToken(String matricule, long minuteEpoch) {
        String input = matricule + ":" + minuteEpoch + ":ecole221-server-secret-key";
        // FNV-1a 32-bit (même algo que le frontend pour compatibilité)
        int h = 0x811c9dc5;
        for (int i = 0; i < input.length(); i++) {
            h ^= input.charAt(i);
            h *= 0x01000193;
        }
        return Integer.toHexString(h >>> 0).toUpperCase();
    }

    @Override
    public PageResult<Map<String, Object>> getStudentAttendances(String studentId, int page, int size) {
        var springPage = attendanceJpaRepository.findByStudentId(studentId, PageRequest.of(page, size));
        List<Map<String, Object>> content = springPage.getContent().stream().map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("date", a.getDate());
            map.put("status", a.getStatus());
            map.put("studentId", a.getStudentId());
            map.put("studentName", a.getStudentName());
            map.put("courseId", a.getCourseId());
            return map;
        }).toList();
        return new PageResult<>(content, springPage.getNumber(), springPage.getSize(), springPage.getTotalElements(), springPage.getTotalPages());
    }

    @Override
    public void submitSelfAttendance(String studentId, String type, String method) {
        AttendanceJpaEntity attendance = new AttendanceJpaEntity();
        attendance.setId("att-" + UUID.randomUUID().toString().substring(0, 8));
        attendance.setDate(LocalDateTime.now().toString());
        attendance.setStatus(type == null ? "PRESENT" : type);
        attendance.setStudentId(studentId);
        attendance.setStudentName(String.valueOf(getStudentProfile(studentId).getOrDefault("prenom", "")) + " " + getStudentProfile(studentId).getOrDefault("nom", ""));
        attendance.setCourseId(method == null ? "self-check" : method);
        attendanceJpaRepository.save(attendance);
    }

    @Override
    public PageResult<Map<String, Object>> getActiveLiveSessions(int page, int size) { 
        List<Map<String, Object>> sessions = meetJpaRepository.findAll().stream().map(meet -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", meet.getId());
            map.put("courseTitle", meet.getTitle());
            map.put("professorName", "Professeur");
            map.put("status", "ACTIVE");
            map.put("activeUsers", 0);
            map.put("link", meet.getLink());
            map.put("time", meet.getTime());
            map.put("reactions", Map.of());
            return map;
        }).toList();
        return new PageResult<>(sessions, page, size, sessions.size(), sessions.isEmpty() ? 0 : 1);
    }

    @Override
    public void submitReaction(String sessionId, String reactionType) {
        String fallbackId = sessionId != null && sessionId.startsWith("session-")
            ? "meet-" + sessionId.substring("session-".length())
            : sessionId;

        boolean exists = meetJpaRepository.findById(sessionId).isPresent()
            || (fallbackId != null && meetJpaRepository.findById(fallbackId).isPresent());

        if (!exists) {
            throw new IllegalArgumentException("Session live introuvable: " + sessionId);
        }
    }

    // New methods for student data
    @Override
    public List<Map<String, Object>> getStudentGrades(String studentId) {
        List<StudentGradeJpaEntity> grades = studentGradeRepository.findByStudentId(studentId);
        return grades.stream()
            .map(this::gradeToMap)
            .toList();
    }

    @Override
    public List<Map<String, Object>> getStudentCourses(String studentId) {
        List<StudentCourseJpaEntity> courses = studentCourseRepository.findByStudentId(studentId);
        return courses.stream()
            .map(this::courseToMap)
            .toList();
    }

    @Override
    public List<Map<String, Object>> getStudentHomeworks(String studentId) {
        List<StudentHomeworkJpaEntity> homeworks = studentHomeworkRepository.findByStudentId(studentId);
        return homeworks.stream()
            .map(this::homeworkToMap)
            .toList();
    }

    @Override
    public List<Map<String, Object>> getLiveChatMessages(String sessionId) {
        List<LiveChatMessageJpaEntity> messages = liveChatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        return messages.stream()
            .map(this::chatMessageToMap)
            .toList();
    }

    @Override
    public void saveLiveChatMessage(String sessionId, String userId, String userName, String userRole, String text) {
        LiveChatMessageJpaEntity message = new LiveChatMessageJpaEntity(
            "msg-" + UUID.randomUUID().toString().substring(0, 8),
            sessionId,
            userId,
            userName,
            userRole,
            text
        );
        liveChatMessageRepository.save(message);
    }

    @Override
    public void updateHomeworkProgress(String homeworkId, int progress) {
        StudentHomeworkJpaEntity homework = studentHomeworkRepository.findById(homeworkId)
            .orElseThrow(() -> new IllegalArgumentException("Devoir introuvable: " + homeworkId));
        homework.setProgress(progress);
        studentHomeworkRepository.save(homework);
    }

    @Override
    public void submitHomework(String homeworkId, String fileName) {
        StudentHomeworkJpaEntity homework = studentHomeworkRepository.findById(homeworkId)
            .orElseThrow(() -> new IllegalArgumentException("Devoir introuvable: " + homeworkId));
        homework.setStatut("soumis");
        homework.setSubmittedAt(LocalDateTime.now());
        homework.setFileName(fileName);
        homework.setProgress(100);
        studentHomeworkRepository.save(homework);
    }

    // Helper methods for entity mapping
    private Map<String, Object> gradeToMap(StudentGradeJpaEntity grade) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", grade.getId());
        map.put("module", grade.getModule());
        map.put("prof", grade.getProfessor());
        map.put("ects", grade.getEcts());
        map.put("cc", grade.getCcGrade());
        map.put("examen", grade.getExamGrade());
        map.put("moyPromo", grade.getPromoAverage());
        map.put("semester", grade.getSemester());
        return map;
    }

    private Map<String, Object> courseToMap(StudentCourseJpaEntity course) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", course.getId());
        map.put("courseId", course.getCourseId());
        map.put("titre", course.getTitre());
        map.put("coefficient", course.getCoefficient());
        map.put("progress", course.getProgress());
        map.put("statut", course.getStatut());
        map.put("semester", course.getSemester());
        return map;
    }

    private Map<String, Object> homeworkToMap(StudentHomeworkJpaEntity homework) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", homework.getId());
        map.put("courseId", homework.getCourseId());
        map.put("statut", homework.getStatut());
        map.put("titre", homework.getTitre());
        map.put("deadline", homework.getDeadline());
        map.put("priority", homework.getPriority());
        map.put("progress", homework.getProgress());
        map.put("submittedAt", homework.getSubmittedAt());
        map.put("fileName", homework.getFileName());
        return map;
    }

    private Map<String, Object> chatMessageToMap(LiveChatMessageJpaEntity message) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", message.getId());
        map.put("user", message.getUserName());
        map.put("text", message.getText());
        map.put("timestamp", message.getTimestamp());
        map.put("userId", message.getUserId());
        map.put("userRole", message.getUserRole());
        return map;
    }
}
