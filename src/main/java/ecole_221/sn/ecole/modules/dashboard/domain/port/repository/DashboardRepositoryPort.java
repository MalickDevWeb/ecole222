package ecole_221.sn.ecole.modules.dashboard.domain.port.repository;

import ecole_221.sn.ecole.modules.dashboard.domain.model.DashboardKpi;

import java.util.List;
import java.util.Optional;

/**
 * Interface du repository pour le module Dashboard.
 * Définit les opérations de persistance pour les KPIs et statistiques.
 */
public interface DashboardRepositoryPort {
    
    /**
     * Récupère tous les KPIs du dashboard.
     */
    List<DashboardKpi> findAllKpis();
    
    /**
     * Récupère un KPI par son identifiant.
     */
    Optional<DashboardKpi> findKpiById(String id);
    
    /**
     * Sauvegarde un KPI.
     */
    DashboardKpi saveKpi(DashboardKpi kpi);
    
    /**
     * Supprime un KPI.
     */
    void deleteKpi(String id);
}