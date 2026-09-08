package ecole_221.sn.ecole.modules.finance.domain.port.repository;

import ecole_221.sn.ecole.modules.finance.domain.model.Expense;
import ecole_221.sn.ecole.modules.finance.domain.valueobject.ExpenseCategory;
import ecole_221.sn.ecole.modules.finance.domain.valueobject.ExpenseStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Output Port pour la persistance des dépenses.
 * Définit les opérations de persistance pour l'agrégat Expense.
 */
public interface ExpenseRepositoryPort {
    
    /**
     * Sauvegarde une dépense (création ou modification).
     */
    Expense save(Expense expense);
    
    /**
     * Récupère une dépense par son identifiant.
     */
    Optional<Expense> findById(String id);
    
    /**
     * Récupère toutes les dépenses.
     */
    List<Expense> findAll();
    
    /**
     * Supprime une dépense par son identifiant.
     */
    void deleteById(String id);
    
    /**
     * Récupère les dépenses par catégorie.
     */
    List<Expense> findByCategory(ExpenseCategory category);
    
    /**
     * Récupère les dépenses par statut.
     */
    List<Expense> findByStatus(ExpenseStatus status);
    
    /**
     * Récupère les dépenses entre deux dates.
     */
    List<Expense> findByDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Récupère les dépenses récentes depuis une date donnée.
     */
    List<Expense> findRecentExpenses(LocalDateTime startDate);
    
    /**
     * Calcule le total des dépenses payées pour une catégorie.
     */
    Long totalPaidByCategory(ExpenseCategory category);
}