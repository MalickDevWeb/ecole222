package ecole_221.sn.ecole.modules.audit.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "security_events")
public class SecurityEventJpaEntity {

    @Id
    private String id;
    
    @Column(name = "event_type")
    private String eventType; // 'badge_scan', 'qr_scan', 'access_denied', 'suspicious_activity', 'emergency'
    
    @Column(name = "event_category")
    private String eventCategory; // 'access_control', 'surveillance', 'emergency', 'policy_violation'
    
    @Column(name = "severity")
    private String severity; // 'low', 'medium', 'high', 'critical'
    
    @Column(name = "checkpoint_id")
    private String checkpointId; // ID du point de contrôle
    
    @Column(name = "checkpoint_name")
    private String checkpointName; // 'Portail Principal', 'Entrée B', etc.
    
    @Column(name = "person_id")
    private String personId; // ID de la personne (étudiant, professeur, vigile)
    
    @Column(name = "person_name")
    private String personName;
    
    @Column(name = "person_type")
    private String personType; // 'student', 'professor', 'vigil', 'visitor', 'unknown'
    
    @Column(name = "badge_id")
    private String badgeId;
    
    @Column(name = "access_status")
    private String accessStatus; // 'granted', 'denied', 'suspended', 'expired'
    
    @Column(name = "denial_reason")
    private String denialReason; // 'suspended_qr', 'invalid_badge', 'payment_overdue', etc.
    
    @Column(name = "event_description")
    private String eventDescription;
    
    @Column(name = "location")
    private String location; // Coordonnées ou description du lieu
    
    @Column(name = "camera_recording")
    private Boolean cameraRecording;
    
    @Column(name = "camera_reference")
    private String cameraReference; // Référence de l'enregistrement vidéo
    
    @Column(name = "requires_investigation")
    private Boolean requiresInvestigation;
    
    @Column(name = "investigation_status")
    private String investigationStatus; // 'pending', 'in_progress', 'resolved', 'closed'
    
    @Column(name = "resolved_by")
    private String resolvedBy; // ID de l'admin qui a résolu l'événement
    
    @Column(name = "resolution_notes")
    private String resolutionNotes;
    
    @Column(name = "additional_data")
    private String additionalData; // JSON pour données supplémentaires
    
    @Column(name = "event_timestamp")
    private LocalDateTime eventTimestamp;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public SecurityEventJpaEntity() {
        this.createdAt = LocalDateTime.now();
        this.eventTimestamp = LocalDateTime.now();
        this.cameraRecording = false;
        this.requiresInvestigation = false;
        this.investigationStatus = "pending";
    }

    public SecurityEventJpaEntity(String id, String eventType, String eventCategory, String severity,
                                   String checkpointId, String checkpointName, String personId, 
                                   String personName, String personType, String accessStatus) {
        this();
        this.id = id;
        this.eventType = eventType;
        this.eventCategory = eventCategory;
        this.severity = severity;
        this.checkpointId = checkpointId;
        this.checkpointName = checkpointName;
        this.personId = personId;
        this.personName = personName;
        this.personType = personType;
        this.accessStatus = accessStatus;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public String getEventCategory() { return eventCategory; }
    public void setEventCategory(String eventCategory) { this.eventCategory = eventCategory; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getCheckpointId() { return checkpointId; }
    public void setCheckpointId(String checkpointId) { this.checkpointId = checkpointId; }
    
    public String getCheckpointName() { return checkpointName; }
    public void setCheckpointName(String checkpointName) { this.checkpointName = checkpointName; }
    
    public String getPersonId() { return personId; }
    public void setPersonId(String personId) { this.personId = personId; }
    
    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }
    
    public String getPersonType() { return personType; }
    public void setPersonType(String personType) { this.personType = personType; }
    
    public String getBadgeId() { return badgeId; }
    public void setBadgeId(String badgeId) { this.badgeId = badgeId; }
    
    public String getAccessStatus() { return accessStatus; }
    public void setAccessStatus(String accessStatus) { this.accessStatus = accessStatus; }
    
    public String getDenialReason() { return denialReason; }
    public void setDenialReason(String denialReason) { this.denialReason = denialReason; }
    
    public String getEventDescription() { return eventDescription; }
    public void setEventDescription(String eventDescription) { this.eventDescription = eventDescription; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public Boolean getCameraRecording() { return cameraRecording; }
    public void setCameraRecording(Boolean cameraRecording) { this.cameraRecording = cameraRecording; }
    
    public String getCameraReference() { return cameraReference; }
    public void setCameraReference(String cameraReference) { this.cameraReference = cameraReference; }
    
    public Boolean getRequiresInvestigation() { return requiresInvestigation; }
    public void setRequiresInvestigation(Boolean requiresInvestigation) { this.requiresInvestigation = requiresInvestigation; }
    
    public String getInvestigationStatus() { return investigationStatus; }
    public void setInvestigationStatus(String investigationStatus) { this.investigationStatus = investigationStatus; }
    
    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
    
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    
    public String getAdditionalData() { return additionalData; }
    public void setAdditionalData(String additionalData) { this.additionalData = additionalData; }
    
    public LocalDateTime getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(LocalDateTime eventTimestamp) { this.eventTimestamp = eventTimestamp; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}