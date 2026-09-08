package ecole_221.sn.ecole.modules.platform.organization.domain.model;

import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Factory pour la création d'instances Organization.
 * Suit le pattern Factory pour garantir la création cohérente des agrégats.
 */
public class OrganizationFactory {
    
    /**
     * Crée une nouvelle organization avec statut PENDING.
     */
    public static Organization create(String name, OrganizationType type, String email) {
        return create(
            name,
            type,
            OrganizationStatus.PENDING,
            null, // country
            null, // city
            null, // address
            null, // phone
            email,
            null, // website
            null, // logo
            null, // subscriptionId
            null, // primaryDomainId
            null  // settings
        );
    }
    
    /**
     * Crée une nouvelle organization avec tous les détails.
     */
    public static Organization create(String name, OrganizationType type, OrganizationStatus status,
                                     String country, String city, String address, String phone, String email,
                                     String website, String logo, String subscriptionId, String primaryDomainId,
                                     Map<String, Object> settings) {
        
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        
        return Organization.createInternal(
            id,
            name,
            type,
            status,
            country,
            city,
            address,
            phone,
            email,
            website,
            logo,
            subscriptionId,
            primaryDomainId,
            settings,
            now,
            now,
            null, // validatedAt
            null  // suspendedAt
        );
    }
    
    /**
     * Reconstruit une organization depuis la base de données.
     */
    public static Organization reconstruct(String id, String name, String type, String status,
                                         String country, String city, String address, String phone, String email,
                                         String website, String logo, String subscriptionId, String primaryDomainId,
                                         Map<String, Object> settings, LocalDateTime createdAt, LocalDateTime updatedAt,
                                         LocalDateTime validatedAt, LocalDateTime suspendedAt) {
        
        return Organization.createInternal(
            id,
            name,
            OrganizationType.fromString(type),
            OrganizationStatus.fromString(status),
            country,
            city,
            address,
            phone,
            email,
            website,
            logo,
            subscriptionId,
            primaryDomainId,
            settings,
            createdAt,
            updatedAt,
            validatedAt,
            suspendedAt
        );
    }
}
