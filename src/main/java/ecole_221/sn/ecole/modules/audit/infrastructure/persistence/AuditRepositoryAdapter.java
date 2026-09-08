package ecole_221.sn.ecole.modules.audit.infrastructure.persistence;

import ecole_221.sn.ecole.modules.audit.domain.exception.AuditLogNotFoundException;
import ecole_221.sn.ecole.modules.audit.domain.exception.SecurityEventNotFoundException;
import ecole_221.sn.ecole.modules.audit.domain.model.AuditLog;
import ecole_221.sn.ecole.modules.audit.domain.model.AuditLogFactory;
import ecole_221.sn.ecole.modules.audit.domain.model.SecurityEvent;
import ecole_221.sn.ecole.modules.audit.domain.model.SecurityEventFactory;
import ecole_221.sn.ecole.modules.audit.domain.port.repository.AuditRepositoryPort;
import ecole_221.sn.ecole.modules.audit.domain.valueobject.SeverityLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptateur de repository pour le module Audit.
 * Implémente AuditRepositoryPort en utilisant les repositories JPA existants.
 */
@Component
public class AuditRepositoryAdapter implements AuditRepositoryPort {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditRepositoryAdapter.class);
    
    private final AuditLogJpaRepository auditLogJpaRepository;
    private final SecurityEventJpaRepository securityEventJpaRepository;
    
    public AuditRepositoryAdapter(AuditLogJpaRepository auditLogJpaRepository,
                                  SecurityEventJpaRepository securityEventJpaRepository) {
        this.auditLogJpaRepository = auditLogJpaRepository;
        this.securityEventJpaRepository = securityEventJpaRepository;
    }
    
    // ==================== REPOSITORY AUDIT LOGS ====================
    
    @Override
    public AuditLog saveAuditLog(AuditLog auditLog) {
        logger.debug("Sauvegarde du log d'audit: {}", auditLog.getId());
        AuditLogJpaEntity entity = toAuditLogEntity(auditLog);
        AuditLogJpaEntity savedEntity = auditLogJpaRepository.save(entity);
        return toAuditLog(savedEntity);
    }
    
    @Override
    public Optional<AuditLog> findAuditLogById(String id) {
        logger.debug("Récupération du log d'audit: {}", id);
        return auditLogJpaRepository.findById(id)
            .map(this::toAuditLog);
    }
    
    @Override
    public List<AuditLog> findAllAuditLogs() {
        logger.debug("Récupération de tous les logs d'audit depuis JPA");
        return auditLogJpaRepository.findAll().stream()
            .map(this::toAuditLog)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<AuditLog> findAuditLogsByUserId(String userId) {
        logger.debug("Récupération des logs d'audit par utilisateur: {}", userId);
        return auditLogJpaRepository.findByActorId(userId).stream()
            .map(this::toAuditLog)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<AuditLog> findAuditLogsByAction(String action) {
        logger.debug("Récupération des logs d'audit par action: {}", action);
        return auditLogJpaRepository.findByActionType(action).stream()
            .map(this::toAuditLog)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<AuditLog> findAuditLogsBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        logger.debug("Récupération des logs d'audit entre {} et {}", startDate, endDate);
        return auditLogJpaRepository.findByActionTimestampBetween(startDate, endDate).stream()
            .map(this::toAuditLog)
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteAuditLogById(String id) {
        logger.debug("Suppression du log d'audit: {}", id);
        if (!auditLogJpaRepository.existsById(id)) {
            throw new AuditLogNotFoundException(id);
        }
        auditLogJpaRepository.deleteById(id);
    }
    
    // ==================== REPOSITORY SECURITY EVENTS ====================
    
    @Override
    public SecurityEvent saveSecurityEvent(SecurityEvent securityEvent) {
        logger.debug("Sauvegarde de l'événement de sécurité: {}", securityEvent.getId());
        SecurityEventJpaEntity entity = toSecurityEventEntity(securityEvent);
        SecurityEventJpaEntity savedEntity = securityEventJpaRepository.save(entity);
        return toSecurityEvent(savedEntity);
    }
    
    @Override
    public Optional<SecurityEvent> findSecurityEventById(String id) {
        logger.debug("Récupération de l'événement de sécurité: {}", id);
        return securityEventJpaRepository.findById(id)
            .map(this::toSecurityEvent);
    }
    
    @Override
    public List<SecurityEvent> findAllSecurityEvents() {
        logger.debug("Récupération de tous les événements de sécurité depuis JPA");
        return securityEventJpaRepository.findAll().stream()
            .map(this::toSecurityEvent)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<SecurityEvent> findSecurityEventsByUserId(String userId) {
        logger.debug("Récupération des événements de sécurité par utilisateur: {}", userId);
        return securityEventJpaRepository.findByPersonId(userId).stream()
            .map(this::toSecurityEvent)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<SecurityEvent> findSecurityEventsBySeverity(SeverityLevel severity) {
        logger.debug("Récupération des événements de sécurité par gravité: {}", severity);
        return securityEventJpaRepository.findBySeverity(severity.value()).stream()
            .map(this::toSecurityEvent)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<SecurityEvent> findSecurityEventsBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        logger.debug("Récupération des événements de sécurité entre {} et {}", startDate, endDate);
        return securityEventJpaRepository.findByEventTimestampBetween(startDate, endDate).stream()
            .map(this::toSecurityEvent)
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteSecurityEventById(String id) {
        logger.debug("Suppression de l'événement de sécurité: {}", id);
        if (!securityEventJpaRepository.existsById(id)) {
            throw new SecurityEventNotFoundException(id);
        }
        securityEventJpaRepository.deleteById(id);
    }
    
    // ==================== MAPPERS JPA ↔ DOMAIN ====================
    
    private AuditLog toAuditLog(AuditLogJpaEntity entity) {
        return AuditLogFactory.reconstruct(
            entity.getId(),
            entity.getActorId(),
            entity.getActionType(),
            entity.getActionDescription(),
            entity.getActionTimestamp(),
            entity.getIpAddress(),
            entity.getUserAgent()
        );
    }
    
    private AuditLogJpaEntity toAuditLogEntity(AuditLog auditLog) {
        AuditLogJpaEntity entity = new AuditLogJpaEntity();
        entity.setId(auditLog.getId());
        entity.setActorId(auditLog.getUserId());
        entity.setActionType(auditLog.getAction());
        entity.setActionDescription(auditLog.getDescription());
        entity.setActionTimestamp(auditLog.getTimestamp());
        entity.setIpAddress(auditLog.getIpAddress());
        entity.setUserAgent(auditLog.getUserAgent());
        return entity;
    }
    
    private SecurityEvent toSecurityEvent(SecurityEventJpaEntity entity) {
        return SecurityEventFactory.reconstruct(
            entity.getId(),
            entity.getEventType(),
            entity.getPersonId(),
            entity.getEventDescription(),
            new SeverityLevel(entity.getSeverity()),
            entity.getEventTimestamp(),
            entity.getLocation()
        );
    }
    
    private SecurityEventJpaEntity toSecurityEventEntity(SecurityEvent securityEvent) {
        SecurityEventJpaEntity entity = new SecurityEventJpaEntity();
        entity.setId(securityEvent.getId());
        entity.setEventType(securityEvent.getEventType());
        entity.setPersonId(securityEvent.getUserId());
        entity.setEventDescription(securityEvent.getDescription());
        entity.setSeverity(securityEvent.getSeverity().value());
        entity.setEventTimestamp(securityEvent.getTimestamp());
        entity.setLocation(securityEvent.getSourceIp());
        return entity;
    }
}