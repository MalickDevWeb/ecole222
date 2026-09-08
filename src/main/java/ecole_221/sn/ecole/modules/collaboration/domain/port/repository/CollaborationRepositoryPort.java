package ecole_221.sn.ecole.modules.collaboration.domain.port.repository;

import ecole_221.sn.ecole.modules.collaboration.domain.model.Workgroup;
import ecole_221.sn.ecole.modules.collaboration.domain.model.Task;
import ecole_221.sn.ecole.modules.collaboration.domain.model.Message;
import ecole_221.sn.ecole.modules.collaboration.domain.model.Meet;
import ecole_221.sn.ecole.modules.collaboration.domain.model.AccessControl;
import ecole_221.sn.ecole.modules.collaboration.domain.model.GroupChat;
import ecole_221.sn.ecole.modules.collaboration.domain.valueobject.TaskPriority;
import ecole_221.sn.ecole.modules.collaboration.domain.valueobject.TaskStatus;
import ecole_221.sn.ecole.modules.collaboration.domain.valueobject.Permission;
import ecole_221.sn.ecole.modules.collaboration.domain.valueobject.GroupChatType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Output Ports pour la persistance des entités de collaboration.
 * Définit les opérations de persistance pour tous les agrégats du module Collaboration.
 */
public interface CollaborationRepositoryPort {
    
    // ==================== REPOSITORY GROUPES DE TRAVAIL ====================
    
    /**
     * Sauvegarde un groupe de travail (création ou modification).
     */
    Workgroup saveWorkgroup(Workgroup workgroup);
    
    /**
     * Récupère un groupe de travail par son identifiant.
     */
    Optional<Workgroup> findWorkgroupById(String id);
    
    /**
     * Récupère tous les groupes de travail.
     */
    List<Workgroup> findAllWorkgroups();
    
    /**
     * Supprime un groupe de travail par son ID.
     */
    void deleteWorkgroup(String id);
    
    /**
     * Récupère les groupes de travail par créateur.
     */
    List<Workgroup> findWorkgroupsByCreatorId(String creatorId);
    
    // ==================== REPOSITORY TÂCHES ====================
    
    /**
     * Sauvegarde une tâche (création ou modification).
     */
    Task saveTask(Task task);
    
    /**
     * Récupère une tâche par son identifiant.
     */
    Optional<Task> findTaskById(String id);
    
    /**
     * Récupère toutes les tâches.
     */
    List<Task> findAllTasks();
    
    /**
     * Supprime une tâche par son ID.
     */
    void deleteTask(String id);
    
    /**
     * Récupère les tâches par groupe de travail.
     */
    List<Task> findTasksByWorkgroup(String workgroupId);
    
    /**
     * Récupère les tâches par assigné.
     */
    List<Task> findTasksByAssignee(String assigneeId);
    
    /**
     * Récupère les tâches par statut.
     */
    List<Task> findTasksByStatus(TaskStatus status);
    
    /**
     * Récupère les tâches par priorité.
     */
    List<Task> findTasksByPriority(TaskPriority priority);
    
    // ==================== REPOSITORY MESSAGES (Type WhatsApp) ====================
    
    /**
     * Sauvegarde un message.
     */
    Message saveMessage(Message message);
    
    /**
     * Récupère un message par son identifiant.
     */
    Optional<Message> findMessageById(String id);
    
    /**
     * Récupère les messages d'un groupe.
     */
    List<Message> findMessagesByGroupId(String groupId);
    
    /**
     * Récupère les messages récents d'un groupe.
     */
    List<Message> findMessagesByGroupIdAfter(String groupId, LocalDateTime timestamp);
    
    /**
     * Récupère les messages d'un expéditeur.
     */
    List<Message> findMessagesBySenderId(String senderId);
    
    /**
     * Supprime un message par son ID.
     */
    void deleteMessage(String id);
    
    // ==================== REPOSITORY RÉUNIONS (Visioconférences) ====================
    
    /**
     * Sauvegarde une réunion.
     */
    Meet saveMeet(Meet meet);
    
    /**
     * Récupère une réunion par son identifiant.
     */
    Optional<Meet> findMeetById(String id);
    
    /**
     * Récupère les réunions organisées par un utilisateur.
     */
    List<Meet> findMeetsByOrganizerId(String organizerId);
    
    /**
     * Récupère les réunions où l'utilisateur est participant.
     */
    List<Meet> findMeetsByParticipantId(String participantId);
    
    /**
     * Récupère les réunions actives.
     */
    List<Meet> findActiveMeets();
    
    /**
     * Récupère les réunions à venir.
     */
    List<Meet> findUpcomingMeets(LocalDateTime before);
    
    /**
     * Supprime une réunion par son ID.
     */
    void deleteMeet(String id);
    
    // ==================== REPOSITORY CONTRÔLE D'ACCÈS (Permissions) ====================
    
    /**
     * Sauvegarde un contrôle d'accès.
     */
    AccessControl saveAccessControl(AccessControl accessControl);
    
    /**
     * Récupère un contrôle d'accès par son identifiant.
     */
    Optional<AccessControl> findAccessControlById(String id);
    
    /**
     * Récupère le contrôle d'accès d'un utilisateur sur une ressource.
     */
    Optional<AccessControl> findAccessControlByUserAndResource(String userId, String resourceId, String resourceType);
    
    /**
     * Récupère tous les contrôles d'accès d'un utilisateur.
     */
    List<AccessControl> findAccessControlsByUserId(String userId);
    
    /**
     * Récupère les contrôles d'accès actifs d'un utilisateur.
     */
    List<AccessControl> findActiveAccessControlsByUserId(String userId);
    
    /**
     * Récupère les contrôles d'accès pour une ressource.
     */
    List<AccessControl> findAccessControlsByResourceId(String resourceId, String resourceType);
    
    /**
     * Récupère les contrôles d'accès avec une permission spécifique.
     */
    List<AccessControl> findAccessControlsByUserIdAndPermission(String userId, Permission permission);
    
    /**
     * Supprime un contrôle d'accès par son ID.
     */
    void deleteAccessControl(String id);
    
    /**
     * Révoque l'accès d'un utilisateur à une ressource.
     */
    void revokeAccessControl(String userId, String resourceId, String resourceType);
    
    // ==================== REPOSITORY DISCUSSIONS DE GROUPE (Type WhatsApp) ====================
    
    /**
     * Sauvegarde une discussion de groupe.
     */
    GroupChat saveGroupChat(GroupChat groupChat);
    
    /**
     * Récupère une discussion de groupe par son identifiant.
     */
    Optional<GroupChat> findGroupChatById(String id);
    
    /**
     * Récupère les discussions d'un utilisateur.
     */
    List<GroupChat> findGroupChatsByUserId(String userId);
    
    /**
     * Récupère les discussions créées par un utilisateur.
     */
    List<GroupChat> findGroupChatsByCreatorId(String creatorId);
    
    /**
     * Récupère les discussions par type.
     */
    List<GroupChat> findGroupChatsByType(GroupChatType chatType);
    
    /**
     * Récupère les discussions actives d'un utilisateur.
     */
    List<GroupChat> findActiveGroupChatsByUserId(String userId);
    
    /**
     * Récupère les discussions archivées d'un utilisateur.
     */
    List<GroupChat> findArchivedGroupChatsByUserId(String userId);
    
    /**
     * Récupère les discussions avec messages non lus.
     */
    List<GroupChat> findGroupChatsWithUnreadMessages(String userId);
    
    /**
     * Supprime une discussion de groupe par son ID.
     */
    void deleteGroupChat(String id);
    
    /**
     * Retire un participant d'une discussion.
     */
    void removeParticipantFromGroupChat(String chatId, String participantId);
}