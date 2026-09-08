package ecole_221.sn.ecole.modules.professor.application.service;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.professor.application.port.dto.*;
import ecole_221.sn.ecole.modules.professor.domain.model.Course;
import ecole_221.sn.ecole.modules.professor.domain.valueobject.Priority;
import ecole_221.sn.ecole.modules.professor.domain.valueobject.SessionStatus;
import ecole_221.sn.ecole.modules.professor.domain.valueobject.SessionType;
import ecole_221.sn.ecole.partage.PageResult;
import ecole_221.sn.ecole.partage.dto.PageResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper pour convertir les objets Map en DTOs pour le module Professor.
 * Centralise toute la logique de conversion pour une meilleure maintenabilité.
 */
public final class ProfessorMapper {
    private static final Logger logger = LoggerFactory.getLogger(ProfessorMapper.class);

    private ProfessorMapper() {
        logger.debug("ProfessorMapper utility class initialized");
        // Utility class - prevent instantiation
    }

    // ==================== COURSE MAPPING ====================

    /**
     * Convertit un Course (modèle domaine) en CourseDto.
     * Note: Les champs credits, studentCount et modules ne sont pas dans le modèle domaine.
     * Des valeurs par défaut sont utilisées pour ces champs.
     */
    public static CourseDto toCourseDto(Course course) {
        logger.debug("Converting Course to CourseDto, courseId={}", course != null ? course.getId() : "null");
        if (course == null) {
            logger.warn("Course is null, returning null");
            return null;
        }
        
        CourseDto dto = new CourseDto(
            course.getId(),
            course.getName(), // titre standardisé
            course.getDescription(),
            course.getCode(),
            null, // classeId - not in domain model
            0,  // credits - not in domain model
            0,  // studentCount - not in domain model
            List.of()  // modules - not in domain model
        );
        logger.debug("Successfully converted Course to CourseDto: {}", dto);
        return dto;
    }

    /**
     * Convertit une Map en CourseDto.
     */
    public static CourseDto toCourseDto(Map<String, Object> courseMap) {
        logger.debug("Converting Map to CourseDto");
        if (courseMap == null) {
            logger.warn("courseMap is null, returning null");
            return null;
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modulesMap = (List<Map<String, Object>>) courseMap.getOrDefault("modules", List.of());
        logger.debug("Found {} modules in courseMap", modulesMap.size());
        
        List<ModuleDto> modules = modulesMap.stream()
            .map(ProfessorMapper::toModuleDto)
            .collect(Collectors.toList());
        
        CourseDto dto = new CourseDto(
            String.valueOf(courseMap.getOrDefault("id", "")),
            String.valueOf(courseMap.getOrDefault("titre", courseMap.getOrDefault("title", ""))), // titre standardisé
            String.valueOf(courseMap.getOrDefault("description", "")),
            String.valueOf(courseMap.getOrDefault("code", "")),
            String.valueOf(courseMap.getOrDefault("classeId", "")), // classeId ajouté
            toInt(courseMap.getOrDefault("credits", 0)),
            toInt(courseMap.getOrDefault("studentCount", 0)),
            modules
        );
        logger.debug("Successfully converted Map to CourseDto with id={}", dto.id());
        return dto;
    }

    /**
     * Convertit une PageResult de Course en PageResponseDto de CourseDto.
     */
    public static PageResponseDto<CourseDto> toCoursePageResponse(
            PageResult<Course> pageResult) {
        logger.debug("Converting PageResult<Course> to PageResponseDto<CourseDto>");
        if (pageResult == null) {
            logger.warn("pageResult is null, returning null");
            return null;
        }
        
        logger.debug("Processing {} courses in pageResult", pageResult.getContent().size());
        List<CourseDto> content = pageResult.getContent().stream()
            .map(ProfessorMapper::toCourseDto)
            .collect(Collectors.toList());
        
        PageResponseDto<CourseDto> response = new PageResponseDto<>(
            content,
            pageResult.getPageNumber(),
            pageResult.getPageSize(),
            pageResult.getTotalElements(),
            pageResult.getTotalPages(),
            pageResult.getPageNumber() >= pageResult.getTotalPages() - 1
        );
        logger.debug("Created PageResponseDto with {} items, page {}/{}", 
            content.size(), pageResult.getPageNumber() + 1, pageResult.getTotalPages());
        return response;
    }

    /**
     * Convertit une PageResult de Map en PageResponseDto de CourseDto (pour rétrocompatibilité).
     */
    public static PageResponseDto<CourseDto> toCoursePageResponseFromMap(
            PageResult<Map<String, Object>> pageResult) {
        if (pageResult == null) {
            return null;
        }
        
        List<CourseDto> content = pageResult.getContent().stream()
            .map(ProfessorMapper::toCourseDto)
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

    // ==================== MODULE MAPPING ====================

    /**
     * Convertit une Map en ModuleDto.
     */
    public static ModuleDto toModuleDto(Map<String, Object> moduleMap) {
        if (moduleMap == null) {
            return null;
        }
        
        return new ModuleDto(
            String.valueOf(moduleMap.getOrDefault("id", "")),
            String.valueOf(moduleMap.getOrDefault("titre", moduleMap.getOrDefault("name", ""))), // titre standardisé
            String.valueOf(moduleMap.getOrDefault("description", "")),
            toInt(moduleMap.getOrDefault("hours", 0)),
            String.valueOf(moduleMap.getOrDefault("courseId", ""))
        );
    }

    /**
     * Convertit une PageResult de Map en PageResponseDto de ModuleDto.
     */
    public static PageResponseDto<ModuleDto> toModulePageResponse(
            PageResult<Map<String, Object>> pageResult) {
        if (pageResult == null) {
            return null;
        }
        
        List<ModuleDto> content = pageResult.getContent().stream()
            .map(ProfessorMapper::toModuleDto)
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

    // ==================== LESSON MAPPING ====================

    /**
     * Convertit une Map en LessonDto.
     */
    public static LessonDto toLessonDto(Map<String, Object> lessonMap) {
        if (lessonMap == null) {
            return null;
        }
        
        return new LessonDto(
            String.valueOf(lessonMap.getOrDefault("id", "")),
            String.valueOf(lessonMap.getOrDefault("titre", lessonMap.getOrDefault("title", ""))), // titre standardisé
            toInt(lessonMap.getOrDefault("duration", 0)),
            parseLocalDate(lessonMap.get("date")),
            parseSessionType(String.valueOf(lessonMap.getOrDefault("type", "CM"))),
            String.valueOf(lessonMap.getOrDefault("courseId", ""))
        );
    }

    /**
     * Convertit une PageResult de Map en PageResponseDto de LessonDto.
     */
    public static PageResponseDto<LessonDto> toLessonPageResponse(
            PageResult<Map<String, Object>> pageResult) {
        if (pageResult == null) {
            return null;
        }
        
        List<LessonDto> content = pageResult.getContent().stream()
            .map(ProfessorMapper::toLessonDto)
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

    // ==================== HOMEWORK MAPPING ====================

    /**
     * Convertit une Map en HomeworkDto.
     */
    public static HomeworkDto toHomeworkDto(Map<String, Object> homeworkMap) {
        if (homeworkMap == null) {
            return null;
        }
        
        return new HomeworkDto(
            String.valueOf(homeworkMap.getOrDefault("id", "")),
            String.valueOf(homeworkMap.getOrDefault("titre", "")),
            String.valueOf(homeworkMap.getOrDefault("desc", "")),
            parsePriority(String.valueOf(homeworkMap.getOrDefault("prio", "MEDIUM"))),
            parseLocalDate(homeworkMap.get("deadlineStr")),
            String.valueOf(homeworkMap.getOrDefault("courseId", ""))
        );
    }

    /**
     * Convertit une PageResult de Map en PageResponseDto de HomeworkDto.
     */
    public static PageResponseDto<HomeworkDto> toHomeworkPageResponse(
            PageResult<Map<String, Object>> pageResult) {
        if (pageResult == null) {
            return null;
        }
        
        List<HomeworkDto> content = pageResult.getContent().stream()
            .map(ProfessorMapper::toHomeworkDto)
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

    // ==================== QUIZ MAPPING ====================

    /**
     * Convertit une Map en QuizDto.
     */
    public static QuizDto toQuizDto(Map<String, Object> quizMap) {
        if (quizMap == null) {
            return null;
        }
        
        return new QuizDto(
            String.valueOf(quizMap.getOrDefault("id", "")),
            String.valueOf(quizMap.getOrDefault("title", "")),
            toInt(quizMap.getOrDefault("duration", 0)),
            toInt(quizMap.getOrDefault("questionCount", 0)),
            toDouble(quizMap.getOrDefault("maxScore", 20.0)),
            String.valueOf(quizMap.getOrDefault("moduleId", ""))
        );
    }

    /**
     * Convertit une PageResult de Map en PageResponseDto de QuizDto.
     */
    public static PageResponseDto<QuizDto> toQuizPageResponse(
            PageResult<Map<String, Object>> pageResult) {
        if (pageResult == null) {
            return null;
        }
        
        List<QuizDto> content = pageResult.getContent().stream()
            .map(ProfessorMapper::toQuizDto)
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

    // ==================== SCHEDULE MAPPING ====================

    /**
     * Convertit une Map en ScheduleDto.
     */
    public static ScheduleDto toScheduleDto(Map<String, Object> scheduleMap) {
        if (scheduleMap == null) {
            return null;
        }
        
        return new ScheduleDto(
            String.valueOf(scheduleMap.getOrDefault("id", "")),
            String.valueOf(scheduleMap.getOrDefault("courseTitle", "")),
            String.valueOf(scheduleMap.getOrDefault("day", "")),
            String.valueOf(scheduleMap.getOrDefault("time", "")),
            String.valueOf(scheduleMap.getOrDefault("room", "")),
            String.valueOf(scheduleMap.getOrDefault("courseId", "")),
            parseSessionType(String.valueOf(scheduleMap.getOrDefault("type", "CM"))),
            parseLocalDate(scheduleMap.get("dateStr")),
            parseSessionStatus(String.valueOf(scheduleMap.getOrDefault("status", "SCHEDULED")))
        );
    }

    /**
     * Convertit une List de Map en List de ScheduleDto.
     */
    public static List<ScheduleDto> toScheduleDtoList(List<Map<String, Object>> schedulesMap) {
        if (schedulesMap == null) {
            return List.of();
        }
        
        return schedulesMap.stream()
            .map(ProfessorMapper::toScheduleDto)
            .collect(Collectors.toList());
    }

    // ==================== STUDENT MAPPING ====================

    /**
     * Convertit un User en StudentDto.
     */
    public static StudentDto toStudentDto(User user) {
        if (user == null) {
            return null;
        }
        
        // Note: User class doesn't have getMatricule() getter, using empty string
        // The matricule field exists but has no getter method
        return new StudentDto(
            user.getId(),
            "",  // matricule - not accessible via getter
            user.getNom(),
            user.getPrenom(),
            user.getEmail(),
            null,  // attendanceStatus
            null   // averageGrade
        );
    }

    /**
     * Convertit une List de User en List de StudentDto.
     */
    public static List<StudentDto> toStudentDtoList(List<User> users) {
        if (users == null) {
            return List.of();
        }
        
        return users.stream()
            .map(ProfessorMapper::toStudentDto)
            .collect(Collectors.toList());
    }

    // ==================== GRADE MAPPING ====================

    /**
     * Convertit une Map en GradeDto.
     */
    public static GradeDto toGradeDto(Map<String, Object> gradeMap) {
        if (gradeMap == null) {
            return null;
        }
        
        Double cc = toDouble(gradeMap.get("cc"));
        Double examen = toDouble(gradeMap.get("examen"));
        Double finalGrade = GradeDto.calculateFinalGrade(cc, examen);
        String mention = GradeDto.calculateMention(finalGrade);
        
        return new GradeDto(
            String.valueOf(gradeMap.getOrDefault("id", "")),
            String.valueOf(gradeMap.getOrDefault("studentId", "")),
            String.valueOf(gradeMap.getOrDefault("nom", gradeMap.getOrDefault("studentName", ""))), // nom standardisé
            String.valueOf(gradeMap.getOrDefault("prenom", "")), // prenom ajouté
            cc,
            examen,
            finalGrade,
            mention
        );
    }

    // ==================== RESPONSE FACTORIES ====================

    /**
     * Crée une réponse de succès simple.
     */
    public static Map<String, Object> createSuccessResponse() {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        return response;
    }

    /**
     * Crée une réponse de succès avec message.
     */
    public static Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> response = createSuccessResponse();
        response.put("message", message);
        return response;
    }

    /**
     * Crée une réponse pour l'assignation d'un portail.
     */
    public static Map<String, Object> createGateAssignmentResponse(String gate, String assignedTo) {
        Map<String, Object> response = createSuccessResponse();
        response.put("gate", gate);
        response.put("assignedTo", assignedTo);
        return response;
    }

    // ==================== REQUEST CONVERSION ====================

    /**
     * Convertit un CreateHomeworkRequestDto en Map pour le service.
     */
    public static Map<String, Object> toHomeworkMap(CreateHomeworkRequestDto request) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("titre", request.titre());
        map.put("desc", request.desc());
        map.put("prio", request.prio().name());
        map.put("deadlineStr", request.deadlineStr());
        return map;
    }

    /**
     * Convertit un CreateLessonRequestDto en Map pour le service.
     */
    public static Map<String, Object> toLessonMap(CreateLessonRequestDto request) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("title", request.title());
        map.put("duration", request.duration());
        map.put("date", request.date());
        return map;
    }

    /**
     * Convertit un CreateModuleRequestDto en Map pour le service.
     */
    public static Map<String, Object> toModuleMap(CreateModuleRequestDto request) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("name", request.name());
        return map;
    }

    /**
     * Convertit un CreateQuizRequestDto en Map pour le service.
     */
    public static Map<String, Object> toQuizMap(CreateQuizRequestDto request) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("title", request.title());
        map.put("duration", request.duration());
        return map;
    }

    /**
     * Convertit un SubmitAttendanceRequestDto en Map pour le service.
     */
    public static Map<String, Object> toAttendanceMap(SubmitAttendanceRequestDto request) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("date", request.date());
        map.put("studentId", request.studentId());
        map.put("studentName", request.studentName());
        map.put("status", request.status().name());
        map.put("courseId", request.courseId());
        return map;
    }

    // ==================== PARSING HELPERS ====================

    private static Priority parsePriority(String priority) {
        if (priority == null) {
            return Priority.MEDIUM;
        }
        
        try {
            return Priority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Priority.MEDIUM;
        }
    }

    private static SessionType parseSessionType(String type) {
        if (type == null) {
            return SessionType.CM;
        }
        
        try {
            return SessionType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SessionType.CM;
        }
    }

    private static SessionStatus parseSessionStatus(String status) {
        if (status == null) {
            return SessionStatus.SCHEDULED;
        }
        
        try {
            return SessionStatus.valueOf(status.toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return SessionStatus.SCHEDULED;
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
