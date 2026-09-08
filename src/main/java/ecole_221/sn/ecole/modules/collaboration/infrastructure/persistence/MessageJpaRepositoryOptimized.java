package ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageJpaRepositoryOptimized extends JpaRepository<MessageJpaEntityOptimized, String> {
    
    // Requêtes optimisées avec index composites
    List<MessageJpaEntityOptimized> findByGroupId(String groupId);
    
    List<MessageJpaEntityOptimized> findByGroupIdOrderByTimestampDesc(String groupId);
    
    Page<MessageJpaEntityOptimized> findByGroupIdOrderByTimestampDesc(String groupId, Pageable pageable);
    
    @Query("SELECT m FROM MessageJpaEntityOptimized m WHERE m.groupId = :groupId AND m.timestamp > :timestamp ORDER BY m.timestamp ASC")
    List<MessageJpaEntityOptimized> findMessagesAfter(@Param("groupId") String groupId, @Param("timestamp") LocalDateTime timestamp);
    
    @Query("SELECT m FROM MessageJpaEntityOptimized m WHERE m.groupId = :groupId AND m.timestamp < :timestamp ORDER BY m.timestamp DESC")
    List<MessageJpaEntityOptimized> findMessagesBefore(@Param("groupId") String groupId, @Param("timestamp") LocalDateTime timestamp);
    
    List<MessageJpaEntityOptimized> findBySenderId(String senderId);
    
    List<MessageJpaEntityOptimized> findBySenderIdOrderByTimestampDesc(String senderId);
    
    @Query("SELECT m FROM MessageJpaEntityOptimized m WHERE m.senderId = :senderId AND m.groupId = :groupId")
    List<MessageJpaEntityOptimized> findBySenderIdAndGroupId(@Param("senderId") String senderId, @Param("groupId") String groupId);
    
    @Query("SELECT m FROM MessageJpaEntityOptimized m ORDER BY m.timestamp DESC")
    List<MessageJpaEntityOptimized> findByMessageType(@Param("type") String type);
    
    @Query("SELECT m FROM MessageJpaEntityOptimized m WHERE m.groupId = :groupId ORDER BY m.timestamp DESC")
    List<MessageJpaEntityOptimized> findByGroupIdAndMessageType(@Param("groupId") String groupId, @Param("type") String type);
    
    @Query("SELECT m FROM MessageJpaEntityOptimized m WHERE m.groupId = :groupId ORDER BY m.timestamp DESC")
    List<MessageJpaEntityOptimized> findByGroupIdAndStatus(@Param("groupId") String groupId, @Param("status") String status);
    
    // Recherche de messages
    @Query("SELECT m FROM MessageJpaEntityOptimized m WHERE m.groupId = :groupId AND m.content LIKE %:keyword%")
    List<MessageJpaEntityOptimized> searchMessagesInGroup(@Param("groupId") String groupId, @Param("keyword") String keyword);
    
    @Query("SELECT m FROM MessageJpaEntityOptimized m WHERE m.senderId = :senderId AND m.content LIKE %:keyword%")
    List<MessageJpaEntityOptimized> searchMessagesBySender(@Param("senderId") String senderId, @Param("keyword") String keyword);
    
    // Statistiques
    @Query("SELECT COUNT(m) FROM MessageJpaEntityOptimized m WHERE m.groupId = :groupId")
    long countMessagesByGroup(@Param("groupId") String groupId);
    
    @Query("SELECT COUNT(m) FROM MessageJpaEntityOptimized m WHERE m.senderId = :senderId")
    long countMessagesBySender(@Param("senderId") String senderId);
    
    @Query("SELECT COUNT(m) FROM MessageJpaEntityOptimized m WHERE m.groupId = :groupId")
    long countUnreadMessagesByGroup(@Param("groupId") String groupId);
    
    // Messages avec média
    @Query("SELECT m FROM MessageJpaEntityOptimized m WHERE m.groupId = :groupId AND m.mediaUrl IS NOT NULL")
    List<MessageJpaEntityOptimized> findMediaMessagesByGroup(@Param("groupId") String groupId);
    
    // Messages non supprimés
    @Query("SELECT m FROM MessageJpaEntityOptimized m WHERE m.groupId = :groupId AND m.deletedForEveryone = false ORDER BY m.timestamp DESC")
    List<MessageJpaEntityOptimized> findActiveMessagesByGroup(@Param("groupId") String groupId);
    
    // Suppression
    @Query("UPDATE MessageJpaEntityOptimized m SET m.deletedForEveryone = true WHERE m.id = :messageId")
    void markAsDeletedForEveryone(@Param("messageId") String messageId);
}