package ecole_221.sn.ecole.modules.audit.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogJpaEntity, String> {
    List<AuditLogJpaEntity> findByActionType(String actionType);
    List<AuditLogJpaEntity> findByEntityType(String entityType);
    List<AuditLogJpaEntity> findByActorId(String actorId);
    List<AuditLogJpaEntity> findByModule(String module);
    List<AuditLogJpaEntity> findBySeverity(String severity);
    
    /**
     * Récupère les logs d'audit entre deux dates.
     */
    List<AuditLogJpaEntity> findByActionTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT a FROM AuditLogJpaEntity a WHERE a.actionTimestamp >= :startTime ORDER BY a.actionTimestamp DESC")
    List<AuditLogJpaEntity> findRecentAuditLogs(@Param("startTime") LocalDateTime startTime);
    
    @Query("SELECT a FROM AuditLogJpaEntity a WHERE a.actionTimestamp >= :startTime ORDER BY a.actionTimestamp DESC LIMIT 50")
    List<AuditLogJpaEntity> findTop50RecentAuditLogs(@Param("startTime") LocalDateTime startTime);
    
    @Query("SELECT a FROM AuditLogJpaEntity a WHERE a.actorId = :actorId AND a.actionTimestamp >= :startTime ORDER BY a.actionTimestamp DESC")
    List<AuditLogJpaEntity> findByActorIdSince(@Param("actorId") String actorId, @Param("startTime") LocalDateTime startTime);
    
    @Query("SELECT COUNT(a) FROM AuditLogJpaEntity a WHERE a.status = 'failure' AND a.actionTimestamp >= :startTime")
    Long countFailuresSince(@Param("startTime") LocalDateTime startTime);
    
    @Query("SELECT a FROM AuditLogJpaEntity a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.actionTimestamp DESC")
    List<AuditLogJpaEntity> findEntityHistory(@Param("entityType") String entityType, @Param("entityId") String entityId);
}