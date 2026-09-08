package ecole_221.sn.ecole.modules.finance.application.port.usecase;

import ecole_221.sn.ecole.modules.finance.domain.model.Expense;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Input Port pour la consultation des dépenses.
 */
public interface GetExpenseUseCase {
    /**
     * Récupère une dépense par son ID.
     */
    Optional<Expense> getById(String id);
    
    /**
     * Récupère toutes les dépenses.
     */
    List<Expense> getAll();
    
    /**
     * Récupère les dépenses par catégorie.
     */
    List<Expense> getByCategory(String category);
    
    /**
     * Récupère les dépenses par statut.
     */
    List<Expense> getByStatus(String status);
    
    /**
     * Récupère les dépenses entre deux dates.
     */
    List<Expense> getByDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Récupère les dépenses récentes.
     */
    List<Expense> getRecentExpenses(LocalDateTime startDate);
    
    /**
     * Calcule le total des dépenses payées par catégorie.
     */
    Long getTotalPaidByCategory(String category);
}