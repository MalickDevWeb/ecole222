package ecole_221.sn.ecole.modules.student.infrastructure.persistence;

import ecole_221.sn.ecole.modules.student.domain.model.StudentCourse;
import ecole_221.sn.ecole.modules.student.domain.model.StudentCourseFactory;
import ecole_221.sn.ecole.modules.student.domain.port.repository.StudentCourseRepositoryPort;
import ecole_221.sn.ecole.modules.student.domain.valueobject.CourseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptateur de repository pour l'agrégat StudentCourse.
 * Implémente StudentCourseRepositoryPort en utilisant StudentCourseJpaRepository.
 */
@Component
public class StudentCourseRepositoryAdapter implements StudentCourseRepositoryPort {
    
    private static final Logger logger = LoggerFactory.getLogger(StudentCourseRepositoryAdapter.class);
    
    private final StudentCourseJpaRepository studentCourseJpaRepository;
    
    public StudentCourseRepositoryAdapter(StudentCourseJpaRepository studentCourseJpaRepository) {
        this.studentCourseJpaRepository = studentCourseJpaRepository;
    }
    
    @Override
    public StudentCourse save(StudentCourse course) {
        logger.debug("Sauvegarde du cours d'étudiant: {}", course.getId());
        StudentCourseJpaEntity entity = toStudentCourseEntity(course);
        StudentCourseJpaEntity savedEntity = studentCourseJpaRepository.save(entity);
        return toStudentCourse(savedEntity);
    }
    
    @Override
    public Optional<StudentCourse> findById(String id) {
        logger.debug("Récupération du cours d'étudiant: {}", id);
        return studentCourseJpaRepository.findById(id)
            .map(this::toStudentCourse);
    }
    
    @Override
    public List<StudentCourse> findAll() {
        logger.debug("Récupération de tous les cours d'étudiants");
        return studentCourseJpaRepository.findAll().stream()
            .map(this::toStudentCourse)
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(String id) {
        logger.debug("Suppression du cours d'étudiant: {}", id);
        studentCourseJpaRepository.deleteById(id);
    }
    
    @Override
    public List<StudentCourse> findByStudentId(String studentId) {
        logger.debug("Récupération des cours par étudiant: {}", studentId);
        return studentCourseJpaRepository.findByStudentId(studentId).stream()
            .map(this::toStudentCourse)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<StudentCourse> findByCourseId(String courseId) {
        logger.debug("Récupération des cours par ID de cours: {}", courseId);
        return studentCourseJpaRepository.findByCourseId(courseId).stream()
            .map(this::toStudentCourse)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<StudentCourse> findByStatut(CourseStatus statut) {
        logger.debug("Récupération des cours par statut: {}", statut);
        return studentCourseJpaRepository.findByStatut(statut.value()).stream()
            .map(this::toStudentCourse)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<StudentCourse> findBySemester(String semester) {
        logger.debug("Récupération des cours par semestre: {}", semester);
        return studentCourseJpaRepository.findBySemester(semester).stream()
            .map(this::toStudentCourse)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<StudentCourse> findByStudentIdAndSemester(String studentId, String semester) {
        logger.debug("Récupération des cours par étudiant et semestre: {} - {}", studentId, semester);
        return studentCourseJpaRepository.findByStudentIdAndSemester(studentId, semester).stream()
            .map(this::toStudentCourse)
            .collect(Collectors.toList());
    }
    
    // ==================== MAPPERS JPA ↔ DOMAIN ====================
    
    private StudentCourse toStudentCourse(StudentCourseJpaEntity entity) {
        return StudentCourseFactory.reconstruct(
            entity.getId(),
            entity.getStudentId(),
            entity.getCourseId(),
            entity.getTitre(),
            entity.getCoefficient(),
            entity.getProgress(),
            new CourseStatus(entity.getStatut()),
            entity.getSemester(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
    
    private StudentCourseJpaEntity toStudentCourseEntity(StudentCourse course) {
        StudentCourseJpaEntity entity = new StudentCourseJpaEntity();
        entity.setId(course.getId());
        entity.setStudentId(course.getStudentId());
        entity.setCourseId(course.getCourseId());
        entity.setTitre(course.getTitre());
        entity.setCoefficient(course.getCoefficient());
        entity.setProgress(course.getProgress());
        entity.setStatut(course.getStatut().value());
        entity.setSemester(course.getSemester());
        entity.setCreatedAt(course.getCreatedAt());
        entity.setUpdatedAt(course.getUpdatedAt());
        return entity;
    }
}