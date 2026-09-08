package ecole_221.sn.ecole.modules.professor.domain.model;

import ecole_221.sn.ecole.modules.professor.domain.valueobject.Priority;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Factory pour créer des instances de Homework.
 * Permet d'accéder au constructeur privé de l'agrégat racine.
 */
public class HomeworkFactory {
    
    /**
     * Crée un nouveau devoir avec un ID généré et la priorité MEDIUM par défaut.
     */
    public static Homework create(String courseId, String title, String description, LocalDate deadline) {
        String id = UUID.randomUUID().toString();
        return Homework.createInternal(id, courseId, title, description, Priority.MEDIUM, deadline);
    }

    /**
     * Crée un nouveau devoir avec un ID généré et une priorité personnalisée.
     */
    public static Homework create(String courseId, String title, String description,
                                Priority priority, LocalDate deadline) {
        String id = UUID.randomUUID().toString();
        return Homework.createInternal(id, courseId, title, description, priority, deadline);
    }

    /**
     * Reconstruit un devoir existant (pour la persistance).
     */
    public static Homework reconstruct(String id, String courseId, String title, String description,
                                     Priority priority, LocalDate deadline) {
        return Homework.createInternal(id, courseId, title, description, priority, deadline);
    }
}