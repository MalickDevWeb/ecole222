package ecole_221.sn.ecole.modules.finance.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface StudentFinanceJpaRepository extends JpaRepository<StudentFinanceJpaEntity, String> {
    Optional<StudentFinanceJpaEntity> findByStudentId(String studentId);
    List<StudentFinanceJpaEntity> findByClasse(String classe);
    List<StudentFinanceJpaEntity> findByStatus(String status);
    
    @Query("SELECT sf FROM StudentFinanceJpaEntity sf WHERE sf.debt > 0")
    List<StudentFinanceJpaEntity> findStudentsWithDebt();
    
    @Query("SELECT SUM(sf.debt) FROM StudentFinanceJpaEntity sf WHERE sf.status = 'En Retard'")
    Long totalDebtByStatus();
    
    @Query("SELECT COUNT(sf) FROM StudentFinanceJpaEntity sf WHERE sf.status = :status")
    Long countByStatus(@Param("status") String status);
}