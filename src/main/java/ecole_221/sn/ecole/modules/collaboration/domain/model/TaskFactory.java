package ecole_221.sn.ecole.modules.collaboration.domain.model;

import ecole_221.sn.ecole.modules.collaboration.domain.valueobject.TaskPriority;
import ecole_221.sn.ecole.modules.collaboration.domain.valueobject.TaskStatus;

import java.util.UUID;

/**
 * Factory pour créer des instances de Task.
 * Permet d'accéder au constructeur privé de l'agrégat racine.
 */
public class TaskFactory {
    
    /**
     * Crée une nouvelle tâche avec un ID généré et le statut "TODO" par défaut.
     */
    public static Task create(String title, String description, String workgroupId, String assigneeId, 
                           TaskPriority priority) {
        String id = UUID.randomUUID().toString();
        return new Task(id, title, description, workgroupId, assigneeId, TaskStatus.TODO, priority);
    }
    
    /**
     * Crée une nouvelle tâche avec un ID généré et un statut personnalisé.
     */
    public static Task create(String title, String description, String workgroupId, String assigneeId, 
                           TaskStatus status, TaskPriority priority) {
        String id = UUID.randomUUID().toString();
        return new Task(id, title, description, workgroupId, assigneeId, status, priority);
    }
    
    /**
     * Reconstruit une tâche existante (pour la persistance).
     */
    public static Task reconstruct(String id, String title, String description, String workgroupId, 
                                String assigneeId, TaskStatus status, TaskPriority priority) {
        return new Task(id, title, description, workgroupId, assigneeId, status, priority);
    }
    
    /**
     * Crée une tâche avec des paramètres String (compatibilité service).
     */
    public static Task createTask(String workgroupId, String title, String status, String assignedTo) {
        String id = UUID.randomUUID().toString();
        TaskStatus taskStatus = TaskStatus.valueOf(status.toUpperCase());
        TaskPriority priority = TaskPriority.MEDIUM; // Valeur par défaut
        return new Task(id, title, null, workgroupId, assignedTo, taskStatus, priority);
    }
}