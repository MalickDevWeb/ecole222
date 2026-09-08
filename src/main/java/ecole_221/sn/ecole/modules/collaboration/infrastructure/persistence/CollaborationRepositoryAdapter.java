package ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence;

import ecole_221.sn.ecole.modules.collaboration.domain.exception.TaskNotFoundException;
import ecole_221.sn.ecole.modules.collaboration.domain.exception.WorkgroupNotFoundException;
import ecole_221.sn.ecole.modules.collaboration.domain.model.Workgroup;
import ecole_221.sn.ecole.modules.collaboration.domain.model.WorkgroupFactory;
import ecole_221.sn.ecole.modules.collaboration.domain.model.Task;
import ecole_221.sn.ecole.modules.collaboration.domain.model.TaskFactory;
import ecole_221.sn.ecole.modules.collaboration.domain.port.repository.CollaborationRepositoryPort;
import ecole_221.sn.ecole.modules.collaboration.domain.valueobject.TaskPriority;
import ecole_221.sn.ecole.modules.collaboration.domain.valueobject.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptateur de repository pour le module Collaboration.
 * Implémente CollaborationRepositoryPort en utilisant les repositories JPA existants.
 */
@Component
public class CollaborationRepositoryAdapter implements CollaborationRepositoryPort {
    
    private static final Logger logger = LoggerFactory.getLogger(CollaborationRepositoryAdapter.class);
    
    private final WorkgroupJpaRepository workgroupJpaRepository;
    private final TaskJpaRepository taskJpaRepository;
    
    public CollaborationRepositoryAdapter(WorkgroupJpaRepository workgroupJpaRepository,
                                        TaskJpaRepository taskJpaRepository) {
        this.workgroupJpaRepository = workgroupJpaRepository;
        this.taskJpaRepository = taskJpaRepository;
    }
    
    // ==================== REPOSITORY GROUPES DE TRAVAIL ====================
    
    @Override
    public Workgroup saveWorkgroup(Workgroup workgroup) {
        logger.debug("Sauvegarde du groupe de travail: {}", workgroup.getId());
        WorkgroupJpaEntity entity = toWorkgroupEntity(workgroup);
        WorkgroupJpaEntity savedEntity = workgroupJpaRepository.save(entity);
        return toWorkgroup(savedEntity);
    }
    
    @Override
    public Optional<Workgroup> findWorkgroupById(String id) {
        logger.debug("Récupération du groupe de travail: {}", id);
        return workgroupJpaRepository.findById(id)
            .map(this::toWorkgroup);
    }
    
    @Override
    public List<Workgroup> findAllWorkgroups() {
        logger.debug("Récupération de tous les groupes de travail depuis JPA");
        return workgroupJpaRepository.findAll().stream()
            .map(this::toWorkgroup)
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteWorkgroup(String id) {
        logger.debug("Suppression du groupe de travail: {}", id);
        if (!workgroupJpaRepository.existsById(id)) {
            throw new WorkgroupNotFoundException(id);
        }
        workgroupJpaRepository.deleteById(id);
    }
    
    @Override
    public List<Workgroup> findWorkgroupsByCreatorId(String creatorId) {
        logger.debug("Récupération des groupes de travail par créateur: {}", creatorId);
        // Note: Si le champ creatorId n'existe pas dans l'entité JPA, retourner tous les workgroups
        return workgroupJpaRepository.findAll().stream()
            .map(this::toWorkgroup)
            .collect(Collectors.toList());
    }
    
    // ==================== REPOSITORY TÂCHES ====================
    
    @Override
    public Task saveTask(Task task) {
        logger.debug("Sauvegarde de la tâche: {}", task.getId());
        TaskJpaEntity entity = toTaskEntity(task);
        TaskJpaEntity savedEntity = taskJpaRepository.save(entity);
        return toTask(savedEntity);
    }
    
    @Override
    public Optional<Task> findTaskById(String id) {
        logger.debug("Récupération de la tâche: {}", id);
        return taskJpaRepository.findById(id)
            .map(this::toTask);
    }
    
    @Override
    public List<Task> findAllTasks() {
        logger.debug("Récupération de toutes les tâches depuis JPA");
        return taskJpaRepository.findAll().stream()
            .map(this::toTask)
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteTask(String id) {
        logger.debug("Suppression de la tâche: {}", id);
        if (!taskJpaRepository.existsById(id)) {
            throw new TaskNotFoundException(id);
        }
        taskJpaRepository.deleteById(id);
    }
    
    @Override
    public List<Task> findTasksByWorkgroup(String workgroupId) {
        logger.debug("Récupération des tâches par groupe de travail: {}", workgroupId);
        return taskJpaRepository.findByGroupId(workgroupId).stream()
            .map(this::toTask)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Task> findTasksByAssignee(String assigneeId) {
        logger.debug("Récupération des tâches par assigné: {}", assigneeId);
        return taskJpaRepository.findByAssignedTo(assigneeId).stream()
            .map(this::toTask)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Task> findTasksByStatus(TaskStatus status) {
        logger.debug("Récupération des tâches par statut: {}", status);
        return taskJpaRepository.findByStatus(status.value()).stream()
            .map(this::toTask)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Task> findTasksByPriority(TaskPriority priority) {
        logger.debug("Récupération des tâches par priorité: {}", priority);
        return taskJpaRepository.findByPriority(priority.value()).stream()
            .map(this::toTask)
            .collect(Collectors.toList());
    }
    
    // ==================== MAPPERS JPA ↔ DOMAIN ====================
    
    private Workgroup toWorkgroup(WorkgroupJpaEntity entity) {
        return WorkgroupFactory.reconstruct(
            entity.getId(),
            entity.getName(),
            entity.getDescription(),
            entity.getCreatorId(),
            entity.getMembersCount()
        );
    }
    
    private WorkgroupJpaEntity toWorkgroupEntity(Workgroup workgroup) {
        WorkgroupJpaEntity entity = new WorkgroupJpaEntity();
        entity.setId(workgroup.getId());
        entity.setName(workgroup.getName());
        entity.setDescription(workgroup.getDescription());
        entity.setCreatorId(workgroup.getCreatorId());
        entity.setMembersCount(workgroup.getMemberCount());
        return entity;
    }
    
    private Task toTask(TaskJpaEntity entity) {
        return TaskFactory.reconstruct(
            entity.getId(),
            entity.getTitle(),
            null, // Pas de champ description dans l'entité JPA
            entity.getGroupId(),
            entity.getAssignedTo(),
            new TaskStatus(entity.getStatus()),
            new TaskPriority(entity.getPriority() != null ? entity.getPriority() : "MEDIUM")
        );
    }
    
    private TaskJpaEntity toTaskEntity(Task task) {
        TaskJpaEntity entity = new TaskJpaEntity();
        entity.setId(task.getId());
        entity.setTitle(task.getTitle());
        entity.setGroupId(task.getWorkgroupId());
        entity.setAssignedTo(task.getAssigneeId());
        entity.setStatus(task.getStatus().value());
        entity.setPriority(task.getPriority().value());
        return entity;
    }
    
    // ==================== MÉTHODES NON IMPLÉMENTÉES (pour compatibilité) ====================

    @Override
    public void removeParticipantFromGroupChat(String chatId, String participantId) {
        // Non implémenté pour l'instant - nécessite une entité GroupChat
        logger.warn("removeParticipantFromGroupChat non implémenté");
    }

    @Override
    public void deleteGroupChat(String id) {
        // Non implémenté pour l'instant - nécessite une entité GroupChat
        logger.warn("deleteGroupChat non implémenté");
    }

    @Override
    public List<ecole_221.sn.ecole.modules.collaboration.domain.model.GroupChat> findGroupChatsWithUnreadMessages(String userId) {
        // Non implémenté pour l'instant - nécessite une entité GroupChat
        logger.warn("findGroupChatsWithUnreadMessages non implémenté");
        return List.of();
    }

    @Override
    public List<ecole_221.sn.ecole.modules.collaboration.domain.model.GroupChat> findArchivedGroupChatsByUserId(String userId) {
        // Non implémenté pour l'instant - nécessite une entité GroupChat
        logger.warn("findArchivedGroupChatsByUserId non implémenté");
        return List.of();
    }

    @Override
    public ecole_221.sn.ecole.modules.collaboration.domain.model.GroupChat saveGroupChat(ecole_221.sn.ecole.modules.collaboration.domain.model.GroupChat groupChat) {
        // Non implémenté pour l'instant - nécessite une entité GroupChat
        logger.warn("saveGroupChat non implémenté");
        return groupChat;
    }

    @Override
    public java.util.Optional<ecole_221.sn.ecole.modules.collaboration.domain.model.GroupChat> findGroupChatById(String id) {
        // Non implémenté pour l'instant - nécessite une entité GroupChat
        logger.warn("findGroupChatById non implémenté");
        return java.util.Optional.empty();
    }

    @Override
    public List<ecole_221.sn.ecole.modules.collaboration.domain.model.GroupChat> findGroupChatsByUserId(String userId) {
        // Non implémenté pour l'instant - nécessite une entité GroupChat
        logger.warn("findGroupChatsByUserId non implémenté");
        return List.of();
    }

    @Override
    public List<ecole_221.sn.ecole.modules.collaboration.domain.model.GroupChat> findGroupChatsByCreatorId(String creatorId) {
        // Non implémenté pour l'instant - nécessite une entité GroupChat
        logger.warn("findGroupChatsByCreatorId non implémenté");
        return List.of();
    }

    @Override
    public List<ecole_221.sn.ecole.modules.collaboration.domain.model.GroupChat> findGroupChatsByType(ecole_221.sn.ecole.modules.collaboration.domain.valueobject.GroupChatType chatType) {
        // Non implémenté pour l'instant - nécessite une entité GroupChat
        logger.warn("findGroupChatsByType non implémenté");
        return List.of();
    }

    @Override
    public List<ecole_221.sn.ecole.modules.collaboration.domain.model.GroupChat> findActiveGroupChatsByUserId(String userId) {
        // Non implémenté pour l'instant - nécessite une entité GroupChat
        logger.warn("findActiveGroupChatsByUserId non implémenté");
        return List.of();
    }

    @Override
    public ecole_221.sn.ecole.modules.collaboration.domain.model.Message saveMessage(ecole_221.sn.ecole.modules.collaboration.domain.model.Message message) {
        // Non implémenté pour l'instant - nécessite une entité Message
        logger.warn("saveMessage non implémenté");
        return message;
    }

    @Override
    public java.util.Optional<ecole_221.sn.ecole.modules.collaboration.domain.model.Message> findMessageById(String id) {
        // Non implémenté pour l'instant - nécessite une entité Message
        logger.warn("findMessageById non implémenté");
        return java.util.Optional.empty();
    }

    @Override
    public List<ecole_221.sn.ecole.modules.collaboration.domain.model.Message> findMessagesByGroupId(String groupId) {
        // Non implémenté pour l'instant - nécessite une entité Message
        logger.warn("findMessagesByGroupId non implémenté");
        return List.of();
    }

    @Override
    public List<ecole_221.sn.ecole.modules.collaboration.domain.model.Message> findMessagesByGroupIdAfter(String groupId, java.time.LocalDateTime timestamp) {
        // Non implémenté pour l'instant - nécessite une entité Message
        logger.warn("findMessagesByGroupIdAfter non implémenté");
        return List.of();
    }

    @Override
    public List<ecole_221.sn.ecole.modules.collaboration.domain.model.Message> findMessagesBySenderId(String senderId) {
        // Non implémenté pour l'instant - nécessite une entité Message
        logger.warn("findMessagesBySenderId non implémenté");
        return List.of();
    }

    @Override
    public void deleteMessage(String id) {
        // Non implémenté pour l'instant - nécessite une entité Message
        logger.warn("deleteMessage non implémenté");
    }

    @Override
    public ecole_221.sn.ecole.modules.collaboration.domain.model.Meet saveMeet(ecole_221.sn.ecole.modules.collaboration.domain.model.Meet meet) {
        // Non implémenté pour l'instant - nécessite une entité Meet
        logger.warn("saveMeet non implémenté");
        return meet;
    }

    @Override
    public java.util.Optional<ecole_221.sn.ecole.modules.collaboration.domain.model.Meet> findMeetById(String id) {
        // Non implémenté pour l'instant - nécessite une entité Meet
        logger.warn("findMeetById non implémenté");
        return java.util.Optional.empty();
    }

    @Override
    public List<ecole_221.sn.ecole.modules.collaboration.domain.model.Meet> findMeetsByOrganizerId(String organizerId) {
        // Non implémenté pour l'instant - nécessite une entité Meet
        logger.warn("findMeetsByOrganizerId non implémenté");
        return List.of();
    }

    @Override
    public List<ecole_221.sn.ecole.modules.collaboration.domain.model.Meet> findMeetsByParticipantId(String participantId) {
        // Non implémenté pour l'instant - nécessite une entité Meet
        logger.warn("findMeetsByParticipantId non implémenté");
        return List.of();
    }

    @Override
    public List<ecole_221.sn.ecole.modules.collaboration.domain.model.Meet> findActiveMeets() {
        // Non implémenté pour l'instant - nécessite une entité Meet
        logger.warn("findActiveMeets non implémenté");
        return List.of();
    }

    @Override
    public List<ecole_221.sn.ecole.modules.collaboration.domain.model.Meet> findUpcomingMeets(java.time.LocalDateTime before) {
        // Non implémenté pour l'instant - nécessite une entité Meet
        logger.warn("findUpcomingMeets non implémenté");
        return List.of();
    }

    @Override
    public void deleteMeet(String id) {
        // Non implémenté pour l'instant - nécessite une entité Meet
        logger.warn("deleteMeet non implémenté");
    }

    @Override
    public ecole_221.sn.ecole.modules.collaboration.domain.model.AccessControl saveAccessControl(ecole_221.sn.ecole.modules.collaboration.domain.model.AccessControl accessControl) {
        // Non implémenté pour l'instant - nécessite une entité AccessControl
        logger.warn("saveAccessControl non implémenté");
        return accessControl;
    }

    @Override
    public java.util.Optional<ecole_221.sn.ecole.modules.collaboration.domain.model.AccessControl> findAccessControlById(String id) {
        // Non implémenté pour l'instant - nécessite une entité AccessControl
        logger.warn("findAccessControlById non implémenté");
        return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<ecole_221.sn.ecole.modules.collaboration.domain.model.AccessControl> findAccessControlByUserAndResource(String userId, String resourceId, String resourceType) {
        // Non implémenté pour l'instant - nécessite une entité AccessControl
        logger.warn("findAccessControlByUserAndResource non implémenté");
        return java.util.Optional.empty();
    }

    @Override
    public List<ecole_221.sn.ecole.modules.collaboration.domain.model.AccessControl> findAccessControlsByUserId(String userId) {
        // Non implémenté pour l'instant - nécessite une entité AccessControl
        logger.warn("findAccessControlsByUserId non implémenté");
        return List.of();
    }

    @Override
    public List<ecole_221.sn.ecole.modules.collaboration.domain.model.AccessControl> findActiveAccessControlsByUserId(String userId) {
        // Non implémenté pour l'instant - nécessite une entité AccessControl
        logger.warn("findActiveAccessControlsByUserId non implémenté");
        return List.of();
    }

    @Override
    public List<ecole_221.sn.ecole.modules.collaboration.domain.model.AccessControl> findAccessControlsByResourceId(String resourceId, String resourceType) {
        // Non implémenté pour l'instant - nécessite une entité AccessControl
        logger.warn("findAccessControlsByResourceId non implémenté");
        return List.of();
    }

    @Override
    public List<ecole_221.sn.ecole.modules.collaboration.domain.model.AccessControl> findAccessControlsByUserIdAndPermission(String userId, ecole_221.sn.ecole.modules.collaboration.domain.valueobject.Permission permission) {
        // Non implémenté pour l'instant - nécessite une entité AccessControl
        logger.warn("findAccessControlsByUserIdAndPermission non implémenté");
        return List.of();
    }

    @Override
    public void deleteAccessControl(String id) {
        // Non implémenté pour l'instant - nécessite une entité AccessControl
        logger.warn("deleteAccessControl non implémenté");
    }

    @Override
    public void revokeAccessControl(String userId, String resourceId, String resourceType) {
        // Non implémenté pour l'instant - nécessite une entité AccessControl
        logger.warn("revokeAccessControl non implémenté");
    }
}