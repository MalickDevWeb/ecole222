package ecole_221.sn.ecole.modules.superadmin.application.port.command;

/**
 * Commande de mise à jour partielle d'un établissement scolaire (tenant).
 * Tous les champs sont optionnels (null = ne pas modifier).
 */
public record UpdateTenantCommand(
        // champs facultatifs (PATCH) mais, s'ils sont fournis, ils doivent être valides
        @jakarta.validation.constraints.Pattern(regexp = "^(?=.*\\S).{1,150}$", message = "Le nom de l'établissement ne peut pas être vide (150 caractères au plus)")
        String name,
        String country,
        String city,
        String address,
        @jakarta.validation.constraints.Pattern(regexp = "^$|^\\+?[0-9 .-]{6,20}$", message = "Numéro de téléphone invalide")
        String phone,
        @jakarta.validation.constraints.Email(message = "L'e-mail de l'établissement doit être valide")
        String email,
        String website,
        String logo
) {}
