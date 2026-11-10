package ecole_221.sn.ecole.modules.superadmin.application.port.dto;

import java.time.LocalDateTime;

/**
 * DTO de réponse pour un tenant (établissement scolaire).
 * Protège le domaine en ne l'exposant jamais directement via l'API.
 */
public record TenantDto(
        String id,
        String name,
        String slug,
        String type,
        String status,
        String country,
        String city,
        String address,
        String phone,
        String email,
        String website,
        String logo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime validatedAt,
        LocalDateTime suspendedAt,
        /** Mot de passe provisoire de l'administrateur, renvoyé UNE SEULE FOIS à la création (null sinon). */
        String motDePasseProvisoireAdmin
) {
    public TenantDto avecMotDePasse(String motDePasse) {
        return new TenantDto(id, name, slug, type, status, country, city, address, phone, email, website, logo,
                createdAt, updatedAt, validatedAt, suspendedAt, motDePasse);
    }
}
