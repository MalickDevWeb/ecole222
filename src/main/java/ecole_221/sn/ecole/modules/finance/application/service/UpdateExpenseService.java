package ecole_221.sn.ecole.modules.finance.application.service;

import ecole_221.sn.ecole.modules.finance.application.port.command.UpdateExpenseCommand;
import ecole_221.sn.ecole.modules.finance.application.port.command.UpdateExpenseStatusCommand;
import ecole_221.sn.ecole.modules.finance.application.port.usecase.UpdateExpenseUseCase;
import ecole_221.sn.ecole.modules.finance.domain.exception.ExpenseNotFoundException;
import ecole_221.sn.ecole.modules.finance.domain.model.Expense;
import ecole_221.sn.ecole.modules.finance.domain.port.repository.ExpenseRepositoryPort;
import ecole_221.sn.ecole.modules.finance.domain.valueobject.ExpenseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service implémentant le UseCase de mise à jour de dépense.
 */
@Service
public class UpdateExpenseService implements UpdateExpenseUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(UpdateExpenseService.class);
    
    private final ExpenseRepositoryPort expenseRepositoryPort;
    
    public UpdateExpenseService(ExpenseRepositoryPort expenseRepositoryPort) {
        this.expenseRepositoryPort = expenseRepositoryPort;
    }
    
    @Override
    public Expense update(UpdateExpenseCommand command) {
        logger.info("Mise à jour de la dépense: {}", command.id());
        
        // Récupération de la dépense existante
        Expense expense = expenseRepositoryPort.findById(command.id())
            .orElseThrow(() -> new ExpenseNotFoundException(command.id()));
        
        // Mise à jour des champs
        expense.updateLabel(command.label());
        expense.updateDescription(command.description());
        
        // Sauvegarde
        Expense updatedExpense = expenseRepositoryPort.save(expense);
        
        logger.info("Dépense mise à jour avec succès: {}", updatedExpense.getId());
        return updatedExpense;
    }
    
    @Override
    public Expense updateStatus(UpdateExpenseStatusCommand command) {
        logger.info("Mise à jour du statut de la dépense: {} -> {}", command.id(), command.status());
        
        // Récupération de la dépense existante
        Expense expense = expenseRepositoryPort.findById(command.id())
            .orElseThrow(() -> new ExpenseNotFoundException(command.id()));
        
        // Conversion String vers Value Object et mise à jour
        ExpenseStatus status = new ExpenseStatus(command.status());
        
        if (status.equals(ExpenseStatus.PAYE)) {
            expense.markAsPaid();
        } else if (status.equals(ExpenseStatus.EN_ATTENTE)) {
            expense.markAsPending();
        } else if (status.equals(ExpenseStatus.ANNULE)) {
            expense.cancel();
        }
        
        // Sauvegarde
        Expense updatedExpense = expenseRepositoryPort.save(expense);
        
        logger.info("Statut de la dépense mis à jour avec succès: {}", updatedExpense.getId());
        return updatedExpense;
    }
}