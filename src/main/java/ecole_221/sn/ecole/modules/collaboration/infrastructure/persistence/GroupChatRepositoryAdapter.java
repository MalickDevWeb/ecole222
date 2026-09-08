package ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence;

import ecole_221.sn.ecole.modules.collaboration.domain.model.GroupChat;
import ecole_221.sn.ecole.modules.collaboration.domain.model.GroupChatFactory;
import ecole_221.sn.ecole.modules.collaboration.domain.port.repository.GroupChatRepositoryPort;
import ecole_221.sn.ecole.modules.collaboration.domain.valueobject.GroupChatType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class GroupChatRepositoryAdapter implements GroupChatRepositoryPort {
    
    private static final Logger logger = LoggerFactory.getLogger(GroupChatRepositoryAdapter.class);
    private final GroupChatJpaRepository jpaRepository;
    
    public GroupChatRepositoryAdapter(GroupChatJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public GroupChat save(GroupChat groupChat) {
        try {
            GroupChatJpaEntity entity = toEntity(groupChat);
            GroupChatJpaEntity saved = jpaRepository.save(entity);
            logger.info("GroupChat sauvegardé: {}", saved.getId());
            return toDomain(saved);
        } catch (Exception e) {
            logger.error("Erreur lors de la sauvegarde du GroupChat: {}", groupChat.getId(), e);
            throw new RuntimeException("Impossible de sauvegarder le GroupChat", e);
        }
    }
    
    @Override
    public Optional<GroupChat> findById(String id) {
        try {
            return jpaRepository.findById(id)
                    .map(this::toDomain);
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche du GroupChat: {}", id, e);
            return Optional.empty();
        }
    }
    
    @Override
    public List<GroupChat> findByUserId(String userId) {
        try {
            return jpaRepository.findAllChatsByUserId(userId)
                    .stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche des GroupChats pour l'utilisateur: {}", userId, e);
            return List.of();
        }
    }
    
    @Override
    public List<GroupChat> findByCreatorId(String creatorId) {
        try {
            return jpaRepository.findByCreatorId(creatorId)
                    .stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche des GroupChats créés par: {}", creatorId, e);
            return List.of();
        }
    }
    
    @Override
    public List<GroupChat> findByType(GroupChatType chatType) {
        try {
            return jpaRepository.findByChatType(chatType.type())
                    .stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche des GroupChats de type: {}", chatType, e);
            return List.of();
        }
    }
    
    @Override
    public List<GroupChat> findActiveChatsByUserId(String userId) {
        try {
            return jpaRepository.findActiveChatsByUserId(userId)
                    .stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche des GroupChats actifs pour: {}", userId, e);
            return List.of();
        }
    }
    
    @Override
    public List<GroupChat> findArchivedChatsByUserId(String userId) {
        try {
            return jpaRepository.findArchivedChatsByUserId(userId)
                    .stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche des GroupChats archivés pour: {}", userId, e);
            return List.of();
        }
    }
    
    @Override
    public List<GroupChat> findChatsWithUnreadMessages(String userId) {
        try {
            // Pour l'instant, retourne tous les chats actifs
            // TODO: Implémenter la logique de comptage des messages non lus
            return jpaRepository.findActiveChatsByUserId(userId)
                    .stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche des GroupChats avec messages non lus pour: {}", userId, e);
            return List.of();
        }
    }
    
    @Override
    public void deleteById(String id) {
        try {
            jpaRepository.deleteById(id);
            logger.info("GroupChat supprimé: {}", id);
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression du GroupChat: {}", id, e);
            throw new RuntimeException("Impossible de supprimer le GroupChat", e);
        }
    }
    
    @Override
    public void removeParticipant(String chatId, String participantId) {
        try {
            Optional<GroupChatJpaEntity> chatOpt = jpaRepository.findById(chatId);
            if (chatOpt.isPresent()) {
                GroupChatJpaEntity entity = chatOpt.get();
                entity.getParticipantIds().remove(participantId);
                entity.getAdminIds().remove(participantId);
                jpaRepository.save(entity);
                logger.info("Participant {} retiré du GroupChat: {}", participantId, chatId);
            }
        } catch (Exception e) {
            logger.error("Erreur lors du retrait du participant {} du GroupChat: {}", participantId, chatId, e);
            throw new RuntimeException("Impossible de retirer le participant", e);
        }
    }
    
    // ==================== MAPPERS ====================
    
    private GroupChatJpaEntity toEntity(GroupChat domain) {
        GroupChatJpaEntity entity = new GroupChatJpaEntity(
                domain.getId(),
                domain.getName(),
                domain.getDescription(),
                domain.getAvatarUrl(),
                domain.getCreatorId(),
                domain.getChatType().type(),
                domain.getCreatedAt()
        );
        
        entity.setParticipantIds(domain.getParticipantIds());
        entity.setAdminIds(domain.getAdminIds());
        entity.setLastMessageTime(domain.getLastMessageTime());
        entity.setMuted(domain.isMuted());
        entity.setMutedUntil(domain.getMutedUntil());
        entity.setArchived(domain.isArchived());
        entity.setPinnedMessageId(domain.getPinnedMessageId());
        entity.setMaxParticipants(domain.getMaxParticipants());
        
        return entity;
    }
    
    private GroupChat toDomain(GroupChatJpaEntity entity) {
        return GroupChatFactory.reconstitute(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getAvatarUrl(),
                entity.getCreatorId(),
                entity.getChatType(),
                entity.getParticipantIds(),
                entity.getAdminIds(),
                entity.getLastMessageTime(),
                entity.getCreatedAt(),
                entity.isMuted(),
                entity.getMutedUntil(),
                entity.isArchived(),
                entity.getPinnedMessageId(),
                entity.getMaxParticipants()
        );
    }
}