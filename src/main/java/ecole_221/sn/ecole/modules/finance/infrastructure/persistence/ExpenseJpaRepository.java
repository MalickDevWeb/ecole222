package ecole_221.sn.ecole.modules.finance.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface ExpenseJpaRepository extends JpaRepository<ExpenseJpaEntity, String> {
    List<ExpenseJpaEntity> findByCategory(String category);
    List<ExpenseJpaEntity> findByStatus(String status);
    List<ExpenseJpaEntity> findByDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT SUM(e.amount) FROM ExpenseJpaEntity e WHERE e.category = :category AND e.status = 'Payé'")
    Long totalByCategory(@Param("category") String category);
    
    @Query("SELECT e FROM ExpenseJpaEntity e WHERE e.date >= :startDate ORDER BY e.date DESC")
    List<ExpenseJpaEntity> findRecentExpenses(@Param("startDate") LocalDateTime startDate);
}