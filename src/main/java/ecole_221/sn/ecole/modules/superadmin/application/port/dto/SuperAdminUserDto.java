package ecole_221.sn.ecole.modules.superadmin.application.port.dto;

/**
 * DTO pour représenter un utilisateur global (pour le tableau de bord Super Admin).
 */
public record SuperAdminUserDto(
        String id,
        String nom,
        String prenom,
        String email,
        String role,
        String matricule
) {}
