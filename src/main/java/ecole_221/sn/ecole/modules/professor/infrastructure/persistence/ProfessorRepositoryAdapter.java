package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaEntity;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaRepository;
import ecole_221.sn.ecole.modules.professor.domain.model.Course;
import ecole_221.sn.ecole.modules.professor.domain.model.CourseFactory;
import ecole_221.sn.ecole.modules.professor.domain.port.repository.ProfessorRepositoryPort;
import ecole_221.sn.ecole.partage.PageResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@Primary
public class ProfessorRepositoryAdapter implements ProfessorRepositoryPort {

    private final CourseJpaRepository courseJpaRepository;
    private final HomeworkJpaRepository homeworkJpaRepository;
    private final LessonJpaRepository lessonJpaRepository;
    private final ModuleJpaRepository moduleJpaRepository;
    private final QuizJpaRepository quizJpaRepository;
    private final GradeJpaRepository gradeJpaRepository;
    private final AttendanceJpaRepository attendanceJpaRepository;
    private final UserJpaRepository userJpaRepository;

    public ProfessorRepositoryAdapter(CourseJpaRepository courseJpaRepository, 
                                      HomeworkJpaRepository homeworkJpaRepository,
                                      LessonJpaRepository lessonJpaRepository,
                                      ModuleJpaRepository moduleJpaRepository,
                                      QuizJpaRepository quizJpaRepository,
                                      GradeJpaRepository gradeJpaRepository,
                                      AttendanceJpaRepository attendanceJpaRepository,
                                      UserJpaRepository userJpaRepository) {
        this.courseJpaRepository = courseJpaRepository;
        this.homeworkJpaRepository = homeworkJpaRepository;
        this.lessonJpaRepository = lessonJpaRepository;
        this.moduleJpaRepository = moduleJpaRepository;
        this.quizJpaRepository = quizJpaRepository;
        this.gradeJpaRepository = gradeJpaRepository;
        this.attendanceJpaRepository = attendanceJpaRepository;
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public PageResult<Course> getCoursesByProfessor(String profId, int page, int size) {
        var springPage = courseJpaRepository.findByProfId(profId, PageRequest.of(page, size));
        List<Course> content = springPage.getContent()
                .stream()
                .map(entity -> CourseFactory.reconstruct(
                    entity.getId(),
                    entity.getName(),
                    entity.getClasse(),
                    entity.getTotalHours(),
                    entity.getProfId(),
                    entity.getCode(),
                    entity.getDescription()
                ))
                .collect(Collectors.toList());

        return new PageResult<>(
            content,
            springPage.getNumber(),
            springPage.getSize(),
            springPage.getTotalElements(),
            springPage.getTotalPages()
        );
    }

    // For now, keep the other methods returning dummy data until we create JPA entities for them
    @Override
    public List<User> getStudentsByClass(String classe) {
        return userJpaRepository.findByRole("ETUDIANT").stream()
            .map(entity -> {
                try {
                    return UserFactory.reconstructSimple(
                        entity.getId(),
                        entity.getNom(),
                        entity.getPrenom(),
                        entity.getEmail(),
                        entity.getPassword(),
                        entity.getMatricule(),
                        RoleUtilisateur.valueOf(entity.getRole()),
                        entity.getPhotoUrl()
                    );
                } catch (Exception e) {
                    // Fallback: use fromString if valueOf fails
                    return UserFactory.reconstructSimple(
                        entity.getId(),
                        entity.getNom(),
                        entity.getPrenom(),
                        entity.getEmail(),
                        entity.getPassword(),
                        entity.getMatricule(),
                        RoleUtilisateur.fromString(entity.getRole()),
                        entity.getPhotoUrl()
                    );
                }
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getGradesByCourse(String courseId) {
        return gradeJpaRepository.findByCourseId(courseId).stream().map(g -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", g.getId());
            map.put("studentId", g.getStudentId());
            map.put("courseId", g.getCourseId());
            map.put("cc", g.getCc());
            map.put("examen", g.getExamen());
            map.put("average", g.getAverage());
            
            // Récupérer les informations de l'étudiant
            userJpaRepository.findById(g.getStudentId()).ifPresent(user -> {
                map.put("nom", user.getNom());
                map.put("prenom", user.getPrenom());
            });
            
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public void saveGrade(String studentId, String courseId, Double cc, Double exam) {
        GradeJpaEntity grade = gradeJpaRepository.findByStudentIdAndCourseId(studentId, courseId)
            .orElseGet(GradeJpaEntity::new);
        if (grade.getId() == null) {
            grade.setId("grade-" + UUID.randomUUID().toString().substring(0, 8));
        }
        grade.setStudentId(studentId);
        grade.setCourseId(courseId);
        grade.setCc(cc);
        grade.setExamen(exam);
        double average = (cc != null && exam != null)
            ? (cc * 0.4 + exam * 0.6)
            : (cc != null ? cc : (exam != null ? exam : 0.0));
        grade.setAverage(Math.round(average * 100.0) / 100.0);
        gradeJpaRepository.save(grade);
    }

    @Override
    public Map<String, Object> getGrade(String studentId, String courseId) {
        return gradeJpaRepository.findByStudentIdAndCourseId(studentId, courseId)
            .map(g -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", g.getId());
                map.put("studentId", g.getStudentId());
                map.put("courseId", g.getCourseId());
                map.put("cc", g.getCc());
                map.put("examen", g.getExamen());
                map.put("average", g.getAverage());
                return map;
            })
            .orElseGet(Map::of);
    }

    @Override
    public void saveHomework(String courseId, String title, String desc, String prio, String deadline) {
        HomeworkJpaEntity homework = new HomeworkJpaEntity();
        homework.setId("hw-" + UUID.randomUUID().toString().substring(0, 8));
        homework.setCourseId(courseId);
        homework.setTitle(title);
        homework.setDescription(desc);
        homework.setPriority(prio);
        homework.setDeadline(deadline);
        homeworkJpaRepository.save(homework);
    }

    @Override
    public PageResult<Map<String, Object>> getHomeworksByCourse(String courseId, int page, int size) {
        var springPage = homeworkJpaRepository.findByCourseId(courseId, PageRequest.of(page, size));
        List<Map<String, Object>> content = springPage.getContent().stream().map(h -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", h.getId());
            map.put("title", h.getTitle());
            map.put("description", h.getDescription());
            map.put("priority", h.getPriority());
            map.put("deadline", h.getDeadline());
            return map;
        }).collect(Collectors.toList());
        return new PageResult<>(content, springPage.getNumber(), springPage.getSize(), springPage.getTotalElements(), springPage.getTotalPages());
    }

    @Override
    public void saveLesson(String courseId, String title, int duration, String date) {
        LessonJpaEntity lesson = new LessonJpaEntity();
        lesson.setId("lesson-" + UUID.randomUUID().toString().substring(0, 8));
        lesson.setCourseId(courseId);
        lesson.setTitle(title);
        lesson.setDuration(duration);
        lesson.setDate(date);
        lessonJpaRepository.save(lesson);
    }

    @Override
    public PageResult<Map<String, Object>> getLessonsByCourse(String courseId, int page, int size) {
        var springPage = lessonJpaRepository.findByCourseId(courseId, PageRequest.of(page, size));
        List<Map<String, Object>> content = springPage.getContent().stream().map(l -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", l.getId());
            map.put("title", l.getTitle());
            map.put("duration", l.getDuration());
            map.put("date", l.getDate());
            return map;
        }).collect(Collectors.toList());
        return new PageResult<>(content, springPage.getNumber(), springPage.getSize(), springPage.getTotalElements(), springPage.getTotalPages());
    }

    @Override
    public void saveModule(String courseId, String name) {
        ModuleJpaEntity module = new ModuleJpaEntity();
        module.setId("module-" + UUID.randomUUID().toString().substring(0, 8));
        module.setCourseId(courseId);
        module.setName(name);
        moduleJpaRepository.save(module);
    }

    @Override
    public PageResult<Map<String, Object>> getModulesByCourse(String courseId, int page, int size) {
        var springPage = moduleJpaRepository.findByCourseId(courseId, PageRequest.of(page, size));
        List<Map<String, Object>> content = springPage.getContent().stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("name", m.getName());
            return map;
        }).collect(Collectors.toList());
        return new PageResult<>(content, springPage.getNumber(), springPage.getSize(), springPage.getTotalElements(), springPage.getTotalPages());
    }

    @Override
    public void saveQuiz(String courseId, String title, int duration) {
        QuizJpaEntity quiz = new QuizJpaEntity();
        quiz.setId("quiz-" + UUID.randomUUID().toString().substring(0, 8));
        quiz.setCourseId(courseId);
        quiz.setTitle(title);
        quiz.setDuration(duration);
        quizJpaRepository.save(quiz);
    }

    @Override
    public PageResult<Map<String, Object>> getQuizzesByCourse(String courseId, int page, int size) {
        var springPage = quizJpaRepository.findByCourseId(courseId, PageRequest.of(page, size));
        List<Map<String, Object>> content = springPage.getContent().stream().map(q -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", q.getId());
            map.put("title", q.getTitle());
            map.put("duration", q.getDuration());
            return map;
        }).collect(Collectors.toList());
        return new PageResult<>(content, springPage.getNumber(), springPage.getSize(), springPage.getTotalElements(), springPage.getTotalPages());
    }

    @Override
    public void saveAttendance(String date, String studentId, String studentName, String status, String courseId) {
        AttendanceJpaEntity attendance = new AttendanceJpaEntity();
        attendance.setId("att-" + UUID.randomUUID().toString().substring(0, 8));
        attendance.setDate(date);
        attendance.setStudentId(studentId);
        attendance.setStudentName(studentName);
        attendance.setStatus(status);
        attendance.setCourseId(courseId);
        attendanceJpaRepository.save(attendance);
    }

    @Override
    public PageResult<Map<String, Object>> getAttendancesByCourse(String courseId, int page, int size) {
        var springPage = attendanceJpaRepository.findByCourseId(courseId, PageRequest.of(page, size));
        List<Map<String, Object>> content = springPage.getContent().stream().map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("date", a.getDate());
            map.put("studentId", a.getStudentId());
            map.put("studentName", a.getStudentName());
            map.put("status", a.getStatus());
            map.put("courseId", a.getCourseId());
            return map;
        }).collect(Collectors.toList());
        return new PageResult<>(content, springPage.getNumber(), springPage.getSize(), springPage.getTotalElements(), springPage.getTotalPages());
    }
}
