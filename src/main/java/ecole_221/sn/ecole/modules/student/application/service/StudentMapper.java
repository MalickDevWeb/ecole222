package ecole_221.sn.ecole.modules.student.application.service;

import ecole_221.sn.ecole.modules.student.application.port.dto.*;
import ecole_221.sn.ecole.modules.student.domain.valueobject.AttendanceStatus;
import ecole_221.sn.ecole.modules.student.domain.valueobject.CourseStatus;
import ecole_221.sn.ecole.modules.student.domain.valueobject.ReactionType;
import ecole_221.sn.ecole.partage.PageResult;
import ecole_221.sn.ecole.partage.dto.PageResponseDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper pour convertir les objets Map en DTOs pour le module Student.
 * Centralise toute la logique de conversion pour une meilleure maintenabilité.
 */
@SuppressWarnings("unchecked")
public final class StudentMapper {

    private StudentMapper() {
        // Utility class - prevent instantiation
    }

    // ==================== PROFILE MAPPING ====================

    /**
     * Convertit une Map en StudentProfileDto.
     */
    public static StudentProfileDto toProfileDto(Map<String, Object> profileMap) {
        if (profileMap == null) {
            return null;
        }
        
        return new StudentProfileDto(
            String.valueOf(profileMap.getOrDefault("id", "")),
            String.valueOf(profileMap.getOrDefault("nom", "")),
            String.valueOf(profileMap.getOrDefault("prenom", "")),
            String.valueOf(profileMap.getOrDefault("email", "")),
            String.valueOf(profileMap.getOrDefault("matricule", "")),
            parseLocalDate(profileMap.get("dateNaissance")),
            String.valueOf(profileMap.getOrDefault("telephone", "")),
            String.valueOf(profileMap.getOrDefault("adresse", "")),
            String.valueOf(profileMap.getOrDefault("filiere", "")),
            String.valueOf(profileMap.getOrDefault("niveau", "")),
            String.valueOf(profileMap.getOrDefault("photoUrl", ""))
        );
    }

    // ==================== DASHBOARD MAPPING ====================

    /**
     * Convertit une Map en StudentDashboardDto.
     */
    public static StudentDashboardDto toDashboardDto(Map<String, Object> dashboardMap) {
        if (dashboardMap == null) {
            return null;
        }
        
        return new StudentDashboardDto(
            toInt(dashboardMap.getOrDefault("totalCredits", 0)),
            toDouble(dashboardMap.getOrDefault("moyenneGenerale", 0.0)),
            toInt(dashboardMap.getOrDefault("coursEnCours", 0)),
            toInt(dashboardMap.getOrDefault("devoirsARendre", 0)),
            toExamInfoDtoList(dashboardMap.get("prochainsExamens")),
            toAttendanceStatsDto((Map<String, Object>) dashboardMap.get("statsPresence")),
            toNotificationDtoList(dashboardMap.get("notifications"))
        );
    }

    private static List<StudentDashboardDto.ExamInfoDto> toExamInfoDtoList(Object value) {
        if (value == null) {
            return List.of();
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> examensList = (List<Map<String, Object>>) value;
        return examensList.stream()
            .map(e -> new StudentDashboardDto.ExamInfoDto(
                String.valueOf(e.getOrDefault("id", "")),
                String.valueOf(e.getOrDefault("titre", "")),
                String.valueOf(e.getOrDefault("date", "")),
                String.valueOf(e.getOrDefault("salle", ""))
            ))
            .collect(Collectors.toList());
    }

    private static StudentDashboardDto.AttendanceStatsDto toAttendanceStatsDto(Map<String, Object> statsMap) {
        if (statsMap == null) {
            statsMap = Map.of();
        }
        
        return new StudentDashboardDto.AttendanceStatsDto(
            toDouble(statsMap.getOrDefault("tauxPresence", 0.0)),
            toInt(statsMap.getOrDefault("present", 0)),
            toInt(statsMap.getOrDefault("absent", 0)),
            toInt(statsMap.getOrDefault("retards", 0))
        );
    }

    private static List<StudentDashboardDto.NotificationDto> toNotificationDtoList(Object value) {
        if (value == null) {
            return List.of();
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> notifsList = (List<Map<String, Object>>) value;
        return notifsList.stream()
            .map(n -> new StudentDashboardDto.NotificationDto(
                String.valueOf(n.getOrDefault("id", "")),
                String.valueOf(n.getOrDefault("titre", "")),
                String.valueOf(n.getOrDefault("message", "")),
                String.valueOf(n.getOrDefault("date", "")),
                Boolean.parseBoolean(String.valueOf(n.getOrDefault("lu", false)))
            ))
            .collect(Collectors.toList());
    }

    // ==================== DIGITAL BADGE MAPPING ====================

    /**
     * Convertit une Map en DigitalBadgeDto.
     */
    public static DigitalBadgeDto toDigitalBadgeDto(Map<String, Object> badgeMap) {
        if (badgeMap == null) {
            return null;
        }
        
        return new DigitalBadgeDto(
            String.valueOf(badgeMap.getOrDefault("id", "")),
            String.valueOf(badgeMap.getOrDefault("studentId", "")),
            String.valueOf(badgeMap.getOrDefault("studentName", "")),
            String.valueOf(badgeMap.getOrDefault("matricule", "")),
            String.valueOf(badgeMap.getOrDefault("classe", "")),
            String.valueOf(badgeMap.getOrDefault("qrPayload", "")),
            String.valueOf(badgeMap.getOrDefault("token", "")),
            toLong(badgeMap.getOrDefault("minuteEpoch", 0L)),
            toInt(badgeMap.getOrDefault("expiresInSeconds", 60)),
            String.valueOf(badgeMap.getOrDefault("status", ""))
        );
    }

    // ==================== ATTENDANCE MAPPING ====================

    /**
     * Convertit une Map en AttendanceDto.
     */
    public static AttendanceDto toAttendanceDto(Map<String, Object> attendanceMap) {
        if (attendanceMap == null) {
            return null;
        }
        
        return new AttendanceDto(
            String.valueOf(attendanceMap.getOrDefault("id", "")),
            parseLocalDateTime(attendanceMap.getOrDefault("date", LocalDateTime.now().toString())),
            parseAttendanceStatus(String.valueOf(attendanceMap.getOrDefault("status", "UNKNOWN"))),
            String.valueOf(attendanceMap.getOrDefault("studentId", "")),
            String.valueOf(attendanceMap.getOrDefault("studentName", "")),
            String.valueOf(attendanceMap.getOrDefault("courseId", ""))
        );
    }

    /**
     * Convertit une PageResult de Map en PageResponseDto de AttendanceDto.
     */
    public static PageResponseDto<AttendanceDto> toAttendancePageResponse(
            PageResult<Map<String, Object>> pageResult) {
        if (pageResult == null) {
            return null;
        }
        
        List<AttendanceDto> content = pageResult.getContent().stream()
            .map(StudentMapper::toAttendanceDto)
            .collect(Collectors.toList());
        
        return new PageResponseDto<>(
            content,
            pageResult.getPageNumber(),
            pageResult.getPageSize(),
            pageResult.getTotalElements(),
            pageResult.getTotalPages(),
            pageResult.getPageNumber() >= pageResult.getTotalPages() - 1
        );
    }

    // ==================== LIVE SESSION MAPPING ====================

    /**
     * Convertit une Map en LiveSessionDto.
     */
    public static LiveSessionDto toLiveSessionDto(Map<String, Object> sessionMap) {
        if (sessionMap == null) {
            return null;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> reactions = (Map<String, Integer>) sessionMap.getOrDefault("reactions", Map.of());
        
        return new LiveSessionDto(
            String.valueOf(sessionMap.getOrDefault("id", "")),
            String.valueOf(sessionMap.getOrDefault("courseTitle", "")),
            String.valueOf(sessionMap.getOrDefault("professorName", "")),
            String.valueOf(sessionMap.getOrDefault("status", "")),
            toInt(sessionMap.getOrDefault("activeUsers", 0)),
            String.valueOf(sessionMap.getOrDefault("link", "")),
            String.valueOf(sessionMap.getOrDefault("time", "")),
            reactions
        );
    }

    /**
     * Convertit une PageResult de Map en PageResponseDto de LiveSessionDto.
     */
    public static PageResponseDto<LiveSessionDto> toLiveSessionPageResponse(
            PageResult<Map<String, Object>> pageResult) {
        if (pageResult == null) {
            return null;
        }
        
        List<LiveSessionDto> content = pageResult.getContent().stream()
            .map(StudentMapper::toLiveSessionDto)
            .collect(Collectors.toList());
        
        return new PageResponseDto<>(
            content,
            pageResult.getPageNumber(),
            pageResult.getPageSize(),
            pageResult.getTotalElements(),
            pageResult.getTotalPages(),
            pageResult.getPageNumber() >= pageResult.getTotalPages() - 1
        );
    }

    // ==================== GRADE MAPPING ====================

    /**
     * Convertit une Map en GradeDto.
     */
    public static GradeDto toGradeDto(Map<String, Object> gradeMap) {
        if (gradeMap == null) {
            return null;
        }
        
        return new GradeDto(
            String.valueOf(gradeMap.getOrDefault("id", "")),
            String.valueOf(gradeMap.getOrDefault("module", "")),
            String.valueOf(gradeMap.getOrDefault("prof", "")),
            toInt(gradeMap.getOrDefault("ects", 0)),
            toDouble(gradeMap.getOrDefault("cc", 0.0)),
            toDouble(gradeMap.getOrDefault("examen", 0.0)),
            toDouble(gradeMap.getOrDefault("moyPromo", 0.0))
        );
    }

    /**
     * Convertit une List de Map en List de GradeDto.
     */
    public static List<GradeDto> toGradeDtoList(List<Map<String, Object>> gradesMap) {
        if (gradesMap == null) {
            return List.of();
        }
        
        return gradesMap.stream()
            .map(StudentMapper::toGradeDto)
            .collect(Collectors.toList());
    }

    // ==================== COURSE MAPPING ====================

    /**
     * Convertit une Map en CourseDto.
     */
    public static CourseDto toCourseDto(Map<String, Object> courseMap) {
        if (courseMap == null) {
            return null;
        }
        
        @SuppressWarnings("unchecked")
        List<String> unites = (List<String>) courseMap.getOrDefault("unites", List.of());
        
        return new CourseDto(
            String.valueOf(courseMap.getOrDefault("id", "")),
            String.valueOf(courseMap.getOrDefault("titre", "")),
            toInt(courseMap.getOrDefault("coefficient", 0)),
            toInt(courseMap.getOrDefault("progress", 0)),
            parseCourseStatus(String.valueOf(courseMap.getOrDefault("statut", "EN_COURS"))),
            unites
        );
    }

    /**
     * Convertit une List de Map en List de CourseDto.
     */
    public static List<CourseDto> toCourseDtoList(List<Map<String, Object>> coursesMap) {
        if (coursesMap == null) {
            return List.of();
        }
        
        return coursesMap.stream()
            .map(StudentMapper::toCourseDto)
            .collect(Collectors.toList());
    }

    // ==================== HOMEWORK MAPPING ====================

    /**
     * Convertit une Map en HomeworkDto.
     */
    public static HomeworkDto toHomeworkDto(Map<String, Object> homeworkMap) {
        if (homeworkMap == null) {
            return null;
        }
        
        Object progressVal = homeworkMap.get("progress");
        Integer progress = progressVal instanceof Number n ? n.intValue() : null;

        return new HomeworkDto(
            String.valueOf(homeworkMap.getOrDefault("id", "")),
            String.valueOf(homeworkMap.getOrDefault("statut", "")),
            String.valueOf(homeworkMap.getOrDefault("titre", "")),
            parseLocalDate(homeworkMap.get("deadlineStr")),
            String.valueOf(homeworkMap.getOrDefault("prio", "")),
            String.valueOf(homeworkMap.getOrDefault("courseId", "")),
            String.valueOf(homeworkMap.getOrDefault("desc", "")),
            progress
        );
    }

    /**
     * Convertit une List de Map en List de HomeworkDto.
     */
    public static List<HomeworkDto> toHomeworkDtoList(List<Map<String, Object>> homeworksMap) {
        if (homeworksMap == null) {
            return List.of();
        }
        
        return homeworksMap.stream()
            .map(StudentMapper::toHomeworkDto)
            .collect(Collectors.toList());
    }

    // ==================== REACTION MAPPING ====================

    /**
     * Convertit une String en ReactionType.
     */
    public static ReactionType toReactionType(String type) {
        if (type == null) {
            return null;
        }
        
        try {
            return ReactionType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Crée un ReactionResponseDto.
     */
    public static ReactionResponseDto toReactionResponseDto(String sessionId, ReactionType reaction) {
        return new ReactionResponseDto(true, sessionId, reaction);
    }

    // ==================== QUIZ MAPPING ====================

    /**
     * Crée un QuizSubmissionResponseDto.
     */
    public static QuizSubmissionResponseDto toQuizSubmissionResponseDto(
            String courseId, int score, int total, String message) {
        return new QuizSubmissionResponseDto(true, courseId, score, total, message);
    }

    // ==================== PARSING HELPERS ====================

    private static AttendanceStatus parseAttendanceStatus(String status) {
        if (status == null) {
            return AttendanceStatus.UNKNOWN;
        }
        
        try {
            return AttendanceStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AttendanceStatus.UNKNOWN;
        }
    }

    private static CourseStatus parseCourseStatus(String status) {
        if (status == null) {
            return CourseStatus.EN_COURS;
        }
        
        try {
            return CourseStatus.valueOf(status.toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return CourseStatus.EN_COURS;
        }
    }

    private static LocalDate parseLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        
        try {
            return LocalDate.parse(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDateTime parseLocalDateTime(Object value) {
        try {
            return LocalDateTime.parse(String.valueOf(value), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private static int toInt(Object val) {
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(String.valueOf(val)); } catch (Exception e) { return 0; }
    }

    private static long toLong(Object val) {
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(String.valueOf(val)); } catch (Exception e) { return 0L; }
    }

    private static double toDouble(Object val) {
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(String.valueOf(val)); } catch (Exception e) { return 0; }
    }
}
