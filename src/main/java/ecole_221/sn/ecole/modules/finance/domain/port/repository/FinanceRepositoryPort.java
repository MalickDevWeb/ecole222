package ecole_221.sn.ecole.modules.finance.domain.port.repository;

import ecole_221.sn.ecole.modules.finance.domain.model.Transaction;

import java.util.List;
import java.util.Optional;

/**
 * Interface du repository pour le module Finance.
 * Définit les opérations de persistance pour les transactions financières.
 */
public interface FinanceRepositoryPort {
    
    /**
     * Récupère toutes les transactions.
     */
    List<Transaction> findAllTransactions();
    
    /**
     * Récupère une transaction par son identifiant.
     */
    Optional<Transaction> findTransactionById(String id);
    
    /**
     * Sauvegarde une transaction.
     */
    Transaction saveTransaction(Transaction transaction);
    
    /**
     * Supprime une transaction.
     */
    void deleteTransaction(String id);
    
    /**
     * Récupère les transactions par étudiant.
     */
    List<Transaction> findTransactionsByStudentId(String studentId);
}