package ecole_221.sn.ecole.modules.parent.application.port.usecase;

import ecole_221.sn.ecole.modules.parent.application.port.dto.*;
import java.util.List;

/**
 * Interface des cas d'utilisation pour le module Parent.
 */
public interface ParentUseCase {
    // Enfants
    List<ChildDto> getEnfantsByParentId(String parentId);
    
    // Stats d'un enfant
    ChildStatsDto getChildStats(String enfantId);
    
    // Présences
    List<PresenceDto> getChildPresences(String enfantId, String mois);
    
    // Notes
    List<GradeDto> getChildGrades(String enfantId);
    
    // Planning
    List<ScheduleDto> getChildSchedule(String enfantId);
    
    // Messages
    List<MessageDto> getParentMessages(String parentId);
    MessageDto sendMessage(String parentId, SendMessageRequestDto request);
    MessageDto markMessageAsRead(String messageId, String parentId);
    
    // Candidatures
    List<ApplicationDto> getParentApplications(String parentId);
    String enrollChild(String parentId, EnrollChildPayloadDto payload);
    
    // Paiements
    PaymentDto getParentPayments(String parentId);
    
    // Profil
    ParentProfileDto updateParentProfile(String parentId, ParentProfileDto profileDto);
    
    // Rendez-vous
    String requestAppointment(String parentId, AppointmentRequestDto request);
    
    // Justificatifs
    String justifyAbsence(String presenceId, String parentId, String motif, String fileUrl);
}
