package ecole_221.sn.ecole.modules.finance.application.port.command;

/**
 * Command pour mettre à jour le statut d'une dépense.
 */
public record UpdateExpenseStatusCommand(
    String id,
    String status
) {
    public UpdateExpenseStatusCommand {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID ne peut pas être vide");
        }
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Le statut ne peut pas être vide");
        }
    }
}