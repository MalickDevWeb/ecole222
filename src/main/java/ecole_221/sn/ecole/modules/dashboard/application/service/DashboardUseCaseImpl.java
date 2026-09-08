package ecole_221.sn.ecole.modules.dashboard.application.service;

import ecole_221.sn.ecole.modules.dashboard.application.port.dto.DashboardKpiDto;
import ecole_221.sn.ecole.modules.dashboard.application.port.usecase.DashboardUseCase;
import ecole_221.sn.ecole.modules.dashboard.domain.model.DashboardKpi;
import ecole_221.sn.ecole.modules.dashboard.domain.model.DashboardKpiFactory;
import ecole_221.sn.ecole.modules.dashboard.domain.port.repository.DashboardRepositoryPort;
import ecole_221.sn.ecole.modules.dashboard.infrastructure.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DashboardUseCaseImpl implements DashboardUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardUseCaseImpl.class);
    
    private final DashboardRepositoryPort dashboardRepositoryPort;
    private final DashboardKpiJpaRepository kpiRepository;
    private final DashboardAlertJpaRepository alertRepository;
    private final ActivityLogJpaRepository activityLogRepository;
    private final RegionalStatsJpaRepository regionalStatsRepository;
    
    public DashboardUseCaseImpl(DashboardRepositoryPort dashboardRepositoryPort,
                                DashboardKpiJpaRepository kpiRepository,
                                DashboardAlertJpaRepository alertRepository,
                                ActivityLogJpaRepository activityLogRepository,
                                RegionalStatsJpaRepository regionalStatsRepository) {
        this.dashboardRepositoryPort = dashboardRepositoryPort;
        this.kpiRepository = kpiRepository;
        this.alertRepository = alertRepository;
        this.activityLogRepository = activityLogRepository;
        this.regionalStatsRepository = regionalStatsRepository;
    }
    
    // ==================== GESTION DES KPIS ====================
    
    @Override
    public List<DashboardKpiDto> getAllKpis() {
        logger.info("Récupération de tous les KPIs");
        return dashboardRepositoryPort.findAllKpis().stream()
            .map(this::toDashboardKpiDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public DashboardKpiDto getKpiByKey(String kpiKey) {
        logger.info("Récupération du KPI par clé: {}", kpiKey);
        // Utilisation du repository JPA pour la recherche par clé
        DashboardKpiJpaEntity entity = kpiRepository.findByKpiKey(kpiKey).orElse(null);
        if (entity != null) {
            return toDashboardKpiDtoFromEntity(entity);
        }
        return null;
    }
    
    @Override
    public List<DashboardKpiDto> getKpisByCategory(String category) {
        logger.info("Récupération des KPIs par catégorie: {}", category);
        // Utilisation du repository JPA pour la recherche par catégorie
        return kpiRepository.findByCategory(category).stream()
            .map(this::toDashboardKpiDtoFromEntity)
            .collect(Collectors.toList());
    }
    
    @Override
    public DashboardKpiDto updateKpi(String id, DashboardKpiDto kpi) {
        logger.info("Mise à jour du KPI: {}", id);
        DashboardKpi dashboardKpi = DashboardKpiFactory.reconstitute(
            id,
            kpi.name(),
            kpi.value(),
            kpi.unit(),
            kpi.trend()
        );
        DashboardKpi updatedKpi = dashboardRepositoryPort.saveKpi(dashboardKpi);
        return toDashboardKpiDto(updatedKpi);
    }
    
    @Override
    public DashboardKpiDto createKpi(DashboardKpiDto kpi) {
        logger.info("Création d'un KPI: {}", kpi.name());
        DashboardKpi dashboardKpi = DashboardKpiFactory.createDefault(
            kpi.name(),
            kpi.value(),
            kpi.unit()
        );
        DashboardKpi savedKpi = dashboardRepositoryPort.saveKpi(dashboardKpi);
        return toDashboardKpiDto(savedKpi);
    }
    
    // ==================== GESTION DES ALERTES (compatibilité JPA) ====================
    
    @Override
    public List<DashboardAlertJpaEntity> getAllAlerts() {
        return alertRepository.findAll();
    }
    
    @Override
    public List<DashboardAlertJpaEntity> getActiveAlerts() {
        return alertRepository.findActiveAlerts(LocalDateTime.now());
    }
    
    @Override
    public List<DashboardAlertJpaEntity> getUrgentAlerts() {
        return alertRepository.findUrgentAlerts();
    }
    
    @Override
    public DashboardAlertJpaEntity createAlert(DashboardAlertJpaEntity alert) {
        logger.info("Création alerte: {}", alert.getTitle());
        return alertRepository.save(alert);
    }
    
    @Override
    public DashboardAlertJpaEntity updateAlert(String id, DashboardAlertJpaEntity alert) {
        alert.setId(id);
        return alertRepository.save(alert);
    }
    
    @Override
    public void deactivateAlert(String id) {
        DashboardAlertJpaEntity alert = alertRepository.findById(id).orElse(null);
        if (alert != null) {
            alert.setIsActive(false);
            alertRepository.save(alert);
        }
    }
    
    // ==================== GESTION DES ACTIVITÉS (compatibilité JPA) ====================
    
    @Override
    public List<ActivityLogJpaEntity> getRecentActivities(int limit) {
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        if (limit <= 10) {
            return activityLogRepository.findTop10RecentActivities(startTime);
        }
        return activityLogRepository.findRecentActivities(startTime).stream()
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<ActivityLogJpaEntity> getActivitiesByType(String activityType) {
        return activityLogRepository.findByActivityType(activityType);
    }
    
    @Override
    public ActivityLogJpaEntity logActivity(ActivityLogJpaEntity activity) {
        logger.info("Log activité: {}", activity.getDescription());
        return activityLogRepository.save(activity);
    }
    
    // ==================== STATISTIQUES RÉGIONALES (compatibilité JPA) ====================
    
    @Override
    public List<RegionalStatsJpaEntity> getRegionalStats(String academicYear) {
        return regionalStatsRepository.findByAcademicYearOrderByStudentCountDesc(academicYear);
    }
    
    @Override
    public RegionalStatsJpaEntity updateRegionalStats(String id, RegionalStatsJpaEntity stats) {
        stats.setId(id);
        return regionalStatsRepository.save(stats);
    }
    
    // ==================== AGRÉGATION DASHBOARD ====================
    
    @Override
    public Map<String, Object> getCompleteDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // KPIs par catégorie
        dashboard.put("financialKpis", getFinancialKpis());
        dashboard.put("academicKpis", getAcademicKpis());
        dashboard.put("securityKpis", getSecurityKpis());
        
        // Alertes actives
        dashboard.put("activeAlerts", getActiveAlerts());
        dashboard.put("urgentAlerts", getUrgentAlerts());
        
        // Activités récentes
        dashboard.put("recentActivities", getRecentActivities(10));
        
        // Statistiques régionales
        dashboard.put("regionalStats", getRegionalStats("2025-2026"));
        
        return dashboard;
    }
    
    @Override
    public Map<String, Object> getFinancialKpis() {
        Map<String, Object> kpis = new HashMap<>();
        List<DashboardKpiJpaEntity> financialKpis = kpiRepository.findByCategory("finance");
        
        for (DashboardKpiJpaEntity kpi : financialKpis) {
            kpis.put(kpi.getKpiKey(), Map.of(
                "value", kpi.getValue(),
                "trend", kpi.getTrend(),
                "trendValue", kpi.getTrendValue(),
                "currency", kpi.getCurrency(),
                "name", kpi.getKpiName()
            ));
        }
        
        return kpis;
    }
    
    @Override
    public Map<String, Object> getAcademicKpis() {
        Map<String, Object> kpis = new HashMap<>();
        List<DashboardKpiJpaEntity> academicKpis = kpiRepository.findByCategory("academic");
        
        for (DashboardKpiJpaEntity kpi : academicKpis) {
            kpis.put(kpi.getKpiKey(), Map.of(
                "value", kpi.getValue(),
                "trend", kpi.getTrend(),
                "trendValue", kpi.getTrendValue(),
                "name", kpi.getKpiName()
            ));
        }
        
        return kpis;
    }
    
    @Override
    public Map<String, Object> getSecurityKpis() {
        Map<String, Object> kpis = new HashMap<>();
        List<DashboardKpiJpaEntity> securityKpis = kpiRepository.findByCategory("security");
        
        for (DashboardKpiJpaEntity kpi : securityKpis) {
            kpis.put(kpi.getKpiKey(), Map.of(
                "value", kpi.getValue(),
                "trend", kpi.getTrend(),
                "trendValue", kpi.getTrendValue(),
                "name", kpi.getKpiName()
            ));
        }
        
        return kpis;
    }
    
    // ==================== MISES À JOUR EN TEMPS RÉEL ====================
    
    @Override
    public void updateKpiValue(String kpiKey, String value) {
        DashboardKpiJpaEntity kpi = kpiRepository.findByKpiKey(kpiKey).orElse(null);
        if (kpi != null) {
            kpi.setValue(value);
            kpi.setLastUpdated(LocalDateTime.now());
            kpiRepository.save(kpi);
            logger.info("KPI mis à jour: {} = {}", kpiKey, value);
        }
    }
    
    @Override
    public void createSystemAlert(String alertType, String title, String message, String severity) {
        DashboardAlertJpaEntity alert = new DashboardAlertJpaEntity(
            UUID.randomUUID().toString(),
            alertType,
            title,
            message,
            severity,
            true,
            null,
            LocalDateTime.now().plusHours(24) // Expire dans 24h
        );
        alertRepository.save(alert);
        logger.info("Alerte système créée: {} - {}", title, severity);
    }
    
    // ==================== MAPPERS ====================
    
    private DashboardKpiDto toDashboardKpiDto(DashboardKpi kpi) {
        return new DashboardKpiDto(
            kpi.getId(),
            kpi.getName(),
            kpi.getValue(),
            kpi.getUnit(),
            kpi.getTrend()
        );
    }

    private DashboardKpiDto toDashboardKpiDtoFromEntity(DashboardKpiJpaEntity entity) {
        return new DashboardKpiDto(
            entity.getId(),
            entity.getKpiName(),
            entity.getValue(),
            entity.getCurrency() != null ? entity.getCurrency() : entity.getValueType(),
            entity.getTrend()
        );
    }
}