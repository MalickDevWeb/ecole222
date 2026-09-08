package ecole_221.sn.ecole.modules.audit.domain.model;

import ecole_221.sn.ecole.modules.audit.domain.valueobject.SeverityLevel;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Factory pour créer des instances de SecurityEvent.
 * Permet d'accéder au constructeur privé de l'agrégat racine.
 */
public class SecurityEventFactory {
    
    /**
     * Crée un nouvel événement de sécurité avec un ID généré et l'horodatage actuel.
     */
    public static SecurityEvent create(String eventType, String userId, String description, 
                                      SeverityLevel severity, String sourceIp) {
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        return new SecurityEvent(id, eventType, userId, description, severity, now, sourceIp);
    }
    
    /**
     * Reconstruit un événement de sécurité existant (pour la persistance).
     */
    public static SecurityEvent reconstruct(String id, String eventType, String userId, String description, 
                                           SeverityLevel severity, LocalDateTime timestamp, String sourceIp) {
        return new SecurityEvent(id, eventType, userId, description, severity, timestamp, sourceIp);
    }
}