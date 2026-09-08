package ecole_221.sn.ecole.modules.student.infrastructure.persistence;

import ecole_221.sn.ecole.modules.student.domain.exception.StudentGradeNotFoundException;
import ecole_221.sn.ecole.modules.student.domain.model.StudentGrade;
import ecole_221.sn.ecole.modules.student.domain.model.StudentGradeFactory;
import ecole_221.sn.ecole.modules.student.domain.port.repository.StudentGradeRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptateur de repository pour l'agrégat StudentGrade.
 * Implémente StudentGradeRepositoryPort en utilisant StudentGradeJpaRepository.
 */
@Component
public class StudentGradeRepositoryAdapter implements StudentGradeRepositoryPort {
    
    private static final Logger logger = LoggerFactory.getLogger(StudentGradeRepositoryAdapter.class);
    
    private final StudentGradeJpaRepository studentGradeJpaRepository;
    
    public StudentGradeRepositoryAdapter(StudentGradeJpaRepository studentGradeJpaRepository) {
        this.studentGradeJpaRepository = studentGradeJpaRepository;
    }
    
    @Override
    public StudentGrade save(StudentGrade grade) {
        logger.debug("Sauvegarde de la note d'étudiant: {}", grade.getId());
        StudentGradeJpaEntity entity = toStudentGradeEntity(grade);
        StudentGradeJpaEntity savedEntity = studentGradeJpaRepository.save(entity);
        return toStudentGrade(savedEntity);
    }
    
    @Override
    public Optional<StudentGrade> findById(String id) {
        logger.debug("Récupération de la note d'étudiant: {}", id);
        return studentGradeJpaRepository.findById(id)
            .map(this::toStudentGrade);
    }
    
    @Override
    public List<StudentGrade> findAll() {
        logger.debug("Récupération de toutes les notes d'étudiants");
        return studentGradeJpaRepository.findAll().stream()
            .map(this::toStudentGrade)
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(String id) {
        logger.debug("Suppression de la note d'étudiant: {}", id);
        if (!studentGradeJpaRepository.existsById(id)) {
            throw new StudentGradeNotFoundException(id);
        }
        studentGradeJpaRepository.deleteById(id);
    }
    
    @Override
    public List<StudentGrade> findByStudentId(String studentId) {
        logger.debug("Récupération des notes par étudiant: {}", studentId);
        return studentGradeJpaRepository.findByStudentId(studentId).stream()
            .map(this::toStudentGrade)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<StudentGrade> findByModule(String module) {
        logger.debug("Récupération des notes par module: {}", module);
        return studentGradeJpaRepository.findByModule(module).stream()
            .map(this::toStudentGrade)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<StudentGrade> findBySemester(String semester) {
        logger.debug("Récupération des notes par semestre: {}", semester);
        return studentGradeJpaRepository.findBySemester(semester).stream()
            .map(this::toStudentGrade)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<StudentGrade> findByStudentIdAndSemester(String studentId, String semester) {
        logger.debug("Récupération des notes par étudiant et semestre: {} - {}", studentId, semester);
        return studentGradeJpaRepository.findByStudentIdAndSemester(studentId, semester).stream()
            .map(this::toStudentGrade)
            .collect(Collectors.toList());
    }
    
    // ==================== MAPPERS JPA ↔ DOMAIN ====================
    
    private StudentGrade toStudentGrade(StudentGradeJpaEntity entity) {
        return StudentGradeFactory.reconstruct(
            entity.getId(),
            entity.getStudentId(),
            entity.getModule(),
            entity.getProfessor(),
            entity.getEcts(),
            entity.getCcGrade(),
            entity.getExamGrade(),
            entity.getPromoAverage(),
            entity.getSemester(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
    
    private StudentGradeJpaEntity toStudentGradeEntity(StudentGrade grade) {
        StudentGradeJpaEntity entity = new StudentGradeJpaEntity();
        entity.setId(grade.getId());
        entity.setStudentId(grade.getStudentId());
        entity.setModule(grade.getModule());
        entity.setProfessor(grade.getProfessor());
        entity.setEcts(grade.getEcts());
        entity.setCcGrade(grade.getCcGrade());
        entity.setExamGrade(grade.getExamGrade());
        entity.setPromoAverage(grade.getPromoAverage());
        entity.setSemester(grade.getSemester());
        entity.setCreatedAt(grade.getCreatedAt());
        entity.setUpdatedAt(grade.getUpdatedAt());
        return entity;
    }
}