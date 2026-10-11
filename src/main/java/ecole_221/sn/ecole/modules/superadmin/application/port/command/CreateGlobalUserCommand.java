package ecole_221.sn.ecole.modules.superadmin.application.port.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Command pour la création d'un utilisateur global.
 * Utilise jakarta.validation pour le fail-fast (Validation Infrastructure).
 */
public record CreateGlobalUserCommand(
        @NotBlank(message = "Le nom est obligatoire")
        String nom,

        @NotBlank(message = "Le prénom est obligatoire")
        String prenom,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        String password,

        @NotBlank(message = "Le rôle est obligatoire")
        @Pattern(regexp = "^(SUPER_ADMIN|PLATFORM_ADMIN|ADMIN|ADMINISTRATOR)$", 
                 message = "Le rôle doit être SUPER_ADMIN ou ADMIN")
        String role
) {}
