package ecole_221.sn.ecole.modules.finance.application.port.command;

import java.time.LocalDateTime;

/**
 * Command pour créer une nouvelle dépense.
 * Contient des types simples uniquement (String, Long, LocalDateTime).
 */
public record CreateExpenseCommand(
    String label,
    String category,
    Long amount,
    LocalDateTime date,
    String description
) {
    public CreateExpenseCommand {
        if (label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("Le libellé ne peut pas être vide");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("La catégorie ne peut pas être vide");
        }
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }
        if (date == null) {
            throw new IllegalArgumentException("La date ne peut pas être null");
        }
    }
}