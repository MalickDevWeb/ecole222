package ecole_221.sn.ecole.modules.finance.application.port.command;

/**
 * Command pour mettre à jour une dépense existante.
 */
public record UpdateExpenseCommand(
    String id,
    String label,
    String description
) {
    public UpdateExpenseCommand {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID ne peut pas être vide");
        }
        if (label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("Le libellé ne peut pas être vide");
        }
    }
}