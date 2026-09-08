package ecole_221.sn.ecole.modules.finance.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionJpaRepository extends JpaRepository<TransactionJpaEntity, String> {
    List<TransactionJpaEntity> findByStudentFinanceId(String studentFinanceId);
    List<TransactionJpaEntity> findByStudentId(String studentId);
    List<TransactionJpaEntity> findByTransactionType(String transactionType);
    List<TransactionJpaEntity> findByPaymentMethod(String paymentMethod);
    List<TransactionJpaEntity> findByTransactionDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT SUM(t.amount) FROM TransactionJpaEntity t WHERE t.transactionType = 'Paiement' AND t.transactionDate >= :startDate")
    Long totalPaymentsSince(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT t FROM TransactionJpaEntity t WHERE t.transactionDate >= :startDate ORDER BY t.transactionDate DESC")
    List<TransactionJpaEntity> findRecentTransactions(@Param("startDate") LocalDateTime startDate);
}