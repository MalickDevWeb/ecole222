package ecole_221.sn.ecole.modules.finance.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface StudentFinanceJpaRepository extends JpaRepository<StudentFinanceJpaEntity, String> {
    Optional<StudentFinanceJpaEntity> findByStudentId(String studentId);
    /** Tous les dossiers d'un élève, le plus récent d'abord (un élève peut en avoir plusieurs). */
    List<StudentFinanceJpaEntity> findAllByStudentIdOrderByCreatedAtDesc(String studentId);
    /**
     * Mêmes dossiers, verrouillés jusqu'à la fin de la transaction (deux encaissements simultanés pour
     * le même élève ne peuvent plus s'écraser — le second attend que le premier ait recalculé le payé).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sf FROM StudentFinanceJpaEntity sf WHERE sf.studentId = :studentId ORDER BY sf.createdAt DESC")
    List<StudentFinanceJpaEntity> verrouillerParEleve(@Param("studentId") String studentId);
    List<StudentFinanceJpaEntity> findByClasse(String classe);
    List<StudentFinanceJpaEntity> findByStatus(String status);
    
    @Query("SELECT sf FROM StudentFinanceJpaEntity sf WHERE sf.debt > 0")
    List<StudentFinanceJpaEntity> findStudentsWithDebt();
    
    /** Reste dû total de l'école (APP-TL-22). */
    @Query("SELECT COALESCE(SUM(sf.debt), 0) FROM StudentFinanceJpaEntity sf WHERE sf.debt > 0")
    Long totalDebtByStatus();
    
    @Query("SELECT COUNT(sf) FROM StudentFinanceJpaEntity sf WHERE sf.status = :status")
    Long countByStatus(@Param("status") String status);
}