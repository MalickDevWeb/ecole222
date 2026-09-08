package ecole_221.sn.ecole.modules.dashboard.infrastructure.web;

import ecole_221.sn.ecole.modules.dashboard.application.port.dto.DashboardKpiDto;
import ecole_221.sn.ecole.modules.dashboard.application.port.usecase.DashboardUseCase;
import ecole_221.sn.ecole.modules.dashboard.infrastructure.persistence.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@CrossOrigin(origins = "*")
@Tag(name = "Dashboard", description = "Dashboard CEO - KPIs, alertes, activités")
public class DashboardController {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    private final DashboardUseCase dashboardUseCase;
    
    public DashboardController(DashboardUseCase dashboardUseCase) {
        this.dashboardUseCase = dashboardUseCase;
    }
    
    // ==================== ENDPOINTS KPIS ====================
    
    @GetMapping("/kpis")
    @Operation(summary = "Liste des KPIs", description = "Retourne tous les indicateurs de performance")
    public ResponseEntity<List<DashboardKpiDto>> getAllKpis() {
        logger.info("GET /dashboard/kpis - Récupération KPIs");
        return ResponseEntity.ok(dashboardUseCase.getAllKpis());
    }
    
    @GetMapping("/kpis/{kpiKey}")
    @Operation(summary = "KPI par clé", description = "Retourne un KPI spécifique")
    public ResponseEntity<DashboardKpiDto> getKpiByKey(@PathVariable String kpiKey) {
        logger.info("GET /dashboard/kpis/{} - Récupération KPI", kpiKey);
        DashboardKpiDto kpi = dashboardUseCase.getKpiByKey(kpiKey);
        if (kpi == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(kpi);
    }
    
    @GetMapping("/kpis/category/{category}")
    @Operation(summary = "KPIs par catégorie", description = "Retourne les KPIs d'une catégorie")
    public ResponseEntity<List<DashboardKpiDto>> getKpisByCategory(@PathVariable String category) {
        logger.info("GET /dashboard/kpis/category/{} - Récupération KPIs par catégorie", category);
        return ResponseEntity.ok(dashboardUseCase.getKpisByCategory(category));
    }
    
    @PostMapping("/kpis")
    @Operation(summary = "Créer KPI", description = "Crée un nouvel indicateur de performance")
    public ResponseEntity<DashboardKpiDto> createKpi(@RequestBody DashboardKpiDto kpi) {
        logger.info("POST /dashboard/kpis - Création KPI");
        return ResponseEntity.ok(dashboardUseCase.createKpi(kpi));
    }
    
    @PutMapping("/kpis/{id}")
    @Operation(summary = "Mettre à jour KPI", description = "Met à jour un indicateur de performance")
    public ResponseEntity<DashboardKpiDto> updateKpi(@PathVariable String id, 
                                                           @RequestBody DashboardKpiDto kpi) {
        logger.info("PUT /dashboard/kpis/{} - Mise à jour KPI", id);
        return ResponseEntity.ok(dashboardUseCase.updateKpi(id, kpi));
    }
    
    @PatchMapping("/kpis/{kpiKey}/value")
    @Operation(summary = "Mettre à jour valeur KPI", description = "Met à jour la valeur d'un KPI en temps réel")
    public ResponseEntity<Map<String, Object>> updateKpiValue(@PathVariable String kpiKey, 
                                                             @RequestBody Map<String, String> body) {
        logger.info("PATCH /dashboard/kpis/{}/value - Mise à jour valeur KPI", kpiKey);
        dashboardUseCase.updateKpiValue(kpiKey, body.get("value"));
        return ResponseEntity.ok(Map.of("success", true));
    }
    
    // ==================== ENDPOINTS ALERTES (compatibilité JPA) ====================
    
    @GetMapping("/alerts")
    @Operation(summary = "Liste des alertes", description = "Retourne toutes les alertes")
    public ResponseEntity<List<DashboardAlertJpaEntity>> getAllAlerts() {
        logger.info("GET /dashboard/alerts - Récupération alertes");
        return ResponseEntity.ok(dashboardUseCase.getAllAlerts());
    }
    
    @GetMapping("/alerts/active")
    @Operation(summary = "Alertes actives", description = "Retourne les alertes actives")
    public ResponseEntity<List<DashboardAlertJpaEntity>> getActiveAlerts() {
        logger.info("GET /dashboard/alerts/active - Récupération alertes actives");
        return ResponseEntity.ok(dashboardUseCase.getActiveAlerts());
    }
    
    @GetMapping("/alerts/urgent")
    @Operation(summary = "Alertes urgentes", description = "Retourne les alertes urgentes et critiques")
    public ResponseEntity<List<DashboardAlertJpaEntity>> getUrgentAlerts() {
        logger.info("GET /dashboard/alerts/urgent - Récupération alertes urgentes");
        return ResponseEntity.ok(dashboardUseCase.getUrgentAlerts());
    }
    
    @PostMapping("/alerts")
    @Operation(summary = "Créer alerte", description = "Crée une nouvelle alerte")
    public ResponseEntity<DashboardAlertJpaEntity> createAlert(@RequestBody DashboardAlertJpaEntity alert) {
        logger.info("POST /dashboard/alerts - Création alerte");
        return ResponseEntity.ok(dashboardUseCase.createAlert(alert));
    }
    
    @PutMapping("/alerts/{id}")
    @Operation(summary = "Mettre à jour alerte", description = "Met à jour une alerte")
    public ResponseEntity<DashboardAlertJpaEntity> updateAlert(@PathVariable String id, 
                                                              @RequestBody DashboardAlertJpaEntity alert) {
        logger.info("PUT /dashboard/alerts/{} - Mise à jour alerte", id);
        return ResponseEntity.ok(dashboardUseCase.updateAlert(id, alert));
    }
    
    @PatchMapping("/alerts/{id}/deactivate")
    @Operation(summary = "Désactiver alerte", description = "Désactive une alerte")
    public ResponseEntity<Map<String, Object>> deactivateAlert(@PathVariable String id) {
        logger.info("PATCH /dashboard/alerts/{}/deactivate - Désactivation alerte", id);
        dashboardUseCase.deactivateAlert(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
    
    // ==================== ENDPOINTS ACTIVITÉS (compatibilité JPA) ====================
    
    @GetMapping("/activities")
    @Operation(summary = "Activités récentes", description = "Retourne les activités récentes")
    public ResponseEntity<List<ActivityLogJpaEntity>> getRecentActivities(@RequestParam(defaultValue = "10") int limit) {
        logger.info("GET /dashboard/activities - Récupération activités récentes");
        return ResponseEntity.ok(dashboardUseCase.getRecentActivities(limit));
    }
    
    @GetMapping("/activities/type/{activityType}")
    @Operation(summary = "Activités par type", description = "Retourne les activités par type")
    public ResponseEntity<List<ActivityLogJpaEntity>> getActivitiesByType(@PathVariable String activityType) {
        logger.info("GET /dashboard/activities/type/{} - Récupération activités par type", activityType);
        return ResponseEntity.ok(dashboardUseCase.getActivitiesByType(activityType));
    }
    
    @PostMapping("/activities")
    @Operation(summary = "Logger activité", description = "Enregistre une nouvelle activité")
    public ResponseEntity<ActivityLogJpaEntity> logActivity(@RequestBody ActivityLogJpaEntity activity) {
        logger.info("POST /dashboard/activities - Log activité");
        return ResponseEntity.ok(dashboardUseCase.logActivity(activity));
    }
    
    // ==================== ENDPOINTS STATISTIQUES RÉGIONALES (compatibilité JPA) ====================
    
    @GetMapping("/regional-stats/{academicYear}")
    @Operation(summary = "Statistiques régionales", description = "Retourne les statistiques par région")
    public ResponseEntity<List<RegionalStatsJpaEntity>> getRegionalStats(@PathVariable String academicYear) {
        logger.info("GET /dashboard/regional-stats/{} - Récupération stats régionales", academicYear);
        return ResponseEntity.ok(dashboardUseCase.getRegionalStats(academicYear));
    }
    
    @PutMapping("/regional-stats/{id}")
    @Operation(summary = "Mettre à jour stats régionales", description = "Met à jour les statistiques régionales")
    public ResponseEntity<RegionalStatsJpaEntity> updateRegionalStats(@PathVariable String id, 
                                                                     @RequestBody RegionalStatsJpaEntity stats) {
        logger.info("PUT /dashboard/regional-stats/{} - Mise à jour stats régionales", id);
        return ResponseEntity.ok(dashboardUseCase.updateRegionalStats(id, stats));
    }
    
    // ==================== ENDPOINTS AGRÉGATION DASHBOARD ====================
    
    @GetMapping("/complete")
    @Operation(summary = "Dashboard complet", description = "Retourne toutes les données du dashboard")
    public ResponseEntity<Map<String, Object>> getCompleteDashboard() {
        logger.info("GET /dashboard/complete - Récupération dashboard complet");
        return ResponseEntity.ok(dashboardUseCase.getCompleteDashboard());
    }
    
    @GetMapping("/financial-kpis")
    @Operation(summary = "KPIs financiers", description = "Retourne les indicateurs financiers")
    public ResponseEntity<Map<String, Object>> getFinancialKpis() {
        logger.info("GET /dashboard/financial-kpis - Récupération KPIs financiers");
        return ResponseEntity.ok(dashboardUseCase.getFinancialKpis());
    }
    
    @GetMapping("/academic-kpis")
    @Operation(summary = "KPIs académiques", description = "Retourne les indicateurs académiques")
    public ResponseEntity<Map<String, Object>> getAcademicKpis() {
        logger.info("GET /dashboard/academic-kpis - Récupération KPIs académiques");
        return ResponseEntity.ok(dashboardUseCase.getAcademicKpis());
    }
    
    @GetMapping("/security-kpis")
    @Operation(summary = "KPIs sécurité", description = "Retourne les indicateurs de sécurité")
    public ResponseEntity<Map<String, Object>> getSecurityKpis() {
        logger.info("GET /dashboard/security-kpis - Récupération KPIs sécurité");
        return ResponseEntity.ok(dashboardUseCase.getSecurityKpis());
    }
    
    // ==================== ENDPOINTS MISES À JOUR EN TEMPS RÉEL ====================
    
    @PostMapping("/system-alert")
    @Operation(summary = "Créer alerte système", description = "Crée une alerte système")
    public ResponseEntity<Map<String, Object>> createSystemAlert(@RequestBody Map<String, String> body) {
        logger.info("POST /dashboard/system-alert - Création alerte système");
        dashboardUseCase.createSystemAlert(
            body.get("alertType"),
            body.get("title"),
            body.get("message"),
            body.get("severity")
        );
        return ResponseEntity.ok(Map.of("success", true));
    }
}