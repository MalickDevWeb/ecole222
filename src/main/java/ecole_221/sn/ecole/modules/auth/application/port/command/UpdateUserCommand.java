package ecole_221.sn.ecole.modules.auth.application.port.command;

/**
 * Command pour mettre à jour un utilisateur existant.
 */
public record UpdateUserCommand(
    String id,
    String nom,
    String prenom,
    String photoUrl
) {
    public UpdateUserCommand {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID ne peut pas être vide");
        }
        if (nom == null || nom.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom ne peut pas être vide");
        }
        if (prenom == null || prenom.trim().isEmpty()) {
            throw new IllegalArgumentException("Le prénom ne peut pas être vide");
        }
    }
}