package ecole_221.sn.ecole.modules.student.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * DTO représentant le tableau de bord d'un étudiant.
 */
@Schema(description = "Tableau de bord étudiant")
public record StudentDashboardDto(
    @Schema(description = "Nombre total de crédits obtenus", example = "60")
    int totalCredits,
    
    @Schema(description = "Moyenne générale", example = "14.5")
    double moyenneGenerale,
    
    @Schema(description = "Nombre de cours en cours", example = "5")
    int coursEnCours,
    
    @Schema(description = "Nombre de devoirs à rendre", example = "3")
    int devoirsARendre,
    
    @Schema(description = "Prochains examens")
    List<ExamInfoDto> prochainsExamens,
    
    @Schema(description = "Statistiques de présence")
    AttendanceStatsDto statsPresence,
    
    @Schema(description = "Notifications récentes")
    List<NotificationDto> notifications
) {
    public record ExamInfoDto(
        @Schema(description = "Identifiant de l'examen")
        String id,
        
        @Schema(description = "Titre du cours", example = "Architecture des Microservices")
        String titre,
        
        @Schema(description = "Date de l'examen")
        String date,
        
        @Schema(description = "Salle")
        String salle
    ) {}
    
    public record AttendanceStatsDto(
        @Schema(description = "Taux de présence en pourcentage", example = "95.0")
        double tauxPresence,
        
        @Schema(description = "Nombre de présences")
        int present,
        
        @Schema(description = "Nombre d'absences")
        int absent,
        
        @Schema(description = "Nombre de retards")
        int retards
    ) {}
    
    public record NotificationDto(
        @Schema(description = "Identifiant de la notification")
        String id,
        
        @Schema(description = "Titre", example = "Nouveau devoir")
        String titre,
        
        @Schema(description = "Message")
        String message,
        
        @Schema(description = "Date de création")
        String date,
        
        @Schema(description = "Lu ou non")
        boolean lu
    ) {}
}
