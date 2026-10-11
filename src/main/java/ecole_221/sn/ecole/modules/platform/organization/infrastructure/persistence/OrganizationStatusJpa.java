package ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence;

/**
 * Enum JPA pour OrganizationStatus.
 * Utilisé pour la persistance en base de données.
 */
public enum OrganizationStatusJpa {
    PENDING,
    ACTIVE,
    SUSPENDED,
    REJECTED,
    EXPIRED,
    CANCELLED,
    ARCHIVED
}
