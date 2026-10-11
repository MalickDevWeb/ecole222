package ecole_221.sn.ecole.modules.superadmin.application.port.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

/**
 * Commande de mise à jour partielle d'un utilisateur global.
 * Tous les champs sont optionnels (null = ne pas modifier).
 */
public record UpdateGlobalUserCommand(
        String nom,
        String prenom,

        @Email(message = "Format d'email invalide")
        String email,

        @Pattern(regexp = "^(SUPER_ADMIN|PLATFORM_ADMIN|ADMIN|ADMINISTRATOR)$",
                 message = "Le rôle doit être SUPER_ADMIN, PLATFORM_ADMIN ou ADMIN")
        String role,

        String telephone,
        String photoUrl
) {}
