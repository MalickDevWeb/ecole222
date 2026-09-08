package ecole_221.sn.ecole.modules.collaboration.domain.port.repository;

import ecole_221.sn.ecole.modules.collaboration.domain.model.Meet;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MeetRepositoryPort {
    Meet save(Meet meet);
    Optional<Meet> findById(String id);
    List<Meet> findByOrganizerId(String organizerId);
    List<Meet> findByParticipantId(String participantId);
    List<Meet> findActiveMeets();
    List<Meet> findUpcomingMeets(LocalDateTime before);
    List<Meet> findScheduledMeetsBetween(LocalDateTime start, LocalDateTime end);
    void deleteById(String id);
}