package ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence;

import ecole_221.sn.ecole.modules.collaboration.domain.model.Message;
import ecole_221.sn.ecole.modules.collaboration.domain.model.MessageFactory;
import ecole_221.sn.ecole.modules.collaboration.domain.port.repository.MessageRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class MessageRepositoryAdapterOptimized implements MessageRepositoryPort {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageRepositoryAdapterOptimized.class);
    private final MessageJpaRepositoryOptimized jpaRepository;
    
    public MessageRepositoryAdapterOptimized(MessageJpaRepositoryOptimized jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public Message save(Message message) {
        try {
            MessageJpaEntityOptimized entity = toEntity(message);
            MessageJpaEntityOptimized saved = jpaRepository.save(entity);
            logger.debug("Message sauvegardé: {}", saved.getId());
            return toDomain(saved);
        } catch (Exception e) {
            logger.error("Erreur lors de la sauvegarde du message: {}", message.getId(), e);
            throw new RuntimeException("Impossible de sauvegarder le message", e);
        }
    }
    
    @Override
    public Optional<Message> findById(String id) {
        try {
            return jpaRepository.findById(id)
                    .map(this::toDomain);
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche du message: {}", id, e);
            return Optional.empty();
        }
    }
    
    @Override
    public List<Message> findByGroupId(String groupId) {
        try {
            long startTime = System.currentTimeMillis();
            List<Message> messages = jpaRepository.findByGroupIdOrderByTimestampDesc(groupId)
                    .stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList());
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Récupération de {} messages pour le groupe {} en {}ms", messages.size(), groupId, duration);
            return messages;
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche des messages pour le groupe: {}", groupId, e);
            return List.of();
        }
    }
    
    @Override
    public List<Message> findByGroupIdAndTimestampAfter(String groupId, LocalDateTime timestamp) {
        try {
            return jpaRepository.findMessagesAfter(groupId, timestamp)
                    .stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche des messages après {} pour le groupe: {}", timestamp, groupId, e);
            return List.of();
        }
    }
    
    @Override
    public List<Message> findBySenderId(String senderId) {
        try {
            return jpaRepository.findBySenderIdOrderByTimestampDesc(senderId)
                    .stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche des messages de l'expéditeur: {}", senderId, e);
            return List.of();
        }
    }
    
    @Override
    public List<Message> findUnreadMessages(String groupId, String userId) {
        try {
            // Pour l'instant, retourne les messages non lus du groupe
            // TODO: Implémenter la logique de lecture par utilisateur
            return jpaRepository.findByGroupIdAndStatus(groupId, "SENT")
                    .stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche des messages non lus pour le groupe: {}", groupId, e);
            return List.of();
        }
    }
    
    @Override
    public void deleteById(String id) {
        try {
            jpaRepository.deleteById(id);
            logger.debug("Message supprimé: {}", id);
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression du message: {}", id, e);
            throw new RuntimeException("Impossible de supprimer le message", e);
        }
    }
    
    @Override
    public void deleteByGroupId(String groupId) {
        try {
            List<MessageJpaEntityOptimized> messages = jpaRepository.findByGroupId(groupId);
            jpaRepository.deleteAll(messages);
            logger.info("{} messages supprimés pour le groupe: {}", messages.size(), groupId);
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression des messages du groupe: {}", groupId, e);
            throw new RuntimeException("Impossible de supprimer les messages du groupe", e);
        }
    }
    
    // ==================== MÉTHODES OPTIMISÉES ====================
    
    public List<Message> findByGroupIdPaginated(String groupId, int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<MessageJpaEntityOptimized> messagePage = jpaRepository.findByGroupIdOrderByTimestampDesc(groupId, pageable);
            return messagePage.getContent()
                    .stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche paginée des messages pour le groupe: {}", groupId, e);
            return List.of();
        }
    }
    
    public List<Message> findRecentMessages(String groupId, int limit) {
        try {
            Pageable pageable = PageRequest.of(0, limit);
            Page<MessageJpaEntityOptimized> messagePage = jpaRepository.findByGroupIdOrderByTimestampDesc(groupId, pageable);
            return messagePage.getContent()
                    .stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche des messages récents pour le groupe: {}", groupId, e);
            return List.of();
        }
    }
    
    public long countMessagesByGroup(String groupId) {
        try {
            return jpaRepository.countMessagesByGroup(groupId);
        } catch (Exception e) {
            logger.error("Erreur lors du comptage des messages pour le groupe: {}", groupId, e);
            return 0;
        }
    }
    
    // ==================== MAPPERS ====================
    
    private MessageJpaEntityOptimized toEntity(Message domain) {
        MessageJpaEntityOptimized entity = new MessageJpaEntityOptimized(
                domain.getId(),
                domain.getGroupId(),
                domain.getSenderId(),
                domain.getContent(),
                domain.getMessageType().type(),
                domain.getStatus().status(),
                domain.getTimestamp()
        );
        
        entity.setMediaUrl(domain.getMediaUrl());
        entity.setDuration(domain.getDuration());
        
        return entity;
    }
    
    private Message toDomain(MessageJpaEntityOptimized entity) {
        return MessageFactory.reconstitute(
                entity.getId(),
                entity.getGroupId(),
                entity.getSenderId(),
                entity.getContent(),
                entity.getMessageType(),
                entity.getStatus(),
                entity.getTimestamp(),
                entity.getMediaUrl(),
                entity.getDuration()
        );
    }
}