package ecole_221.sn.ecole.modules.collaboration.domain.model;

import ecole_221.sn.ecole.modules.collaboration.domain.valueobject.MeetStatus;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MeetFactory {
    
    public static Meet create(String title, String organizerId, LocalDateTime scheduledTime) {
        String id = UUID.randomUUID().toString();
        LocalDateTime createdAt = LocalDateTime.now();
        Set<String> participants = new HashSet<>();
        participants.add(organizerId); // L'organisateur est automatiquement participant
        
        return new Meet(id, title, null, organizerId, null, scheduledTime, 
                      MeetStatus.SCHEDULED, participants, createdAt, null, 0);
    }
    
    public static Meet createWithLink(String title, String organizerId, String meetingLink, 
                                      LocalDateTime scheduledTime) {
        String id = UUID.randomUUID().toString();
        LocalDateTime createdAt = LocalDateTime.now();
        Set<String> participants = new HashSet<>();
        participants.add(organizerId);
        
        return new Meet(id, title, null, organizerId, meetingLink, scheduledTime, 
                      MeetStatus.SCHEDULED, participants, createdAt, null, 0);
    }
    
    public static Meet createWithCapacity(String title, String organizerId, LocalDateTime scheduledTime, 
                                         int maxParticipants) {
        String id = UUID.randomUUID().toString();
        LocalDateTime createdAt = LocalDateTime.now();
        Set<String> participants = new HashSet<>();
        participants.add(organizerId);
        
        return new Meet(id, title, null, organizerId, null, scheduledTime, 
                      MeetStatus.SCHEDULED, participants, createdAt, null, maxParticipants);
    }
    
    public static Meet createInstant(String title, String organizerId) {
        String id = UUID.randomUUID().toString();
        LocalDateTime createdAt = LocalDateTime.now();
        Set<String> participants = new HashSet<>();
        participants.add(organizerId);
        
        Meet meet = new Meet(id, title, null, organizerId, null, LocalDateTime.now(), 
                           MeetStatus.ACTIVE, participants, createdAt, null, 0);
        return meet;
    }
    
    public static Meet reconstitute(String id, String title, String description, String organizerId,
                                   String meetingLink, LocalDateTime scheduledTime, String status,
                                   Set<String> participantIds, LocalDateTime createdAt, 
                                   LocalDateTime endedAt, int maxParticipants) {
        return new Meet(id, title, description, organizerId, meetingLink, scheduledTime, 
                      new MeetStatus(status), participantIds, createdAt, endedAt, maxParticipants);
    }
}