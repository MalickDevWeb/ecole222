package ecole_221.sn.ecole.modules.student.domain.port.repository;

import ecole_221.sn.ecole.modules.student.domain.model.Attendance;
import ecole_221.sn.ecole.modules.student.domain.valueobject.AttendanceStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Output Port pour la persistance des présences.
 * Définit les opérations de persistance pour l'agrégat Attendance.
 */
public interface AttendanceRepositoryPort {
    
    /**
     * Sauvegarde une présence (création ou modification).
     */
    Attendance save(Attendance attendance);
    
    /**
     * Récupère une présence par son ID.
     */
    Optional<Attendance> findById(String id);
    
    /**
     * Récupère toutes les présences.
     */
    List<Attendance> findAll();
    
    /**
     * Supprime une présence par son ID.
     */
    void deleteById(String id);
    
    /**
     * Récupère les présences par étudiant.
     */
    List<Attendance> findByStudentId(String studentId);
    
    /**
     * Récupère les présences par cours.
     */
    List<Attendance> findByCourseId(String courseId);
    
    /**
     * Récupère les présences par date.
     */
    List<Attendance> findByDate(LocalDate date);
    
    /**
     * Récupère les présences par statut.
     */
    List<Attendance> findByStatus(AttendanceStatus status);
    
    /**
     * Récupère les présences par étudiant et cours.
     */
    List<Attendance> findByStudentIdAndCourseId(String studentId, String courseId);
}