package ecole_221.sn.ecole.modules.finance.application.port.usecase;

import ecole_221.sn.ecole.modules.finance.application.port.command.UpdateExpenseCommand;
import ecole_221.sn.ecole.modules.finance.application.port.command.UpdateExpenseStatusCommand;
import ecole_221.sn.ecole.modules.finance.domain.model.Expense;

/**
 * Input Port pour la mise à jour d'une dépense.
 */
public interface UpdateExpenseUseCase {
    /**
     * Met à jour les informations d'une dépense.
     */
    Expense update(UpdateExpenseCommand command);
    
    /**
     * Met à jour le statut d'une dépense.
     */
    Expense updateStatus(UpdateExpenseStatusCommand command);
}