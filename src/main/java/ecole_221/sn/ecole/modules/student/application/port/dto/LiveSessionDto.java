package ecole_221.sn.ecole.modules.student.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO représentant une session live de cours.
 */
@Schema(description = "Session live de cours")
public record LiveSessionDto(
    @Schema(description = "Identifiant de la session", example = "sess-1")
    String id,
    
    @Schema(description = "Titre du cours", example = "Architecture Hexagonale")
    String courseTitle,
    
    @Schema(description = "Nom du professeur", example = "Dr. Aly Diatta")
    String professorName,
    
    @Schema(description = "Statut de la session", example = "ACTIVE")
    String status,
    
    @Schema(description = "Nombre d'utilisateurs actifs")
    int activeUsers,
    
    @Schema(description = "Lien de la session")
    String link,
    
    @Schema(description = "Heure de la session")
    String time,
    
    @Schema(description = "Réactions des étudiants")
    Map<String, Integer> reactions,

    @Schema(description = "Cours de la séance (2026-11-14 : le panneau du professeur la retrouve par son cours)")
    String courseName,

    @Schema(description = "Lien à ouvrir (même valeur que link, nom lu par les écrans de l'élève)")
    String hlsUrl
) {
    public LiveSessionDto {
        // Validation can be added here
    }
}
