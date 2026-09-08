package ecole_221.sn.ecole.modules.professor.domain.port.repository;

import ecole_221.sn.ecole.modules.professor.domain.model.Homework;
import ecole_221.sn.ecole.modules.professor.domain.valueobject.Priority;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Output Port pour la persistance des devoirs.
 * Définit les opérations de persistance pour l'agrégat Homework.
 */
public interface HomeworkRepositoryPort {
    
    /**
     * Sauvegarde un devoir (création ou modification).
     */
    Homework save(Homework homework);
    
    /**
     * Récupère un devoir par son ID.
     */
    Optional<Homework> findById(String id);
    
    /**
     * Récupère tous les devoirs.
     */
    List<Homework> findAll();
    
    /**
     * Supprime un devoir par son ID.
     */
    void deleteById(String id);
    
    /**
     * Récupère les devoirs par cours.
     */
    List<Homework> findByCourseId(String courseId);
    
    /**
     * Récupère les devoirs par priorité.
     */
    List<Homework> findByPriority(Priority priority);
    
    /**
     * Récupère les devoirs en retard.
     */
    List<Homework> findOverdue();
    
    /**
     * Récupère les devoirs dus avant une date.
     */
    List<Homework> findByDeadlineBefore(LocalDate date);
}