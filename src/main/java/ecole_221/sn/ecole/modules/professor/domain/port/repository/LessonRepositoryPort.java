package ecole_221.sn.ecole.modules.professor.domain.port.repository;

import ecole_221.sn.ecole.modules.professor.domain.model.Lesson;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Output Port pour la persistance des leçons.
 * Définit les opérations de persistance pour l'agrégat Lesson.
 */
public interface LessonRepositoryPort {
    
    /**
     * Sauvegarde une leçon (création ou modification).
     */
    Lesson save(Lesson lesson);
    
    /**
     * Récupère une leçon par son ID.
     */
    Optional<Lesson> findById(String id);
    
    /**
     * Récupère toutes les leçons.
     */
    List<Lesson> findAll();
    
    /**
     * Supprime une leçon par son ID.
     */
    void deleteById(String id);
    
    /**
     * Récupère les leçons par cours.
     */
    List<Lesson> findByCourseId(String courseId);
    
    /**
     * Récupère les leçons par date.
     */
    List<Lesson> findByDate(LocalDate date);
    
    /**
     * Récupère les leçons entre deux dates.
     */
    List<Lesson> findByDateBetween(LocalDate startDate, LocalDate endDate);
}