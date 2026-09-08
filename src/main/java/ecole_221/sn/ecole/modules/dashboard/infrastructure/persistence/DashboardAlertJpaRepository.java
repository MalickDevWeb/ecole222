package ecole_221.sn.ecole.modules.dashboard.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface DashboardAlertJpaRepository extends JpaRepository<DashboardAlertJpaEntity, String> {
    List<DashboardAlertJpaEntity> findByAlertType(String alertType);
    List<DashboardAlertJpaEntity> findBySeverity(String severity);
    List<DashboardAlertJpaEntity> findByIsActiveTrue();
    
    @Query("SELECT a FROM DashboardAlertJpaEntity a WHERE a.isActive = true AND (a.expiresAt IS NULL OR a.expiresAt > :now)")
    List<DashboardAlertJpaEntity> findActiveAlerts(@Param("now") LocalDateTime now);
    
    @Query("SELECT a FROM DashboardAlertJpaEntity a WHERE a.severity = 'urgent' OR a.severity = 'critical'")
    List<DashboardAlertJpaEntity> findUrgentAlerts();
}