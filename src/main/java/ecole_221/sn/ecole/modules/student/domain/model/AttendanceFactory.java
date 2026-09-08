package ecole_221.sn.ecole.modules.student.domain.model;

import ecole_221.sn.ecole.modules.student.domain.valueobject.AttendanceStatus;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Factory pour créer des instances de Attendance.
 * Permet d'accéder au constructeur privé de l'agrégat racine.
 */
public class AttendanceFactory {
    
    /**
     * Crée une nouvelle présence avec un ID généré et le statut "UNKNOWN" par défaut.
     */
    public static Attendance create(LocalDate date, String studentId, String studentName, String courseId) {
        String id = UUID.randomUUID().toString();
        return Attendance.createInternal(id, date, studentId, studentName, AttendanceStatus.UNKNOWN, courseId);
    }

    /**
     * Crée une nouvelle présence avec un ID généré et un statut personnalisé.
     */
    public static Attendance create(LocalDate date, String studentId, String studentName,
                                   AttendanceStatus status, String courseId) {
        String id = UUID.randomUUID().toString();
        return Attendance.createInternal(id, date, studentId, studentName, status, courseId);
    }

    /**
     * Reconstruit une présence existante (pour la persistance).
     */
    public static Attendance reconstruct(String id, LocalDate date, String studentId, String studentName,
                                       AttendanceStatus status, String courseId) {
        return Attendance.createInternal(id, date, studentId, studentName, status, courseId);
    }
}