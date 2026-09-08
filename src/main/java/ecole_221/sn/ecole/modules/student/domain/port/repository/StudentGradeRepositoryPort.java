package ecole_221.sn.ecole.modules.student.domain.port.repository;

import ecole_221.sn.ecole.modules.student.domain.model.StudentGrade;

import java.util.List;
import java.util.Optional;

/**
 * Output Port pour la persistance des notes d'étudiants.
 * Définit les opérations de persistance pour l'agrégat StudentGrade.
 */
public interface StudentGradeRepositoryPort {
    
    /**
     * Sauvegarde une note d'étudiant (création ou modification).
     */
    StudentGrade save(StudentGrade grade);
    
    /**
     * Récupère une note par son ID.
     */
    Optional<StudentGrade> findById(String id);
    
    /**
     * Récupère toutes les notes d'étudiants.
     */
    List<StudentGrade> findAll();
    
    /**
     * Supprime une note par son ID.
     */
    void deleteById(String id);
    
    /**
     * Récupère les notes par étudiant.
     */
    List<StudentGrade> findByStudentId(String studentId);
    
    /**
     * Récupère les notes par module.
     */
    List<StudentGrade> findByModule(String module);
    
    /**
     * Récupère les notes par semestre.
     */
    List<StudentGrade> findBySemester(String semester);
    
    /**
     * Récupère les notes par étudiant et semestre.
     */
    List<StudentGrade> findByStudentIdAndSemester(String studentId, String semester);
}