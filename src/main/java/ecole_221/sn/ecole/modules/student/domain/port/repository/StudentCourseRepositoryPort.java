package ecole_221.sn.ecole.modules.student.domain.port.repository;

import ecole_221.sn.ecole.modules.student.domain.model.StudentCourse;
import ecole_221.sn.ecole.modules.student.domain.valueobject.CourseStatus;

import java.util.List;
import java.util.Optional;

/**
 * Output Port pour la persistance des cours d'étudiants.
 * Définit les opérations de persistance pour l'agrégat StudentCourse.
 */
public interface StudentCourseRepositoryPort {
    
    /**
     * Sauvegarde un cours d'étudiant (création ou modification).
     */
    StudentCourse save(StudentCourse course);
    
    /**
     * Récupère un cours par son ID.
     */
    Optional<StudentCourse> findById(String id);
    
    /**
     * Récupère tous les cours d'étudiants.
     */
    List<StudentCourse> findAll();
    
    /**
     * Supprime un cours par son ID.
     */
    void deleteById(String id);
    
    /**
     * Récupère les cours par étudiant.
     */
    List<StudentCourse> findByStudentId(String studentId);
    
    /**
     * Récupère les cours par ID de cours.
     */
    List<StudentCourse> findByCourseId(String courseId);
    
    /**
     * Récupère les cours par statut.
     */
    List<StudentCourse> findByStatut(CourseStatus statut);
    
    /**
     * Récupère les cours par semestre.
     */
    List<StudentCourse> findBySemester(String semester);
    
    /**
     * Récupère les cours par étudiant et semestre.
     */
    List<StudentCourse> findByStudentIdAndSemester(String studentId, String semester);
}