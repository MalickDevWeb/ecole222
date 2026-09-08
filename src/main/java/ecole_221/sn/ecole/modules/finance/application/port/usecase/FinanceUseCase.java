package ecole_221.sn.ecole.modules.finance.application.port.usecase;

import ecole_221.sn.ecole.modules.finance.application.port.dto.TransactionDto;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.StudentFinanceJpaEntity;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.ExpenseJpaEntity;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.TransactionJpaEntity;
import java.util.List;
import java.util.Map;

/**
 * Interface des cas d'utilisation pour le module Finance.
 * Gère les finances des étudiants, dépenses et transactions.
 */
public interface FinanceUseCase {
    
    // ==================== GESTION DES FINANCES ÉTUDIANTS (compatibilité JPA) ====================
    
    List<StudentFinanceJpaEntity> getAllStudentFinances();
    StudentFinanceJpaEntity getStudentFinanceById(String id);
    StudentFinanceJpaEntity getStudentFinanceByStudentId(String studentId);
    List<StudentFinanceJpaEntity> getStudentFinancesByClasse(String classe);
    List<StudentFinanceJpaEntity> getStudentsWithDebt();
    StudentFinanceJpaEntity createStudentFinance(StudentFinanceJpaEntity studentFinance);
    StudentFinanceJpaEntity updateStudentFinance(String id, StudentFinanceJpaEntity studentFinance);
    void deleteStudentFinance(String id);
    StudentFinanceJpaEntity updateStudentStatus(String id, String status);
    
    // ==================== GESTION DES DÉPENSES (compatibilité JPA) ====================
    
    List<ExpenseJpaEntity> getAllExpenses();
    ExpenseJpaEntity getExpenseById(String id);
    List<ExpenseJpaEntity> getExpensesByCategory(String category);
    List<ExpenseJpaEntity> getRecentExpenses();
    ExpenseJpaEntity createExpense(ExpenseJpaEntity expense);
    ExpenseJpaEntity updateExpense(String id, ExpenseJpaEntity expense);
    void deleteExpense(String id);
    
    // ==================== GESTION DES TRANSACTIONS ====================
    
    /**
     * Récupère toutes les transactions.
     */
    List<TransactionDto> getAllTransactions();
    
    /**
     * Récupère les transactions par étudiant.
     */
    List<TransactionDto> getTransactionsByStudentId(String studentId);
    
    /**
     * Récupère les transactions récentes.
     */
    List<TransactionDto> getRecentTransactions();
    
    /**
     * Crée une nouvelle transaction.
     */
    TransactionDto createTransaction(TransactionDto transaction);
    
    // ==================== ANALYTIQUES (compatibilité JPA) ====================
    
    Map<String, Object> getFinanceDashboard();
    Long getTotalDebt();
    Long getTotalCollected();
    Map<String, Long> getExpensesByCategory();
    List<Map<String, Object>> getPaymentTrends();
    
    // ==================== AUTOMATION (compatibilité JPA) ====================
    
    void triggerPaymentReminders();
    void blockDebtors();
    Map<String, Object> getFinancialReport(String period);
}