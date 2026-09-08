package ecole_221.sn.ecole.modules.professor.application.service;

import org.springframework.stereotype.Service;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.professor.application.port.dto.GradeDto;
import ecole_221.sn.ecole.modules.professor.application.port.usecase.ProfessorUseCase;
import ecole_221.sn.ecole.modules.professor.domain.model.Course;
import ecole_221.sn.ecole.modules.professor.domain.port.repository.ProfessorRepositoryPort;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ReminderJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ReminderJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaEntity;
import ecole_221.sn.ecole.partage.PageResult;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProfessorService implements ProfessorUseCase {

    private final ProfessorRepositoryPort repositoryPort;
    private final ReminderJpaRepository reminderRepository;
    private final ScheduleSessionJpaRepository scheduleRepository;

    public ProfessorService(ProfessorRepositoryPort repositoryPort, 
                           ReminderJpaRepository reminderRepository,
                           ScheduleSessionJpaRepository scheduleRepository) {
        this.repositoryPort = repositoryPort;
        this.reminderRepository = reminderRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "professorCoursesV2", key = "{#profId, #page, #size}")
    public PageResult<Course> getCourses(String profId, int page, int size) {
        return repositoryPort.getCoursesByProfessor(profId, page, size);
    }

    @Override
    public List<User> getStudents(String classe) { return repositoryPort.getStudentsByClass(classe); }

    @Override
    public List<GradeDto> getCourseGrades(String courseId) {
        List<Map<String, Object>> gradesMap = repositoryPort.getGradesByCourse(courseId);
        return gradesMap.stream()
            .map(ProfessorMapper::toGradeDto)
            .toList();
    }

    @Override
    public void saveGrade(String studentId, String courseId, Double cc, Double exam) {
        repositoryPort.saveGrade(studentId, courseId, cc, exam);
    }

    @Override
    public Map<String, Object> getGrade(String studentId, String courseId) {
        return repositoryPort.getGrade(studentId, courseId);
    }

    @Override
    public void saveHomework(String courseId, String title, String desc, String prio, String deadline) {
        repositoryPort.saveHomework(courseId, title, desc, prio, deadline);
    }

    @Override
    public PageResult<Map<String, Object>> getHomeworks(String courseId, int page, int size) { return repositoryPort.getHomeworksByCourse(courseId, page, size); }

    @Override
    public void saveLesson(String courseId, String title, int duration, String date) {
        repositoryPort.saveLesson(courseId, title, duration, date);
    }

    @Override
    public PageResult<Map<String, Object>> getLessons(String courseId, int page, int size) { return repositoryPort.getLessonsByCourse(courseId, page, size); }

    @Override
    public void saveModule(String courseId, String name) { repositoryPort.saveModule(courseId, name); }

    @Override
    public PageResult<Map<String, Object>> getModules(String courseId, int page, int size) { return repositoryPort.getModulesByCourse(courseId, page, size); }

    @Override
    public void saveQuiz(String courseId, String title, int duration) { repositoryPort.saveQuiz(courseId, title, duration); }

    @Override
    public PageResult<Map<String, Object>> getQuizzes(String courseId, int page, int size) { return repositoryPort.getQuizzesByCourse(courseId, page, size); }

    @Override
    public void submitAttendance(String date, String studentId, String studentName, String status, String courseId) {
        repositoryPort.saveAttendance(date, studentId, studentName, status, courseId);
    }

    @Override
    public PageResult<Map<String, Object>> getAttendances(String courseId, int page, int size) { return repositoryPort.getAttendancesByCourse(courseId, page, size); }

    // New methods for reminders and schedule
    @Override
    public List<Map<String, Object>> getReminders(String courseId) {
        List<ReminderJpaEntity> reminders = reminderRepository.findByCourseId(courseId);
        return reminders.stream()
            .map(this::reminderToMap)
            .toList();
    }

    @Override
    public void saveReminder(String courseId, String content, Boolean isUrgent, String professorId) {
        ReminderJpaEntity reminder = new ReminderJpaEntity(
            "rem-" + UUID.randomUUID().toString().substring(0, 8),
            content,
            isUrgent,
            courseId,
            professorId,
            LocalDateTime.now().plusDays(7) // Default due date: 7 days from now
        );
        reminderRepository.save(reminder);
    }

    @Override
    public List<Map<String, Object>> getSchedule(String professorId) {
        List<ScheduleSessionJpaEntity> sessions = scheduleRepository.findByProfessorId(professorId);
        return sessions.stream()
            .map(this::scheduleToMap)
            .toList();
    }

    @Override
    public void cancelScheduleSession(String sessionId, String reason) {
        ScheduleSessionJpaEntity session = scheduleRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));
        session.setStatus("annule");
        session.setCancellationReason(reason);
        scheduleRepository.save(session);
    }

    @Override
    public void rescheduleScheduleSession(String sessionId, String day, String time, String room) {
        ScheduleSessionJpaEntity session = scheduleRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));
        session.setDay(day);
        session.setTime(time);
        session.setRoom(room);
        session.setStatus("reporte");
        session.setOriginalDate(LocalDateTime.now());
        scheduleRepository.save(session);
    }

    private Map<String, Object> reminderToMap(ReminderJpaEntity reminder) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", reminder.getId());
        map.put("content", reminder.getContent());
        map.put("isUrgent", reminder.getIsUrgent());
        map.put("courseId", reminder.getCourseId());
        map.put("professorId", reminder.getProfessorId());
        map.put("dueDate", reminder.getDueDate());
        map.put("isCompleted", reminder.getIsCompleted());
        map.put("createdAt", reminder.getCreatedAt());
        return map;
    }

    private Map<String, Object> scheduleToMap(ScheduleSessionJpaEntity session) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", session.getId());
        map.put("courseTitle", session.getCourseTitle());
        map.put("day", session.getDay());
        map.put("time", session.getTime());
        map.put("room", session.getRoom());
        map.put("courseId", session.getCourseId());
        map.put("type", session.getType());
        map.put("dateStr", session.getDateStr());
        map.put("status", session.getStatus());
        map.put("classe", session.getClasse());
        map.put("professorId", session.getProfessorId());
        map.put("cancellationReason", session.getCancellationReason());
        map.put("originalDate", session.getOriginalDate());
        map.put("rescheduledTo", session.getRescheduledTo());
        return map;
    }
}