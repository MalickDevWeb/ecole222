package ecole_221.sn.ecole.modules.audit.domain.port.repository;

import ecole_221.sn.ecole.modules.audit.domain.model.AuditLog;
import ecole_221.sn.ecole.modules.audit.domain.model.SecurityEvent;
import ecole_221.sn.ecole.modules.audit.domain.valueobject.SeverityLevel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Output Port pour la persistance des logs d'audit et événements de sécurité.
 * Définit les opérations de persistance pour les agrégats AuditLog et SecurityEvent.
 */
public interface AuditRepositoryPort {
    
    // ==================== REPOSITORY AUDIT LOGS ====================
    
    /**
     * Sauvegarde un log d'audit (création ou modification).
     */
    AuditLog saveAuditLog(AuditLog auditLog);
    
    /**
     * Récupère un log d'audit par son identifiant.
     */
    Optional<AuditLog> findAuditLogById(String id);
    
    /**
     * Récupère tous les logs d'audit.
     */
    List<AuditLog> findAllAuditLogs();
    
    /**
     * Récupère les logs d'audit par utilisateur.
     */
    List<AuditLog> findAuditLogsByUserId(String userId);
    
    /**
     * Récupère les logs d'audit par action.
     */
    List<AuditLog> findAuditLogsByAction(String action);
    
    /**
     * Récupère les logs d'audit entre deux dates.
     */
    List<AuditLog> findAuditLogsBetweenDates(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Supprime un log d'audit par son ID.
     */
    void deleteAuditLogById(String id);
    
    // ==================== REPOSITORY SECURITY EVENTS ====================
    
    /**
     * Sauvegarde un événement de sécurité (création ou modification).
     */
    SecurityEvent saveSecurityEvent(SecurityEvent securityEvent);
    
    /**
     * Récupère un événement de sécurité par son identifiant.
     */
    Optional<SecurityEvent> findSecurityEventById(String id);
    
    /**
     * Récupère tous les événements de sécurité.
     */
    List<SecurityEvent> findAllSecurityEvents();
    
    /**
     * Récupère les événements de sécurité par utilisateur.
     */
    List<SecurityEvent> findSecurityEventsByUserId(String userId);
    
    /**
     * Récupère les événements de sécurité par niveau de gravité.
     */
    List<SecurityEvent> findSecurityEventsBySeverity(SeverityLevel severity);
    
    /**
     * Récupère les événements de sécurité entre deux dates.
     */
    List<SecurityEvent> findSecurityEventsBetweenDates(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Supprime un événement de sécurité par son ID.
     */
    void deleteSecurityEventById(String id);
}