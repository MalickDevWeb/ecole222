package ecole_221.sn.ecole.modules.superadmin.application.port.command;

/**
 * Commande de mise à jour partielle d'un établissement scolaire (tenant).
 * Tous les champs sont optionnels (null = ne pas modifier).
 */
public record UpdateTenantCommand(
        String name,
        String country,
        String city,
        String address,
        String phone,
        String email,
        String website,
        String logo
) {}
