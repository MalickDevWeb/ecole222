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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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
        // APP-QA-04 : KPI inexistant → 404 (avant : créé avec les valeurs reçues, même absurdes)
        if (id == null || !kpiRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Indicateur introuvable.");
        }
        verifierKpi(kpi);
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
        logger.info("Création d'un KPI: {}", kpi == null ? null : kpi.name());
        verifierKpi(kpi);
        DashboardKpi dashboardKpi = DashboardKpiFactory.createDefault(
            kpi.name().strip(),
            kpi.value().strip(),
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
        logger.info("Création alerte: {}", alert == null ? null : alert.getTitle());
        DashboardAlertJpaEntity nouvelle = new DashboardAlertJpaEntity();
        nouvelle.setId(UUID.randomUUID().toString());
        copierAlerte(alert, nouvelle);
        nouvelle.setIsActive(alert.getIsActive() == null || alert.getIsActive());
        nouvelle.setExpiresAt(alert.getExpiresAt());
        return alertRepository.save(nouvelle);
    }
    
    @Override
    public DashboardAlertJpaEntity updateAlert(String id, DashboardAlertJpaEntity alert) {
        // APP-QA-04 : alerte inexistante → 404 (avant : créée, champs nuls compris) ; seuls les champs éditables sont copiés
        DashboardAlertJpaEntity existante = alertRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alerte introuvable."));
        copierAlerte(alert, existante);
        if (alert.getIsActive() != null) {
            existante.setIsActive(alert.getIsActive());
        }
        if (alert.getExpiresAt() != null) {
            existante.setExpiresAt(alert.getExpiresAt());
        }
        return alertRepository.save(existante);
    }
    
    @Override
    public void deactivateAlert(String id) {
        // APP-QA-04 : alerte inexistante → 404 (avant : « succès »)
        DashboardAlertJpaEntity alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alerte introuvable."));
        alert.setIsActive(false);
        alertRepository.save(alert);
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
        // APP-QA-04 : statistique inexistante → 404 (avant : créée) ; valeurs bornées
        RegionalStatsJpaEntity existante = regionalStatsRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Statistique régionale introuvable."));
        if (stats == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune donnée reçue.");
        }
        existante.setRegionName(texteBorne(stats.getRegionName(), "Le nom de la région", 100, true));
        if (stats.getPercentage() != null && (stats.getPercentage() < 0 || stats.getPercentage() > 100 || stats.getPercentage().isNaN())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le pourcentage doit être entre 0 et 100.");
        }
        if (stats.getStudentCount() != null && stats.getStudentCount() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nombre d'élèves ne peut pas être négatif.");
        }
        existante.setPercentage(stats.getPercentage());
        existante.setStudentCount(stats.getStudentCount());
        existante.setColorCode(texteBorne(stats.getColorCode(), "La couleur", 20, false));
        existante.setAcademicYear(texteBorne(stats.getAcademicYear(), "L'année scolaire", 20, false));
        existante.setLastUpdated(LocalDateTime.now());
        return regionalStatsRepository.save(existante);
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
        // APP-QA-04 : indicateur inconnu → 404, valeur vide ou démesurée → 400 (avant : « succès » sans rien faire)
        String v = texteBorne(value, "La valeur", 50, true);
        DashboardKpiJpaEntity kpi = kpiRepository.findByKpiKey(kpiKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Indicateur introuvable."));
        kpi.setValue(v);
        kpi.setLastUpdated(LocalDateTime.now());
        kpiRepository.save(kpi);
        logger.info("KPI mis à jour: {} = {}", kpiKey, v);
    }
    
    @Override
    public void createSystemAlert(String alertType, String title, String message, String severity) {
        // APP-QA-04 : titre et message obligatoires, gravité dans la liste connue (avant : alerte vide créée)
        alertType = texteBorne(alertType, "Le type d'alerte", 50, false);
        title = texteBorne(title, "Le titre", 200, true);
        message = texteBorne(message, "Le message", 2_000, true);
        severity = gravite(severity);
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

    // ==================== CONTRÔLES DES CHAMPS (APP-QA-04, 2026-11-13) ====================

    private static final java.util.List<String> GRAVITES = java.util.List.of("info", "warning", "urgent", "critical");

    private static String gravite(String g) {
        if (g == null || g.isBlank()) {
            return "info";
        }
        if (!GRAVITES.contains(g.strip())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La gravité doit être l'une des valeurs : " + String.join(", ", GRAVITES) + ".");
        }
        return g.strip();
    }

    private static String texteBorne(String v, String libelle, int max, boolean obligatoire) {
        if (v == null || v.isBlank()) {
            if (obligatoire) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, libelle + " est obligatoire.");
            }
            return null;
        }
        if (v.strip().length() > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, libelle + " ne doit pas dépasser " + max + " caractères.");
        }
        return v.strip();
    }

    private static void verifierKpi(DashboardKpiDto kpi) {
        if (kpi == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune donnée reçue.");
        }
        texteBorne(kpi.name(), "Le nom de l'indicateur", 100, true);
        texteBorne(kpi.value(), "La valeur", 50, true);
        texteBorne(kpi.unit(), "L'unité", 20, false);
        texteBorne(kpi.trend(), "La tendance", 20, false);
    }

    private static void copierAlerte(DashboardAlertJpaEntity depuis, DashboardAlertJpaEntity vers) {
        if (depuis == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune donnée reçue.");
        }
        vers.setAlertType(texteBorne(depuis.getAlertType(), "Le type d'alerte", 50, false));
        vers.setTitle(texteBorne(depuis.getTitle(), "Le titre", 200, true));
        vers.setMessage(texteBorne(depuis.getMessage(), "Le message", 2_000, true));
        vers.setSeverity(gravite(depuis.getSeverity()));
        vers.setAdditionalData(texteBorne(depuis.getAdditionalData(), "Les données complémentaires", 5_000, false));
    }
}
