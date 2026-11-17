package ecole_221.sn.ecole.modules.superadmin.application.port.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Commande de création d'un établissement scolaire (tenant).
 * Validée via jakarta.validation avant d'atteindre le domaine.
 */
public record CreateTenantCommand(

        @NotBlank(message = "Le nom de l'établissement est obligatoire")
        @jakarta.validation.constraints.Size(max = 150, message = "Le nom de l'établissement ne doit pas dépasser 150 caractères")
        String name,

        @NotBlank(message = "L'email de l'établissement est obligatoire")
        @Email(message = "L'email de l'établissement doit être valide")
        String email,

        String country,
        String city,
        String address,
        @jakarta.validation.constraints.Pattern(regexp = "^$|^\\+?[0-9 .-]{6,20}$", message = "Numéro de téléphone invalide (chiffres uniquement, ex. +221 77 123 45 67)")
        String phone,
        String website,
        String logo,

        // Informations de l'administrateur initial de l'école
        @NotBlank(message = "Le nom de l'administrateur est obligatoire")
        String adminNom,

        @NotBlank(message = "Le prénom de l'administrateur est obligatoire")
        String adminPrenom,

        @NotBlank(message = "L'email de l'administrateur est obligatoire")
        @Email(message = "L'email de l'administrateur doit être valide")
        String adminEmail,

        @jakarta.validation.constraints.Pattern(regexp = "^$|^\\+?[0-9 .-]{6,20}$", message = "Numéro de téléphone de l'administrateur invalide")
        String adminPhone
) {}
