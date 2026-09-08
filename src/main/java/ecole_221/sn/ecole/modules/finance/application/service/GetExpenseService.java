package ecole_221.sn.ecole.modules.finance.application.service;

import ecole_221.sn.ecole.modules.finance.application.port.usecase.GetExpenseUseCase;
import ecole_221.sn.ecole.modules.finance.domain.model.Expense;
import ecole_221.sn.ecole.modules.finance.domain.port.repository.ExpenseRepositoryPort;
import ecole_221.sn.ecole.modules.finance.domain.valueobject.ExpenseCategory;
import ecole_221.sn.ecole.modules.finance.domain.valueobject.ExpenseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service implémentant le UseCase de consultation des dépenses.
 */
@Service
public class GetExpenseService implements GetExpenseUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(GetExpenseService.class);
    
    private final ExpenseRepositoryPort expenseRepositoryPort;
    
    public GetExpenseService(ExpenseRepositoryPort expenseRepositoryPort) {
        this.expenseRepositoryPort = expenseRepositoryPort;
    }
    
    @Override
    public Optional<Expense> getById(String id) {
        logger.debug("Récupération de la dépense: {}", id);
        return expenseRepositoryPort.findById(id);
    }
    
    @Override
    public List<Expense> getAll() {
        logger.debug("Récupération de toutes les dépenses");
        return expenseRepositoryPort.findAll();
    }
    
    @Override
    public List<Expense> getByCategory(String category) {
        logger.debug("Récupération des dépenses par catégorie: {}", category);
        ExpenseCategory expenseCategory = new ExpenseCategory(category);
        return expenseRepositoryPort.findByCategory(expenseCategory);
    }
    
    @Override
    public List<Expense> getByStatus(String status) {
        logger.debug("Récupération des dépenses par statut: {}", status);
        ExpenseStatus expenseStatus = new ExpenseStatus(status);
        return expenseRepositoryPort.findByStatus(expenseStatus);
    }
    
    @Override
    public List<Expense> getByDateBetween(LocalDateTime startDate, LocalDateTime endDate) {
        logger.debug("Récupération des dépenses entre {} et {}", startDate, endDate);
        return expenseRepositoryPort.findByDateBetween(startDate, endDate);
    }
    
    @Override
    public List<Expense> getRecentExpenses(LocalDateTime startDate) {
        logger.debug("Récupération des dépenses récentes depuis {}", startDate);
        return expenseRepositoryPort.findRecentExpenses(startDate);
    }
    
    @Override
    public Long getTotalPaidByCategory(String category) {
        logger.debug("Calcul du total des dépenses payées pour la catégorie: {}", category);
        ExpenseCategory expenseCategory = new ExpenseCategory(category);
        return expenseRepositoryPort.totalPaidByCategory(expenseCategory);
    }
}