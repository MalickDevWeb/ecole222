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
    Map<String, Integer> reactions
) {
    public LiveSessionDto {
        // Validation can be added here
    }
}
