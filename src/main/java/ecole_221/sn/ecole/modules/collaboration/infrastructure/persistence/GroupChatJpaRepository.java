package ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupChatJpaRepository extends JpaRepository<GroupChatJpaEntity, String> {
    
    // Requêtes optimisées avec index
    List<GroupChatJpaEntity> findByCreatorId(String creatorId);
    
    List<GroupChatJpaEntity> findByChatType(String chatType);
    
    @Query("SELECT gc FROM GroupChatJpaEntity gc WHERE :userId MEMBER OF gc.participantIds AND gc.archived = false ORDER BY gc.lastMessageTime DESC")
    List<GroupChatJpaEntity> findActiveChatsByUserId(@Param("userId") String userId);
    
    @Query("SELECT gc FROM GroupChatJpaEntity gc WHERE :userId MEMBER OF gc.participantIds AND gc.archived = true ORDER BY gc.lastMessageTime DESC")
    List<GroupChatJpaEntity> findArchivedChatsByUserId(@Param("userId") String userId);
    
    @Query("SELECT gc FROM GroupChatJpaEntity gc WHERE :userId MEMBER OF gc.participantIds AND gc.lastMessageTime > :since ORDER BY gc.lastMessageTime DESC")
    List<GroupChatJpaEntity> findRecentChatsByUserId(@Param("userId") String userId, @Param("since") LocalDateTime since);
    
    @Query("SELECT gc FROM GroupChatJpaEntity gc WHERE :userId MEMBER OF gc.participantIds")
    List<GroupChatJpaEntity> findAllChatsByUserId(@Param("userId") String userId);
    
    @Query("SELECT COUNT(gc) FROM GroupChatJpaEntity gc WHERE :userId MEMBER OF gc.participantIds")
    long countChatsByUserId(@Param("userId") String userId);
    
    // Requêtes pour les administrateurs
    @Query("SELECT gc FROM GroupChatJpaEntity gc WHERE :adminId MEMBER OF gc.adminIds")
    List<GroupChatJpaEntity> findChatsWhereUserIsAdmin(@Param("adminId") String adminId);
    
    // Requêtes pour la recherche
    @Query("SELECT gc FROM GroupChatJpaEntity gc WHERE gc.name LIKE %:keyword% AND :userId MEMBER OF gc.participantIds")
    List<GroupChatJpaEntity> searchChatsByName(@Param("userId") String userId, @Param("keyword") String keyword);
}