package ecole_221.sn.ecole.modules.dashboard.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface DashboardKpiJpaRepository extends JpaRepository<DashboardKpiJpaEntity, String> {
    Optional<DashboardKpiJpaEntity> findByKpiKey(String kpiKey);
    List<DashboardKpiJpaEntity> findByCategory(String category);
    List<DashboardKpiJpaEntity> findAllByOrderByCategoryAscKpiNameAsc();
    
    @Query("SELECT k FROM DashboardKpiJpaEntity k WHERE k.category IN :categories")
    List<DashboardKpiJpaEntity> findByCategories(@Param("categories") List<String> categories);
}