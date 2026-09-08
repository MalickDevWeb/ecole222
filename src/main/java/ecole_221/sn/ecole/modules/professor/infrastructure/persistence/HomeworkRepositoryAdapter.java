package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import ecole_221.sn.ecole.modules.professor.domain.model.Homework;
import ecole_221.sn.ecole.modules.professor.domain.model.HomeworkFactory;
import ecole_221.sn.ecole.modules.professor.domain.port.repository.HomeworkRepositoryPort;
import ecole_221.sn.ecole.modules.professor.domain.valueobject.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptateur de repository pour l'agrégat Homework.
 * Implémente HomeworkRepositoryPort en utilisant HomeworkJpaRepository.
 */
@Component
public class HomeworkRepositoryAdapter implements HomeworkRepositoryPort {
    
    private static final Logger logger = LoggerFactory.getLogger(HomeworkRepositoryAdapter.class);
    
    private final HomeworkJpaRepository homeworkJpaRepository;
    
    public HomeworkRepositoryAdapter(HomeworkJpaRepository homeworkJpaRepository) {
        this.homeworkJpaRepository = homeworkJpaRepository;
    }
    
    @Override
    public Homework save(Homework homework) {
        logger.debug("Sauvegarde du devoir: {}", homework.getId());
        HomeworkJpaEntity entity = toHomeworkEntity(homework);
        HomeworkJpaEntity savedEntity = homeworkJpaRepository.save(entity);
        return toHomework(savedEntity);
    }
    
    @Override
    public Optional<Homework> findById(String id) {
        logger.debug("Récupération du devoir: {}", id);
        return homeworkJpaRepository.findById(id)
            .map(this::toHomework);
    }
    
    @Override
    public List<Homework> findAll() {
        logger.debug("Récupération de tous les devoirs");
        return homeworkJpaRepository.findAll().stream()
            .map(this::toHomework)
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(String id) {
        logger.debug("Suppression du devoir: {}", id);
        homeworkJpaRepository.deleteById(id);
    }
    
    @Override
    public List<Homework> findByCourseId(String courseId) {
        logger.debug("Récupération des devoirs par cours: {}", courseId);
        return homeworkJpaRepository.findByCourseId(courseId).stream()
            .map(this::toHomework)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Homework> findByPriority(Priority priority) {
        logger.debug("Récupération des devoirs par priorité: {}", priority);
        return homeworkJpaRepository.findByPriority(priority.value()).stream()
            .map(this::toHomework)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Homework> findOverdue() {
        logger.debug("Récupération des devoirs en retard");
        String today = LocalDate.now().toString();
        return homeworkJpaRepository.findByDeadlineBefore(today).stream()
            .map(this::toHomework)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Homework> findByDeadlineBefore(LocalDate date) {
        logger.debug("Récupération des devoirs dus avant: {}", date);
        return homeworkJpaRepository.findByDeadlineBefore(date.toString()).stream()
            .map(this::toHomework)
            .collect(Collectors.toList());
    }
    
    // ==================== MAPPERS JPA ↔ DOMAIN ====================
    
    private Homework toHomework(HomeworkJpaEntity entity) {
        return HomeworkFactory.reconstruct(
            entity.getId(),
            entity.getCourseId(),
            entity.getTitle(),
            entity.getDescription(),
            new Priority(entity.getPriority()),
            LocalDate.parse(entity.getDeadline())
        );
    }
    
    private HomeworkJpaEntity toHomeworkEntity(Homework homework) {
        HomeworkJpaEntity entity = new HomeworkJpaEntity();
        entity.setId(homework.getId());
        entity.setCourseId(homework.getCourseId());
        entity.setTitle(homework.getTitle());
        entity.setDescription(homework.getDescription());
        entity.setPriority(homework.getPriority().value());
        entity.setDeadline(homework.getDeadline().toString());
        return entity;
    }
}