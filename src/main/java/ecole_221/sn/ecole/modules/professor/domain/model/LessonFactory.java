package ecole_221.sn.ecole.modules.professor.domain.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Factory pour créer des instances de Lesson.
 * Permet d'accéder au constructeur privé de l'agrégat racine.
 */
public class LessonFactory {
    
    /**
     * Crée une nouvelle leçon avec un ID généré.
     */
    public static Lesson create(String courseId, String title, int duration, LocalDate date) {
        String id = UUID.randomUUID().toString();
        return Lesson.createInternal(id, courseId, title, duration, date);
    }

    /**
     * Reconstruit une leçon existante (pour la persistance).
     */
    public static Lesson reconstruct(String id, String courseId, String title, int duration, LocalDate date) {
        return Lesson.createInternal(id, courseId, title, duration, date);
    }
}