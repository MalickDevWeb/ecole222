package ecole_221.sn.ecole.modules.audit.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Factory pour créer des instances de AuditLog.
 * Permet d'accéder au constructeur privé de l'agrégat racine.
 */
public class AuditLogFactory {
    
    /**
     * Crée un nouveau log d'audit avec un ID généré et l'horodatage actuel.
     */
    public static AuditLog create(String userId, String action, String description, 
                                  String ipAddress, String userAgent) {
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        return new AuditLog(id, userId, action, description, now, ipAddress, userAgent);
    }
    
    /**
     * Reconstruit un log d'audit existant (pour la persistance).
     */
    public static AuditLog reconstruct(String id, String userId, String action, String description, 
                                       LocalDateTime timestamp, String ipAddress, String userAgent) {
        return new AuditLog(id, userId, action, description, timestamp, ipAddress, userAgent);
    }
}