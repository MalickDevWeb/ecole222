package ecole_221.sn.ecole.modules.auth.application.port.command;

/**
 * Command pour créer un nouvel utilisateur.
 * Contient des types simples uniquement (String).
 */
public record CreateUserCommand(
    String nom,
    String prenom,
    String email,
    String password,
    String role,
    String matricule,
    String photoUrl
) {
    public CreateUserCommand {
        if (nom == null || nom.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom ne peut pas être vide");
        }
        if (prenom == null || prenom.trim().isEmpty()) {
            throw new IllegalArgumentException("Le prénom ne peut pas être vide");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("L'email ne peut pas être vide");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 6 caractères");
        }
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Le rôle ne peut pas être vide");
        }
    }
}