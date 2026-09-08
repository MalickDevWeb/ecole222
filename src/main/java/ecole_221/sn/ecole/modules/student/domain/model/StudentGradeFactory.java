package ecole_221.sn.ecole.modules.student.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Factory pour créer des instances de StudentGrade.
 * Permet d'accéder au constructeur privé de l'agrégat racine.
 */
public class StudentGradeFactory {
    
    /**
     * Crée une nouvelle note d'étudiant avec un ID généré et l'horodatage actuel.
     */
    public static StudentGrade create(String studentId, String module, String professor,
                                     int ects, double ccGrade, double examGrade,
                                     double promoAverage, String semester) {
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        return StudentGrade.createInternal(id, studentId, module, professor, ects, ccGrade, examGrade,
                              promoAverage, semester, now, now);
    }

    /**
     * Reconstruit une note d'étudiant existante (pour la persistance).
     */
    public static StudentGrade reconstruct(String id, String studentId, String module, String professor,
                                         int ects, double ccGrade, double examGrade, double promoAverage,
                                         String semester, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return StudentGrade.createInternal(id, studentId, module, professor, ects, ccGrade, examGrade,
                              promoAverage, semester, createdAt, updatedAt);
    }
}