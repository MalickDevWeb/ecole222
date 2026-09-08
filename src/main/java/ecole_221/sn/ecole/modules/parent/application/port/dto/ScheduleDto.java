package ecole_221.sn.ecole.modules.parent.application.port.dto;

/**
 * DTO représentant un créneau dans l'emploi du temps.
 */
public record ScheduleDto(
    String id,
    String jour,  // "Lundi", "Mardi", etc.
    String heure,  // "08h00 - 10h00"
    String cours,
    String professeur,
    String salle,
    String type,  // "CM", "TD", "TP"
    boolean enCours
) {
    public ScheduleDto {
        // Validation can be added here
    }
}
