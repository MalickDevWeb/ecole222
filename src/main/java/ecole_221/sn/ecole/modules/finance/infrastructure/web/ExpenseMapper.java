package ecole_221.sn.ecole.modules.finance.infrastructure.web;

import ecole_221.sn.ecole.modules.finance.application.port.dto.ExpenseDto;
import ecole_221.sn.ecole.modules.finance.domain.model.Expense;

/**
 * Mapper pour convertir entre Expense (domaine) et ExpenseDto (API).
 */
public class ExpenseMapper {
    
    public static ExpenseDto toDto(Expense expense) {
        return new ExpenseDto(
            expense.getId(),
            expense.getLabel(),
            expense.getCategory().value(),
            expense.getAmount(),
            expense.getDate(),
            expense.getDescription(),
            expense.getStatus().value(),
            expense.getCreatedAt(),
            expense.getUpdatedAt()
        );
    }
    
    public static Expense fromDto(ExpenseDto dto) {
        // Note: Cette méthode n'est généralement pas utilisée car on passe par les Commands
        // Elle est disponible pour des cas spécifiques de reconstruction
        return null; // Préférer utiliser ExpenseFactory et les Commands
    }
}