package ecole_221.sn.ecole.modules.referential.application.dto;

/**
 * Qui modifie un référentiel, et dans quel périmètre.
 *
 * @param organizationId école courante (null en mode plateforme)
 * @param admin          ADMIN de l'école ou administrateur plateforme
 * @param superAdmin     SUPER_ADMIN / PLATFORM_ADMIN
 * @param global         la modification vise la valeur plateforme (?scope=global)
 */
public record ReferenceCaller(String organizationId, boolean admin, boolean superAdmin, boolean global) {

    /** Périmètre cible : null = plateforme. */
    public String targetOrganizationId() {
        return global ? null : organizationId;
    }
}
