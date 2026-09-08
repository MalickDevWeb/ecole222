package ecole_221.sn.ecole.modules.audit.application.service;

import ecole_221.sn.ecole.modules.audit.application.port.dto.AuditLogDto;
import ecole_221.sn.ecole.modules.audit.application.port.dto.SecurityEventDto;
import ecole_221.sn.ecole.modules.audit.application.port.usecase.AuditUseCase;
import ecole_221.sn.ecole.modules.audit.domain.model.AuditLog;
import ecole_221.sn.ecole.modules.audit.domain.model.AuditLogFactory;
import ecole_221.sn.ecole.modules.audit.domain.model.SecurityEvent;
import ecole_221.sn.ecole.modules.audit.domain.model.SecurityEventFactory;
import ecole_221.sn.ecole.modules.audit.domain.port.repository.AuditRepositoryPort;
import ecole_221.sn.ecole.modules.audit.domain.valueobject.SeverityLevel;
import ecole_221.sn.ecole.modules.audit.infrastructure.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuditUseCaseImpl implements AuditUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditUseCaseImpl.class);
    
    private final AuditRepositoryPort auditRepositoryPort;
    private final AuditLogJpaRepository auditLogJpaRepository;
    private final SecurityEventJpaRepository securityEventJpaRepository;
    
    public AuditUseCaseImpl(AuditRepositoryPort auditRepositoryPort,
                          AuditLogJpaRepository auditLogJpaRepository,
                          SecurityEventJpaRepository securityEventJpaRepository) {
        this.auditRepositoryPort = auditRepositoryPort;
        this.auditLogJpaRepository = auditLogJpaRepository;
        this.securityEventJpaRepository = securityEventJpaRepository;
    }
    
    // ==================== GESTION DES LOGS D'AUDIT ====================
    
    @Override
    public List<AuditLogDto> getAllAuditLogs() {
        logger.info("Récupération de tous les logs d'audit");
        return auditRepositoryPort.findAllAuditLogs().stream()
            .map(this::toAuditLogDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<AuditLogDto> getRecentAuditLogs(int limit) {
        logger.info("Récupération des {} logs d'audit récents", limit);
        return auditLogJpaRepository.findTop50RecentAuditLogs(LocalDateTime.now().minusDays(7)).stream()
            .limit(limit)
            .map(this::toAuditLogDtoFromEntity)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<AuditLogDto> getAuditLogsByActor(String actorId) {
        logger.info("Récupération des logs d'audit par acteur: {}", actorId);
        return auditLogJpaRepository.findByActorId(actorId).stream()
            .map(this::toAuditLogDtoFromEntity)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<AuditLogDto> getAuditLogsByModule(String module) {
        logger.info("Récupération des logs d'audit par module: {}", module);
        return auditLogJpaRepository.findByModule(module).stream()
            .map(this::toAuditLogDtoFromEntity)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<AuditLogDto> getAuditLogsByEntityType(String entityType) {
        logger.info("Récupération des logs d'audit par type d'entité: {}", entityType);
        return auditLogJpaRepository.findByEntityType(entityType).stream()
            .map(this::toAuditLogDtoFromEntity)
            .collect(Collectors.toList());
    }
    
    @Override
    public AuditLogDto logAction(AuditLogDto auditLogDto) {
        logger.info("Création d'un log d'audit: {}", auditLogDto.action());
        // Use Factory to create new instance
        AuditLog auditLog = AuditLogFactory.create(
            auditLogDto.userId(),
            auditLogDto.action(),
            auditLogDto.description(),
            auditLogDto.ipAddress(),
            auditLogDto.userAgent()
        );
        AuditLog savedAuditLog = auditRepositoryPort.saveAuditLog(auditLog);
        return toAuditLogDto(savedAuditLog);
    }
    
    // ==================== GESTION DES ÉVÉNEMENTS DE SÉCURITÉ ====================
    
    @Override
    public List<SecurityEventDto> getAllSecurityEvents() {
        logger.info("Récupération de tous les événements de sécurité");
        return auditRepositoryPort.findAllSecurityEvents().stream()
            .map(this::toSecurityEventDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<SecurityEventDto> getRecentSecurityEvents(int limit) {
        logger.info("Récupération des {} événements de sécurité récents", limit);
        return securityEventJpaRepository.findTop20RecentSecurityEvents(LocalDateTime.now().minusDays(7)).stream()
            .limit(limit)
            .map(this::toSecurityEventDtoFromEntity)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<SecurityEventDto> getSecurityEventsBySeverity(String severity) {
        logger.info("Récupération des événements de sécurité par gravité: {}", severity);
        // Convert String to SeverityLevel for domain method
        SeverityLevel severityLevel = SeverityLevel.of(severity);
        return auditRepositoryPort.findSecurityEventsBySeverity(severityLevel).stream()
            .map(this::toSecurityEventDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<SecurityEventDto> getSecurityEventsByCheckpoint(String checkpointId) {
        logger.info("Récupération des événements de sécurité par point de contrôle: {}", checkpointId);
        return securityEventJpaRepository.findByCheckpointId(checkpointId).stream()
            .map(this::toSecurityEventDtoFromEntity)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<SecurityEventDto> getPendingInvestigations() {
        logger.info("Récupération des enquêtes en attente");
        return securityEventJpaRepository.findPendingInvestigations().stream()
            .map(this::toSecurityEventDtoFromEntity)
            .collect(Collectors.toList());
    }
    
    @Override
    public SecurityEventDto createSecurityEvent(SecurityEventDto eventDto) {
        logger.info("Création d'un événement de sécurité: {}", eventDto.eventType());
        // Use Factory to create new instance
        SeverityLevel severity = null;
        if (eventDto.severity() != null && !eventDto.severity().isEmpty()) {
            severity = SeverityLevel.of(eventDto.severity());
        }
        
        SecurityEvent securityEvent = SecurityEventFactory.create(
            eventDto.eventType(),
            eventDto.userId(),
            eventDto.description(),
            severity,
            eventDto.sourceIp()
        );
        SecurityEvent savedEvent = auditRepositoryPort.saveSecurityEvent(securityEvent);
        return toSecurityEventDto(savedEvent);
    }
    
    @Override
    public SecurityEventDto updateSecurityEvent(String id, SecurityEventDto eventDto) {
        logger.info("Mise à jour de l'événement de sécurité: {}", id);
        // Create new instance with updated ID instead of using setter
        SeverityLevel severity = null;
        if (eventDto.severity() != null && !eventDto.severity().isEmpty()) {
            severity = SeverityLevel.of(eventDto.severity());
        }
        
        LocalDateTime timestamp = null;
        if (eventDto.timestamp() != null && !eventDto.timestamp().isEmpty()) {
            timestamp = LocalDateTime.parse(eventDto.timestamp(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        
        SecurityEvent updatedEvent = SecurityEventFactory.reconstruct(
            id,
            eventDto.eventType(),
            eventDto.userId(),
            eventDto.description(),
            severity,
            timestamp,
            eventDto.sourceIp()
        );
        SecurityEvent savedEvent = auditRepositoryPort.saveSecurityEvent(updatedEvent);
        return toSecurityEventDto(savedEvent);
    }
    
    @Override
    public void resolveSecurityEvent(String id, String resolutionNotes) {
        logger.info("Résolution de l'événement de sécurité: {}", id);
        // Logique de résolution à implémenter
    }
    
    // ==================== ANALYTIQUES ====================
    
    @Override
    public Map<String, Object> getAuditStatistics() {
        logger.info("Récupération des statistiques d'audit");
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLogs", auditLogJpaRepository.count());
        stats.put("recentLogs", auditLogJpaRepository.findRecentAuditLogs(LocalDateTime.now().minusDays(7)).size());
        return stats;
    }
    
    @Override
    public Map<String, Object> getSecurityStatistics() {
        logger.info("Récupération des statistiques de sécurité");
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEvents", securityEventJpaRepository.count());
        stats.put("criticalEvents", securityEventJpaRepository.countCriticalEventsSince(LocalDateTime.now().minusDays(7)));
        return stats;
    }
    
    @Override
    public List<Map<String, Object>> getSecurityTrends() {
        logger.info("Récupération des tendances de sécurité");
        // Implémentation à compléter
        return new ArrayList<>();
    }
    
    // ==================== HISTORIQUE ====================
    
    @Override
    public List<AuditLogDto> getEntityHistory(String entityType, String entityId) {
        logger.info("Récupération de l'historique de l'entité: {} - {}", entityType, entityId);
        return auditLogJpaRepository.findEntityHistory(entityType, entityId).stream()
            .map(this::toAuditLogDtoFromEntity)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<SecurityEventDto> getPersonSecurityHistory(String personId) {
        logger.info("Récupération de l'historique de sécurité de la personne: {}", personId);
        return securityEventJpaRepository.findPersonSecurityHistory(personId).stream()
            .map(this::toSecurityEventDtoFromEntity)
            .collect(Collectors.toList());
    }
    
    // ==================== MAPPERS ====================
    
    private AuditLogDto toAuditLogDto(AuditLog auditLog) {
        return new AuditLogDto(
            auditLog.getId(),
            auditLog.getUserId(),
            auditLog.getAction(),
            auditLog.getDescription(),
            auditLog.getTimestamp() != null ? auditLog.getTimestamp().toString() : null,
            auditLog.getIpAddress(),
            auditLog.getUserAgent()
        );
    }
    
    private AuditLog toAuditLog(AuditLogDto dto) {
        LocalDateTime timestamp = null;
        if (dto.timestamp() != null && !dto.timestamp().isEmpty()) {
            timestamp = LocalDateTime.parse(dto.timestamp(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        return AuditLogFactory.reconstruct(
            dto.id(),
            dto.userId(),
            dto.action(),
            dto.description(),
            timestamp,
            dto.ipAddress(),
            dto.userAgent()
        );
    }
    
    private SecurityEventDto toSecurityEventDto(SecurityEvent securityEvent) {
        return new SecurityEventDto(
            securityEvent.getId(),
            securityEvent.getEventType(),
            securityEvent.getUserId(),
            securityEvent.getDescription(),
            securityEvent.getSeverity() != null ? securityEvent.getSeverity().value() : null,
            securityEvent.getTimestamp() != null ? securityEvent.getTimestamp().toString() : null,
            securityEvent.getSourceIp()
        );
    }
    
    private SecurityEvent toSecurityEvent(SecurityEventDto dto) {
        SeverityLevel severity = null;
        if (dto.severity() != null && !dto.severity().isEmpty()) {
            severity = SeverityLevel.of(dto.severity());
        }
        
        LocalDateTime timestamp = null;
        if (dto.timestamp() != null && !dto.timestamp().isEmpty()) {
            timestamp = LocalDateTime.parse(dto.timestamp(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        
        return SecurityEventFactory.reconstruct(
            dto.id(),
            dto.eventType(),
            dto.userId(),
            dto.description(),
            severity,
            timestamp,
            dto.sourceIp()
        );
    }
    
    // Mappers pour conversion directe depuis les entités JPA existantes
    private AuditLogDto toAuditLogDtoFromEntity(AuditLogJpaEntity entity) {
        return new AuditLogDto(
            entity.getId(),
            entity.getActorId(),
            entity.getActionType(),
            entity.getActionDescription(),
            entity.getActionTimestamp() != null ? entity.getActionTimestamp().toString() : null,
            entity.getIpAddress(),
            entity.getUserAgent()
        );
    }
    
    private SecurityEventDto toSecurityEventDtoFromEntity(SecurityEventJpaEntity entity) {
        // entity.getSeverity() is already a String from JPA entity, so use it directly
        return new SecurityEventDto(
            entity.getId(),
            entity.getEventType(),
            entity.getPersonId(),
            entity.getEventDescription(),
            entity.getSeverity(),
            entity.getEventTimestamp() != null ? entity.getEventTimestamp().toString() : null,
            entity.getLocation()
        );
    }
}