package ecole_221.sn.ecole.modules.professor.domain.port.repository;

import ecole_221.sn.ecole.modules.professor.domain.model.Course;

import java.util.List;
import java.util.Optional;

/**
 * Output Port pour la persistance des cours.
 * Définit les opérations de persistance pour l'agrégat Course.
 */
public interface CourseRepositoryPort {
    
    /**
     * Sauvegarde un cours (création ou modification).
     */
    Course save(Course course);
    
    /**
     * Récupère un cours par son ID.
     */
    Optional<Course> findById(String id);
    
    /**
     * Récupère tous les cours.
     */
    List<Course> findAll();
    
    /**
     * Supprime un cours par son ID.
     */
    void deleteById(String id);
    
    /**
     * Récupère les cours par professeur.
     */
    List<Course> findByProfId(String profId);
    
    /**
     * Récupère les cours par classe.
     */
    List<Course> findByClasse(String classe);
    
    /**
     * Récupère les cours par code.
     */
    Optional<Course> findByCode(String code);
}