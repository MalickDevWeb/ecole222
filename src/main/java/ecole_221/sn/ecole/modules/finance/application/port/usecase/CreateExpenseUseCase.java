package ecole_221.sn.ecole.modules.finance.application.port.usecase;

import ecole_221.sn.ecole.modules.finance.application.port.command.CreateExpenseCommand;
import ecole_221.sn.ecole.modules.finance.domain.model.Expense;

/**
 * Input Port pour la création d'une dépense.
 */
public interface CreateExpenseUseCase {
    /**
     * Crée une nouvelle dépense.
     */
    Expense create(CreateExpenseCommand command);
}