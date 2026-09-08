package ecole_221.sn.ecole.modules.dashboard.application.port.usecase;

import ecole_221.sn.ecole.modules.dashboard.application.port.dto.DashboardKpiDto;
import ecole_221.sn.ecole.modules.dashboard.infrastructure.persistence.*;

import java.util.List;
import java.util.Map;

/**
 * Interface des cas d'utilisation pour le module Dashboard.
 * Gère les KPIs, alertes, activités et statistiques régionales.
 */
public interface DashboardUseCase {
    
    // ==================== GESTION DES KPIS ====================
    
    /**
     * Récupère tous les KPIs.
     */
    List<DashboardKpiDto> getAllKpis();
    
    /**
     * Récupère un KPI par sa clé.
     */
    DashboardKpiDto getKpiByKey(String kpiKey);
    
    /**
     * Récupère les KPIs par catégorie.
     */
    List<DashboardKpiDto> getKpisByCategory(String category);
    
    /**
     * Met à jour un KPI.
     */
    DashboardKpiDto updateKpi(String id, DashboardKpiDto kpi);
    
    /**
     * Crée un nouveau KPI.
     */
    DashboardKpiDto createKpi(DashboardKpiDto kpi);
    
    // ==================== GESTION DES ALERTES (compatibilité JPA) ====================
    
    /**
     * Récupère toutes les alertes.
     */
    List<DashboardAlertJpaEntity> getAllAlerts();
    
    /**
     * Récupère les alertes actives.
     */
    List<DashboardAlertJpaEntity> getActiveAlerts();
    
    /**
     * Récupère les alertes urgentes.
     */
    List<DashboardAlertJpaEntity> getUrgentAlerts();
    
    /**
     * Crée une alerte.
     */
    DashboardAlertJpaEntity createAlert(DashboardAlertJpaEntity alert);
    
    /**
     * Met à jour une alerte.
     */
    DashboardAlertJpaEntity updateAlert(String id, DashboardAlertJpaEntity alert);
    
    /**
     * Désactive une alerte.
     */
    void deactivateAlert(String id);
    
    // ==================== GESTION DES ACTIVITÉS (compatibilité JPA) ====================
    
    /**
     * Récupère les activités récentes.
     */
    List<ActivityLogJpaEntity> getRecentActivities(int limit);
    
    /**
     * Récupère les activités par type.
     */
    List<ActivityLogJpaEntity> getActivitiesByType(String activityType);
    
    /**
     * Enregistre une activité.
     */
    ActivityLogJpaEntity logActivity(ActivityLogJpaEntity activity);
    
    // ==================== STATISTIQUES RÉGIONALES (compatibilité JPA) ====================
    
    /**
     * Récupère les statistiques régionales.
     */
    List<RegionalStatsJpaEntity> getRegionalStats(String academicYear);
    
    /**
     * Met à jour les statistiques régionales.
     */
    RegionalStatsJpaEntity updateRegionalStats(String id, RegionalStatsJpaEntity stats);
    
    // ==================== AGRÉGATION DASHBOARD ====================
    
    /**
     * Récupère le dashboard complet.
     */
    Map<String, Object> getCompleteDashboard();
    
    /**
     * Récupère les KPIs financiers.
     */
    Map<String, Object> getFinancialKpis();
    
    /**
     * Récupère les KPIs académiques.
     */
    Map<String, Object> getAcademicKpis();
    
    /**
     * Récupère les KPIs de sécurité.
     */
    Map<String, Object> getSecurityKpis();
    
    // ==================== MISES À JOUR EN TEMPS RÉEL ====================
    
    /**
     * Met à jour la valeur d'un KPI.
     */
    void updateKpiValue(String kpiKey, String value);
    
    /**
     * Crée une alerte système.
     */
    void createSystemAlert(String alertType, String title, String message, String severity);
}