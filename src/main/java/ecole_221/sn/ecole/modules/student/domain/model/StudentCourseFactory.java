package ecole_221.sn.ecole.modules.student.domain.model;

import ecole_221.sn.ecole.modules.student.domain.valueobject.CourseStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Factory pour créer des instances de StudentCourse.
 * Permet d'accéder au constructeur privé de l'agrégat racine.
 */
public class StudentCourseFactory {
    
    /**
     * Crée un nouveau cours d'étudiant avec un ID généré et le statut "A_COMMENCER" par défaut.
     */
    public static StudentCourse create(String studentId, String courseId, String titre,
                                     int coefficient, String semester) {
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        return StudentCourse.createInternal(id, studentId, courseId, titre, coefficient, 0,
                              CourseStatus.A_COMMENCER, semester, now, now);
    }

    /**
     * Crée un nouveau cours d'étudiant avec un ID généré et un statut personnalisé.
     */
    public static StudentCourse create(String studentId, String courseId, String titre,
                                     int coefficient, int progress, CourseStatus statut, String semester) {
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        return StudentCourse.createInternal(id, studentId, courseId, titre, coefficient, progress,
                              statut, semester, now, now);
    }

    /**
     * Reconstruit un cours d'étudiant existant (pour la persistance).
     */
    public static StudentCourse reconstruct(String id, String studentId, String courseId, String titre,
                                          int coefficient, int progress, CourseStatus statut, String semester,
                                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        return StudentCourse.createInternal(id, studentId, courseId, titre, coefficient, progress,
                              statut, semester, createdAt, updatedAt);
    }
}