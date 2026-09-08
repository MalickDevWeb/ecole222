package ecole_221.sn.ecole.modules.dashboard.infrastructure.persistence;

import ecole_221.sn.ecole.modules.dashboard.domain.model.DashboardKpi;
import ecole_221.sn.ecole.modules.dashboard.domain.model.DashboardKpiFactory;
import ecole_221.sn.ecole.modules.dashboard.domain.port.repository.DashboardRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptateur de repository pour le module Dashboard.
 * Implémente DashboardRepositoryPort en utilisant les repositories JPA existants.
 */
@Component
public class DashboardRepositoryAdapter implements DashboardRepositoryPort {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardRepositoryAdapter.class);
    
    private final DashboardKpiJpaRepository dashboardKpiJpaRepository;
    
    public DashboardRepositoryAdapter(DashboardKpiJpaRepository dashboardKpiJpaRepository) {
        this.dashboardKpiJpaRepository = dashboardKpiJpaRepository;
    }
    
    @Override
    public List<DashboardKpi> findAllKpis() {
        logger.debug("Récupération de tous les KPIs depuis JPA");
        return dashboardKpiJpaRepository.findAll().stream()
            .map(this::toDashboardKpi)
            .collect(Collectors.toList());
    }
    
    @Override
    public Optional<DashboardKpi> findKpiById(String id) {
        logger.debug("Récupération du KPI: {}", id);
        return dashboardKpiJpaRepository.findById(id)
            .map(this::toDashboardKpi);
    }
    
    @Override
    public DashboardKpi saveKpi(DashboardKpi kpi) {
        logger.debug("Sauvegarde du KPI: {}", kpi.getId());
        DashboardKpiJpaEntity entity = toDashboardKpiEntity(kpi);
        DashboardKpiJpaEntity savedEntity = dashboardKpiJpaRepository.save(entity);
        return toDashboardKpi(savedEntity);
    }
    
    @Override
    public void deleteKpi(String id) {
        logger.debug("Suppression du KPI: {}", id);
        dashboardKpiJpaRepository.deleteById(id);
    }
    
    // ==================== MAPPERS ENTITIES VERS DOMAIN ====================

    private DashboardKpi toDashboardKpi(DashboardKpiJpaEntity entity) {
        String unit = entity.getCurrency() != null ? entity.getCurrency() : entity.getValueType();
        return DashboardKpiFactory.reconstitute(
            entity.getId(),
            entity.getKpiName(),
            entity.getValue(),
            unit,
            entity.getTrend()
        );
    }
    
    private DashboardKpiJpaEntity toDashboardKpiEntity(DashboardKpi kpi) {
        DashboardKpiJpaEntity entity = new DashboardKpiJpaEntity();
        entity.setId(kpi.getId());
        entity.setKpiName(kpi.getName());
        entity.setValue(kpi.getValue());
        entity.setCurrency(kpi.getUnit());
        entity.setTrend(kpi.getTrend());
        return entity;
    }
}