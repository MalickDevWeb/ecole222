package ecole_221.sn.ecole.modules.professor.infrastructure.persistence;

import ecole_221.sn.ecole.modules.professor.domain.model.Lesson;
import ecole_221.sn.ecole.modules.professor.domain.model.LessonFactory;
import ecole_221.sn.ecole.modules.professor.domain.port.repository.LessonRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptateur de repository pour l'agrégat Lesson.
 * Implémente LessonRepositoryPort en utilisant LessonJpaRepository.
 */
@Component
public class LessonRepositoryAdapter implements LessonRepositoryPort {
    
    private static final Logger logger = LoggerFactory.getLogger(LessonRepositoryAdapter.class);
    
    private final LessonJpaRepository lessonJpaRepository;
    
    public LessonRepositoryAdapter(LessonJpaRepository lessonJpaRepository) {
        this.lessonJpaRepository = lessonJpaRepository;
    }
    
    @Override
    public Lesson save(Lesson lesson) {
        logger.debug("Sauvegarde de la leçon: {}", lesson.getId());
        LessonJpaEntity entity = toLessonEntity(lesson);
        LessonJpaEntity savedEntity = lessonJpaRepository.save(entity);
        return toLesson(savedEntity);
    }
    
    @Override
    public Optional<Lesson> findById(String id) {
        logger.debug("Récupération de la leçon: {}", id);
        return lessonJpaRepository.findById(id)
            .map(this::toLesson);
    }
    
    @Override
    public List<Lesson> findAll() {
        logger.debug("Récupération de toutes les leçons");
        return lessonJpaRepository.findAll().stream()
            .map(this::toLesson)
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(String id) {
        logger.debug("Suppression de la leçon: {}", id);
        lessonJpaRepository.deleteById(id);
    }
    
    @Override
    public List<Lesson> findByCourseId(String courseId) {
        logger.debug("Récupération des leçons par cours: {}", courseId);
        return lessonJpaRepository.findByCourseId(courseId).stream()
            .map(this::toLesson)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Lesson> findByDate(LocalDate date) {
        logger.debug("Récupération des leçons par date: {}", date);
        return lessonJpaRepository.findByDate(date.toString()).stream()
            .map(this::toLesson)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Lesson> findByDateBetween(LocalDate startDate, LocalDate endDate) {
        logger.debug("Récupération des leçons entre {} et {}", startDate, endDate);
        return lessonJpaRepository.findByDateBetween(startDate.toString(), endDate.toString()).stream()
            .map(this::toLesson)
            .collect(Collectors.toList());
    }
    
    // ==================== MAPPERS JPA ↔ DOMAIN ====================
    
    private Lesson toLesson(LessonJpaEntity entity) {
        return LessonFactory.reconstruct(
            entity.getId(),
            entity.getCourseId(),
            entity.getTitle(),
            entity.getDuration(),
            LocalDate.parse(entity.getDate())
        );
    }
    
    private LessonJpaEntity toLessonEntity(Lesson lesson) {
        LessonJpaEntity entity = new LessonJpaEntity();
        entity.setId(lesson.getId());
        entity.setCourseId(lesson.getCourseId());
        entity.setTitle(lesson.getTitle());
        entity.setDuration(lesson.getDuration());
        entity.setDate(lesson.getDate().toString());
        return entity;
    }
}