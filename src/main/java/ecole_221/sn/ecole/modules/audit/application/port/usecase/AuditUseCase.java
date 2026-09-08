package ecole_221.sn.ecole.modules.audit.application.port.usecase;

import ecole_221.sn.ecole.modules.audit.application.port.dto.AuditLogDto;
import ecole_221.sn.ecole.modules.audit.application.port.dto.SecurityEventDto;

import java.util.List;
import java.util.Map;

/**
 * Interface des cas d'utilisation pour le module Audit.
 * Gère les logs d'audit et événements de sécurité.
 */
public interface AuditUseCase {
    
    // ==================== GESTION DES LOGS D'AUDIT ====================
    
    /**
     * Récupère tous les logs d'audit.
     */
    List<AuditLogDto> getAllAuditLogs();
    
    /**
     * Récupère les logs d'audit récents.
     */
    List<AuditLogDto> getRecentAuditLogs(int limit);
    
    /**
     * Récupère les logs d'audit par acteur.
     */
    List<AuditLogDto> getAuditLogsByActor(String actorId);
    
    /**
     * Récupère les logs d'audit par module.
     */
    List<AuditLogDto> getAuditLogsByModule(String module);
    
    /**
     * Récupère les logs d'audit par type d'entité.
     */
    List<AuditLogDto> getAuditLogsByEntityType(String entityType);
    
    /**
     * Crée un nouveau log d'audit.
     */
    AuditLogDto logAction(AuditLogDto auditLog);
    
    // ==================== GESTION DES ÉVÉNEMENTS DE SÉCURITÉ ====================
    
    /**
     * Récupère tous les événements de sécurité.
     */
    List<SecurityEventDto> getAllSecurityEvents();
    
    /**
     * Récupère les événements de sécurité récents.
     */
    List<SecurityEventDto> getRecentSecurityEvents(int limit);
    
    /**
     * Récupère les événements de sécurité par gravité.
     */
    List<SecurityEventDto> getSecurityEventsBySeverity(String severity);
    
    /**
     * Récupère les événements de sécurité par point de contrôle.
     */
    List<SecurityEventDto> getSecurityEventsByCheckpoint(String checkpointId);
    
    /**
     * Récupère les enquêtes en attente.
     */
    List<SecurityEventDto> getPendingInvestigations();
    
    /**
     * Crée un nouvel événement de sécurité.
     */
    SecurityEventDto createSecurityEvent(SecurityEventDto event);
    
    /**
     * Met à jour un événement de sécurité.
     */
    SecurityEventDto updateSecurityEvent(String id, SecurityEventDto event);
    
    /**
     * Résout un événement de sécurité.
     */
    void resolveSecurityEvent(String id, String resolutionNotes);
    
    // ==================== ANALYTIQUES ====================
    
    /**
     * Récupère les statistiques d'audit.
     */
    Map<String, Object> getAuditStatistics();
    
    /**
     * Récupère les statistiques de sécurité.
     */
    Map<String, Object> getSecurityStatistics();
    
    /**
     * Récupère les tendances de sécurité.
     */
    List<Map<String, Object>> getSecurityTrends();
    
    // ==================== HISTORIQUE ====================
    
    /**
     * Récupère l'historique d'une entité.
     */
    List<AuditLogDto> getEntityHistory(String entityType, String entityId);
    
    /**
     * Récupère l'historique de sécurité d'une personne.
     */
    List<SecurityEventDto> getPersonSecurityHistory(String personId);
}