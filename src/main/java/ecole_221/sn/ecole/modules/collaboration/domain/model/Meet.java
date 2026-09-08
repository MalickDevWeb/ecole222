package ecole_221.sn.ecole.modules.collaboration.domain.model;

import ecole_221.sn.ecole.modules.collaboration.domain.valueobject.MeetStatus;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Agrégat représentant une visioconférence/réunion.
 * Constructeur privé pour garantir l'invariance - utiliser MeetFactory.
 */
public class Meet {
    
    private final String id;
    private String title;
    private String description;
    private final String organizerId;
    private String meetingLink;
    private LocalDateTime scheduledTime;
    private MeetStatus status;
    private final Set<String> participantIds;
    private final LocalDateTime createdAt;
    private LocalDateTime endedAt;
    private int maxParticipants;
    
    // Constructeur package-private pour permettre aux Factories d'y accéder
    Meet(String id, String title, String description, String organizerId, 
                String meetingLink, LocalDateTime scheduledTime, MeetStatus status,
                Set<String> participantIds, LocalDateTime createdAt, LocalDateTime endedAt, 
                int maxParticipants) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.title = Objects.requireNonNull(title, "Le titre ne peut pas être null");
        this.description = description;
        this.organizerId = Objects.requireNonNull(organizerId, "L'ID de l'organisateur ne peut pas être null");
        this.meetingLink = meetingLink;
        this.scheduledTime = scheduledTime;
        this.status = Objects.requireNonNull(status, "Le statut ne peut pas être null");
        this.participantIds = new HashSet<>(Objects.requireNonNull(participantIds, "Les participants ne peuvent pas être null"));
        this.createdAt = Objects.requireNonNull(createdAt, "La date de création ne peut pas être null");
        this.endedAt = endedAt;
        this.maxParticipants = maxParticipants;
    }
    
    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getOrganizerId() { return organizerId; }
    public String getMeetingLink() { return meetingLink; }
    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public MeetStatus getStatus() { return status; }
    public Set<String> getParticipantIds() { return new HashSet<>(participantIds); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public int getMaxParticipants() { return maxParticipants; }
    public int getParticipantCount() { return participantIds.size(); }
    
    // Comportements métier
    public void start() {
        if (status.isActive()) {
            throw new IllegalStateException("La réunion est déjà active");
        }
        this.status = MeetStatus.ACTIVE;
        this.scheduledTime = LocalDateTime.now();
    }
    
    public void end() {
        if (!status.isActive()) {
            throw new IllegalStateException("La réunion n'est pas active");
        }
        this.status = MeetStatus.ENDED;
        this.endedAt = LocalDateTime.now();
    }
    
    public void cancel() {
        if (status.isEnded()) {
            throw new IllegalStateException("La réunion est déjà terminée");
        }
        this.status = MeetStatus.CANCELLED;
    }
    
    public void addParticipant(String participantId) {
        Objects.requireNonNull(participantId, "L'ID du participant ne peut pas être null");
        
        if (maxParticipants > 0 && participantIds.size() >= maxParticipants) {
            throw new IllegalStateException("La réunion est pleine");
        }
        
        participantIds.add(participantId);
    }
    
    public void removeParticipant(String participantId) {
        participantIds.remove(participantId);
    }
    
    public void updateMeetingLink(String newLink) {
        this.meetingLink = newLink;
    }
    
    public void reschedule(LocalDateTime newTime) {
        if (status.isActive()) {
            throw new IllegalStateException("Impossible de reprogrammer une réunion active");
        }
        this.scheduledTime = Objects.requireNonNull(newTime, "La nouvelle date ne peut pas être null");
    }
    
    public boolean isOrganizer(String userId) {
        return organizerId.equals(userId);
    }
    
    public boolean isParticipant(String userId) {
        return participantIds.contains(userId);
    }
    
    public boolean hasCapacity() {
        return maxParticipants <= 0 || participantIds.size() < maxParticipants;
    }
    
    public boolean isUpcoming() {
        return status.isScheduled() && scheduledTime != null && scheduledTime.isAfter(LocalDateTime.now());
    }
    
    public boolean isOver() {
        return status.isEnded() || (endedAt != null && endedAt.isBefore(LocalDateTime.now()));
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Meet meet = (Meet) o;
        return Objects.equals(id, meet.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Meet{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", organizerId='" + organizerId + '\'' +
                ", status=" + status +
                ", participantCount=" + participantIds.size() +
                '}';
    }
}