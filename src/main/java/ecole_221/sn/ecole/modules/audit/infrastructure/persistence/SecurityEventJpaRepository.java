package ecole_221.sn.ecole.modules.audit.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface SecurityEventJpaRepository extends JpaRepository<SecurityEventJpaEntity, String> {
    List<SecurityEventJpaEntity> findByEventType(String eventType);
    List<SecurityEventJpaEntity> findByEventCategory(String eventCategory);
    List<SecurityEventJpaEntity> findBySeverity(String severity);
    List<SecurityEventJpaEntity> findByCheckpointId(String checkpointId);
    List<SecurityEventJpaEntity> findByPersonId(String personId);
    List<SecurityEventJpaEntity> findByAccessStatus(String accessStatus);
    List<SecurityEventJpaEntity> findByRequiresInvestigationTrue();
    
    /**
     * Récupère les événements de sécurité entre deux dates.
     */
    List<SecurityEventJpaEntity> findByEventTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT s FROM SecurityEventJpaEntity s WHERE s.eventTimestamp >= :startTime ORDER BY s.eventTimestamp DESC")
    List<SecurityEventJpaEntity> findRecentSecurityEvents(@Param("startTime") LocalDateTime startTime);
    
    @Query("SELECT s FROM SecurityEventJpaEntity s WHERE s.eventTimestamp >= :startTime ORDER BY s.eventTimestamp DESC LIMIT 20")
    List<SecurityEventJpaEntity> findTop20RecentSecurityEvents(@Param("startTime") LocalDateTime startTime);
    
    @Query("SELECT s FROM SecurityEventJpaEntity s WHERE s.accessStatus = 'denied' AND s.eventTimestamp >= :startTime ORDER BY s.eventTimestamp DESC")
    List<SecurityEventJpaEntity> findRecentAccessDenials(@Param("startTime") LocalDateTime startTime);
    
    @Query("SELECT s FROM SecurityEventJpaEntity s WHERE s.requiresInvestigation = true AND s.investigationStatus = 'pending'")
    List<SecurityEventJpaEntity> findPendingInvestigations();
    
    @Query("SELECT COUNT(s) FROM SecurityEventJpaEntity s WHERE s.severity = 'critical' AND s.eventTimestamp >= :startTime")
    Long countCriticalEventsSince(@Param("startTime") LocalDateTime startTime);
    
    @Query("SELECT s FROM SecurityEventJpaEntity s WHERE s.personId = :personId ORDER BY s.eventTimestamp DESC")
    List<SecurityEventJpaEntity> findPersonSecurityHistory(@Param("personId") String personId);
}