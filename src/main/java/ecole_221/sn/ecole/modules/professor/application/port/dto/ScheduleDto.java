package ecole_221.sn.ecole.modules.professor.application.port.dto;

import ecole_221.sn.ecole.modules.professor.domain.valueobject.SessionStatus;
import ecole_221.sn.ecole.modules.professor.domain.valueobject.SessionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * DTO représentant une session planifiée dans l'emploi du temps.
 */
@Schema(description = "Session planifiée du professeur")
public record ScheduleDto(
    @Schema(description = "Identifiant de la session", example = "sched-001")
    String id,
    
    @Schema(description = "Titre du cours")
    String courseTitle,
    
    @Schema(description = "Jour de la semaine")
    String day,
    
    @Schema(description = "Créneau horaire")
    String time,
    
    @Schema(description = "Salle")
    String room,
    
    @Schema(description = "Identifiant du cours")
    String courseId,
    
    @Schema(description = "Type de session")
    SessionType type,
    
    @Schema(description = "Date de la session")
    LocalDate date,
    
    @Schema(description = "Statut de la session")
    SessionStatus status
) {
    public ScheduleDto {
        // Validation can be added here
    }
    
    /**
     * Vérifie si la session est dans le futur.
     */
    public boolean isUpcoming() {
        return status == SessionStatus.SCHEDULED || status == SessionStatus.RESCHEDULED;
    }
    
    /**
     * Vérifie si la session est annulée.
     */
    public boolean isCancelled() {
        return status == SessionStatus.CANCELLED;
    }
}
