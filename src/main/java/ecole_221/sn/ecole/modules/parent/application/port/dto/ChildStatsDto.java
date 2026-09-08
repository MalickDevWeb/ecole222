package ecole_221.sn.ecole.modules.parent.application.port.dto;

import java.util.List;

/**
 * DTO représentant les statistiques d'un enfant.
 */
public record ChildStatsDto(
    double tauxPresence,
    int nombreAbsences,
    int nombreRetards,
    double moyenneGenerale,
    int creditValides,
    int devoirsEnRetard,
    List<ExamDto> prochainsExamens,
    List<CourseDto> coursAujourdhui
) {
    public record ExamDto(String titre, String date, String salle) {}
    
    public record CourseDto(
        String id,
        String jour,
        String heure,
        String cours,
        String professeur,
        String salle,
        String type,
        boolean enCours
    ) {}
}
