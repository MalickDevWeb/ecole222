package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.professor.domain.model.Course;
import ecole_221.sn.ecole.modules.professor.domain.model.CourseFactory;
import ecole_221.sn.ecole.modules.professor.domain.port.repository.ProfessorRepositoryPort;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import ecole_221.sn.ecole.partage.PageResult;

@Repository
public class InMemoryProfessorRepository implements ProfessorRepositoryPort {

    private final List<Course> courses = new CopyOnWriteArrayList<>();
    private final List<User> students = new CopyOnWriteArrayList<>();
    private final List<Map<String, Object>> grades = new CopyOnWriteArrayList<>();
    private final List<Map<String, Object>> homeworks = new CopyOnWriteArrayList<>();
    private final List<Map<String, Object>> lessons = new CopyOnWriteArrayList<>();
    private final List<Map<String, Object>> modules = new CopyOnWriteArrayList<>();
    private final List<Map<String, Object>> quizzes = new CopyOnWriteArrayList<>();
    private final List<Map<String, Object>> attendances = new CopyOnWriteArrayList<>();

    public InMemoryProfessorRepository() {
        courses.add(CourseFactory.reconstruct("course-1", "Architecture Logicielle", "M1 GL", 24, "usr-002", null, null));
        courses.add(CourseFactory.reconstruct("course-2", "Base de données NoSQL", "M1 GL", 24, "usr-002", null, null));
        courses.add(CourseFactory.reconstruct("course-3", "Réseaux d'entreprise", "L3 GL", 18, "usr-002", null, null));

        students.add(UserFactory.reconstructSimple("std-001", "Sall", "Mamadou", "student@ecole221.sn", "pass", null, RoleUtilisateur.ETUDIANT, null));
        students.add(UserFactory.reconstructSimple("std-002", "Diop", "Awa", "awa@ecole221.sn", "pass", null, RoleUtilisateur.ETUDIANT, null));

        // Initial grade
        Map<String, Object> grade = new HashMap<>();
        grade.put("studentId", "std-001");
        grade.put("courseId", "course-1");
        grade.put("cc", 14.5);
        grade.put("examen", 16.0);
        grade.put("average", 15.4);
        grades.add(grade);
    }

    @Override
    public PageResult<Course> getCoursesByProfessor(String profId, int page, int size) {
        List<Course> list = courses.stream().filter(c -> c.getProfId().equals(profId)).toList();
        return new PageResult<>(list, page, size, list.size(), 1);
    }

    @Override
    public List<User> getStudentsByClass(String classe) {
        return students; // Simplified for dev: returns all students
    }

    @Override
    public List<Map<String, Object>> getGradesByCourse(String courseId) {
        return grades.stream()
                .filter(g -> g.get("courseId").equals(courseId))
                .map(g -> {
                    Map<String, Object> gradeMap = new HashMap<>(g);
                    // Ajouter les informations de l'étudiant
                    String studentId = (String) g.get("studentId");
                    students.stream()
                        .filter(s -> s.getId().equals(studentId))
                        .findFirst()
                        .ifPresent(student -> {
                            gradeMap.put("nom", student.getNom());
                            gradeMap.put("prenom", student.getPrenom());
                        });
                    return gradeMap;
                })
                .toList();
    }

    @Override
    public void saveGrade(String studentId, String courseId, Double cc, Double exam) {
        grades.removeIf(g -> g.get("studentId").equals(studentId) && g.get("courseId").equals(courseId));
        Map<String, Object> g = new HashMap<>();
        g.put("studentId", studentId);
        g.put("courseId", courseId);
        g.put("cc", cc);
        g.put("examen", exam);
        double avg = (cc != null && exam != null) ? (cc * 0.4 + exam * 0.6) : (cc != null ? cc : (exam != null ? exam : 0.0));
        g.put("average", Math.round(avg * 100.0) / 100.0);
        grades.add(g);
    }

    @Override
    public Map<String, Object> getGrade(String studentId, String courseId) {
        return grades.stream()
                .filter(g -> g.get("studentId").equals(studentId) && g.get("courseId").equals(courseId))
                .findFirst()
                .orElseGet(() -> {
                    Map<String, Object> g = new HashMap<>();
                    g.put("studentId", studentId);
                    g.put("courseId", courseId);
                    g.put("cc", 0.0);
                    g.put("examen", 0.0);
                    g.put("average", 0.0);
                    return g;
                });
    }

    @Override
    public void saveHomework(String courseId, String title, String desc, String prio, String deadline) {
        Map<String, Object> hw = new HashMap<>();
        hw.put("id", "hw-" + UUID.randomUUID().toString().substring(0, 5));
        hw.put("courseId", courseId);
        hw.put("title", title);
        hw.put("desc", desc);
        hw.put("prio", prio);
        hw.put("deadline", deadline);
        homeworks.add(hw);
    }

    @Override
    public PageResult<Map<String, Object>> getHomeworksByCourse(String courseId, int page, int size) {
        List<Map<String, Object>> list = homeworks.stream().filter(h -> h.get("courseId").equals(courseId)).toList();
        return new PageResult<>(list, page, size, list.size(), 1);
    }

    @Override
    public void saveLesson(String courseId, String title, int duration, String date) {
        Map<String, Object> les = new HashMap<>();
        les.put("id", "les-" + UUID.randomUUID().toString().substring(0, 5));
        les.put("courseId", courseId);
        les.put("title", title);
        les.put("duration", duration);
        les.put("date", date);
        lessons.add(les);
    }

    @Override
    public PageResult<Map<String, Object>> getLessonsByCourse(String courseId, int page, int size) {
        List<Map<String, Object>> list = lessons.stream().filter(l -> l.get("courseId").equals(courseId)).toList();
        return new PageResult<>(list, page, size, list.size(), 1);
    }

    @Override
    public void saveModule(String courseId, String name) {
        Map<String, Object> mod = new HashMap<>();
        mod.put("id", "mod-" + UUID.randomUUID().toString().substring(0, 5));
        mod.put("courseId", courseId);
        mod.put("name", name);
        modules.add(mod);
    }

    @Override
    public PageResult<Map<String, Object>> getModulesByCourse(String courseId, int page, int size) {
        List<Map<String, Object>> list = modules.stream().filter(m -> m.get("courseId").equals(courseId)).toList();
        return new PageResult<>(list, page, size, list.size(), 1);
    }

    @Override
    public void saveQuiz(String courseId, String title, int duration) {
        Map<String, Object> qz = new HashMap<>();
        qz.put("id", "qz-" + UUID.randomUUID().toString().substring(0, 5));
        qz.put("courseId", courseId);
        qz.put("title", title);
        qz.put("duration", duration);
        quizzes.add(qz);
    }

    @Override
    public PageResult<Map<String, Object>> getQuizzesByCourse(String courseId, int page, int size) {
        List<Map<String, Object>> list = quizzes.stream().filter(q -> q.get("courseId").equals(courseId)).toList();
        return new PageResult<>(list, page, size, list.size(), 1);
    }

    @Override
    public void saveAttendance(String date, String studentId, String studentName, String status, String courseId) {
        Map<String, Object> att = new HashMap<>();
        att.put("id", "att-" + UUID.randomUUID().toString().substring(0, 5));
        att.put("date", date);
        att.put("studentId", studentId);
        att.put("studentName", studentName);
        att.put("status", status);
        att.put("courseId", courseId);
        attendances.add(att);
    }

    @Override
    public PageResult<Map<String, Object>> getAttendancesByCourse(String courseId, int page, int size) {
        List<Map<String, Object>> list = attendances.stream().filter(a -> a.get("courseId").equals(courseId)).toList();
        return new PageResult<>(list, page, size, list.size(), 1);
    }
}
