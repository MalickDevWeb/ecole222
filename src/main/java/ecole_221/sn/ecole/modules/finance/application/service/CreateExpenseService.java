package ecole_221.sn.ecole.modules.finance.application.service;

import ecole_221.sn.ecole.modules.finance.application.port.command.CreateExpenseCommand;
import ecole_221.sn.ecole.modules.finance.application.port.usecase.CreateExpenseUseCase;
import ecole_221.sn.ecole.modules.finance.domain.model.Expense;
import ecole_221.sn.ecole.modules.finance.domain.model.ExpenseFactory;
import ecole_221.sn.ecole.modules.finance.domain.port.repository.ExpenseRepositoryPort;
import ecole_221.sn.ecole.modules.finance.domain.valueobject.ExpenseCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service implémentant le UseCase de création de dépense.
 */
@Service
public class CreateExpenseService implements CreateExpenseUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(CreateExpenseService.class);
    
    private final ExpenseRepositoryPort expenseRepositoryPort;
    
    public CreateExpenseService(ExpenseRepositoryPort expenseRepositoryPort) {
        this.expenseRepositoryPort = expenseRepositoryPort;
    }
    
    @Override
    public Expense create(CreateExpenseCommand command) {
        logger.info("Création d'une nouvelle dépense: {}", command.label());
        
        // Conversion des types simples vers Value Objects
        ExpenseCategory category = new ExpenseCategory(command.category());
        
        // Création via Factory
        Expense expense = ExpenseFactory.create(
            command.label(),
            category,
            command.amount(),
            command.date(),
            command.description()
        );
        
        // Sauvegarde
        Expense savedExpense = expenseRepositoryPort.save(expense);
        
        logger.info("Dépense créée avec succès: {}", savedExpense.getId());
        return savedExpense;
    }
}