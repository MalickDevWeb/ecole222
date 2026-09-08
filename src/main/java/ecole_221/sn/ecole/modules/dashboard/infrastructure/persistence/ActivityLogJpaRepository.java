package ecole_221.sn.ecole.modules.dashboard.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface ActivityLogJpaRepository extends JpaRepository<ActivityLogJpaEntity, String> {
    List<ActivityLogJpaEntity> findByActivityType(String activityType);
    List<ActivityLogJpaEntity> findByUserId(String userId);
    List<ActivityLogJpaEntity> findByEntityType(String entityType);
    
    @Query("SELECT a FROM ActivityLogJpaEntity a WHERE a.activityTime >= :startTime ORDER BY a.activityTime DESC")
    List<ActivityLogJpaEntity> findRecentActivities(@Param("startTime") LocalDateTime startTime);
    
    @Query("SELECT a FROM ActivityLogJpaEntity a WHERE a.activityTime >= :startTime ORDER BY a.activityTime DESC LIMIT 10")
    List<ActivityLogJpaEntity> findTop10RecentActivities(@Param("startTime") LocalDateTime startTime);
    
    @Query("SELECT COUNT(a) FROM ActivityLogJpaEntity a WHERE a.activityType = :type AND a.activityTime >= :startTime")
    Long countByActivityTypeSince(@Param("type") String type, @Param("startTime") LocalDateTime startTime);
}