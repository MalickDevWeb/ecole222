package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import ecole_221.sn.ecole.modules.professor.domain.exception.CourseNotFoundException;
import ecole_221.sn.ecole.modules.professor.domain.model.Course;
import ecole_221.sn.ecole.modules.professor.domain.model.CourseFactory;
import ecole_221.sn.ecole.modules.professor.domain.port.repository.CourseRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptateur de repository pour l'agrégat Course.
 * Implémente CourseRepositoryPort en utilisant CourseJpaRepository.
 */
@Component
public class CourseRepositoryAdapter implements CourseRepositoryPort {
    
    private static final Logger logger = LoggerFactory.getLogger(CourseRepositoryAdapter.class);
    
    private final CourseJpaRepository courseJpaRepository;
    
    public CourseRepositoryAdapter(CourseJpaRepository courseJpaRepository) {
        this.courseJpaRepository = courseJpaRepository;
    }
    
    @Override
    public Course save(Course course) {
        logger.debug("Sauvegarde du cours: {}", course.getId());
        CourseJpaEntity entity = toCourseEntity(course);
        CourseJpaEntity savedEntity = courseJpaRepository.save(entity);
        return toCourse(savedEntity);
    }
    
    @Override
    public Optional<Course> findById(String id) {
        logger.debug("Récupération du cours: {}", id);
        return courseJpaRepository.findById(id)
            .map(this::toCourse);
    }
    
    @Override
    public List<Course> findAll() {
        logger.debug("Récupération de tous les cours");
        return courseJpaRepository.findAll().stream()
            .map(this::toCourse)
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(String id) {
        logger.debug("Suppression du cours: {}", id);
        if (!courseJpaRepository.existsById(id)) {
            throw new CourseNotFoundException(id);
        }
        courseJpaRepository.deleteById(id);
    }
    
    @Override
    public List<Course> findByProfId(String profId) {
        logger.debug("Récupération des cours par professeur: {}", profId);
        return courseJpaRepository.findByProfId(profId).stream()
            .map(this::toCourse)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Course> findByClasse(String classe) {
        logger.debug("Récupération des cours par classe: {}", classe);
        return courseJpaRepository.findByClasse(classe).stream()
            .map(this::toCourse)
            .collect(Collectors.toList());
    }
    
    @Override
    public Optional<Course> findByCode(String code) {
        logger.debug("Récupération du cours par code: {}", code);
        return courseJpaRepository.findByCode(code)
            .map(this::toCourse);
    }
    
    // ==================== MAPPERS JPA ↔ DOMAIN ====================
    
    private Course toCourse(CourseJpaEntity entity) {
        return CourseFactory.reconstruct(
            entity.getId(),
            entity.getName(),
            entity.getClasse(),
            entity.getTotalHours(),
            entity.getProfId(),
            entity.getCode(),
            entity.getDescription()
        );
    }
    
    private CourseJpaEntity toCourseEntity(Course course) {
        CourseJpaEntity entity = new CourseJpaEntity();
        entity.setId(course.getId());
        entity.setName(course.getName());
        entity.setClasse(course.getClasse());
        entity.setTotalHours(course.getTotalHours());
        entity.setProfId(course.getProfId());
        entity.setCode(course.getCode());
        entity.setDescription(course.getDescription());
        return entity;
    }
}