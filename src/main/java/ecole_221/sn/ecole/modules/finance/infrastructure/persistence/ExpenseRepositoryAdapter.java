package ecole_221.sn.ecole.modules.finance.infrastructure.persistence;

import ecole_221.sn.ecole.modules.finance.domain.exception.ExpenseNotFoundException;
import ecole_221.sn.ecole.modules.finance.domain.model.Expense;
import ecole_221.sn.ecole.modules.finance.domain.model.ExpenseFactory;
import ecole_221.sn.ecole.modules.finance.domain.port.repository.ExpenseRepositoryPort;
import ecole_221.sn.ecole.modules.finance.domain.valueobject.ExpenseCategory;
import ecole_221.sn.ecole.modules.finance.domain.valueobject.ExpenseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptateur de repository pour l'agrégat Expense.
 * Implémente ExpenseRepositoryPort en utilisant ExpenseJpaRepository.
 */
@Component
public class ExpenseRepositoryAdapter implements ExpenseRepositoryPort {
    
    private static final Logger logger = LoggerFactory.getLogger(ExpenseRepositoryAdapter.class);
    
    private final ExpenseJpaRepository expenseJpaRepository;
    
    public ExpenseRepositoryAdapter(ExpenseJpaRepository expenseJpaRepository) {
        this.expenseJpaRepository = expenseJpaRepository;
    }
    
    @Override
    public Expense save(Expense expense) {
        logger.debug("Sauvegarde de la dépense: {}", expense.getId());
        ExpenseJpaEntity entity = toExpenseEntity(expense);
        ExpenseJpaEntity savedEntity = expenseJpaRepository.save(entity);
        return toExpense(savedEntity);
    }
    
    @Override
    public Optional<Expense> findById(String id) {
        logger.debug("Récupération de la dépense: {}", id);
        return expenseJpaRepository.findById(id)
            .map(this::toExpense);
    }
    
    @Override
    public List<Expense> findAll() {
        logger.debug("Récupération de toutes les dépenses");
        return expenseJpaRepository.findAll().stream()
            .map(this::toExpense)
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(String id) {
        logger.debug("Suppression de la dépense: {}", id);
        if (!expenseJpaRepository.existsById(id)) {
            throw new ExpenseNotFoundException(id);
        }
        expenseJpaRepository.deleteById(id);
    }
    
    @Override
    public List<Expense> findByCategory(ExpenseCategory category) {
        logger.debug("Récupération des dépenses par catégorie: {}", category);
        return expenseJpaRepository.findByCategory(category.value()).stream()
            .map(this::toExpense)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Expense> findByStatus(ExpenseStatus status) {
        logger.debug("Récupération des dépenses par statut: {}", status);
        return expenseJpaRepository.findByStatus(status.value()).stream()
            .map(this::toExpense)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Expense> findByDateBetween(LocalDateTime startDate, LocalDateTime endDate) {
        logger.debug("Récupération des dépenses entre {} et {}", startDate, endDate);
        return expenseJpaRepository.findByDateBetween(startDate, endDate).stream()
            .map(this::toExpense)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Expense> findRecentExpenses(LocalDateTime startDate) {
        logger.debug("Récupération des dépenses récentes depuis {}", startDate);
        return expenseJpaRepository.findRecentExpenses(startDate).stream()
            .map(this::toExpense)
            .collect(Collectors.toList());
    }
    
    @Override
    public Long totalPaidByCategory(ExpenseCategory category) {
        logger.debug("Calcul du total des dépenses payées pour la catégorie: {}", category);
        Long total = expenseJpaRepository.totalByCategory(category.value());
        return total != null ? total : 0L;
    }
    
    // ==================== MAPPERS JPA ↔ DOMAIN ====================
    
    private Expense toExpense(ExpenseJpaEntity entity) {
        return ExpenseFactory.reconstruct(
            entity.getId(),
            entity.getLabel(),
            new ExpenseCategory(entity.getCategory()),
            entity.getAmount(),
            entity.getDate(),
            entity.getDescription(),
            new ExpenseStatus(entity.getStatus()),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
    
    private ExpenseJpaEntity toExpenseEntity(Expense expense) {
        ExpenseJpaEntity entity = new ExpenseJpaEntity();
        entity.setId(expense.getId());
        entity.setLabel(expense.getLabel());
        entity.setCategory(expense.getCategory().value());
        entity.setAmount(expense.getAmount());
        entity.setDate(expense.getDate());
        entity.setDescription(expense.getDescription());
        entity.setStatus(expense.getStatus().value());
        entity.setCreatedAt(expense.getCreatedAt());
        entity.setUpdatedAt(expense.getUpdatedAt());
        return entity;
    }
}