package ecole_221.sn.ecole.modules.collaboration.application.port.usecase;

import ecole_221.sn.ecole.modules.collaboration.application.port.dto.WorkgroupDto;
import ecole_221.sn.ecole.modules.collaboration.application.port.dto.TaskDto;

import java.util.List;
import java.util.Map;

/**
 * Interface des cas d'utilisation pour le module Collaboration.
 * Gère les groupes de travail, tâches, documents, messages et réunions.
 */
public interface CollaborationUseCase {
    
    // ==================== GESTION DES GROUPES DE TRAVAIL ====================
    
    /**
     * Récupère tous les groupes de travail.
     */
    List<WorkgroupDto> getWorkgroups();
    
    /**
     * Crée un nouveau groupe de travail.
     */
    WorkgroupDto createWorkgroup(String name, String description, String creatorId);
    
    /**
     * Récupère un groupe de travail par son identifiant.
     */
    WorkgroupDto getWorkgroupById(String id);
    
    /**
     * Supprime un groupe de travail.
     */
    void deleteWorkgroup(String id);
    
    // ==================== GESTION DES TÂCHES ====================
    
    /**
     * Récupère toutes les tâches.
     */
    List<TaskDto> getTasks();
    
    /**
     * Récupère les tâches d'un groupe de travail.
     */
    List<TaskDto> getTasksByWorkgroup(String workgroupId);
    
    /**
     * Crée une nouvelle tâche.
     */
    TaskDto createTask(String workgroupId, String title, String status, String assignedTo);
    
    /**
     * Met à jour le statut d'une tâche.
     */
    void updateTaskStatus(String taskId, String status);
    
    // ==================== FONCTIONNALITÉS EXISTANTES (compatibilité) ====================
    
    /**
     * Récupère les documents d'un groupe (compatibilité).
     */
    List<Map<String, Object>> getDocuments(String groupId);
    
    /**
     * Ajoute un document (compatibilité).
     */
    void addDocument(String groupId, String name, String type, String size, String url, String addedBy);
    
    /**
     * Récupère les tâches d'un groupe (compatibilité).
     */
    List<Map<String, Object>> getTasksCompat(String groupId);
    
    /**
     * Ajoute une tâche (compatibilité).
     */
    void addTask(String groupId, String title, String status, String assignedTo);
    
    /**
     * Récupère les messages d'un groupe (compatibilité).
     */
    List<Map<String, Object>> getMessages(String groupId);
    
    /**
     * Ajoute un message (compatibilité).
     */
    void addMessage(String groupId, String sender, String content);
    
    /**
     * Récupère les réunions (compatibilité).
     */
    List<Map<String, Object>> getMeets();
    
    /**
     * Crée une réunion (compatibilité).
     */
    void createMeet(String title, String link, String time);
}
