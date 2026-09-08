package ecole_221.sn.ecole.modules.professor.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO représentant un étudiant (pour la liste des étudiants d'un cours).
 */
@Schema(description = "Étudiant inscrit dans un cours")
public record StudentDto(
    @Schema(description = "Identifiant de l'étudiant", example = "usr-001")
    String id,
    
    @Schema(description = "Matricule", example = "SN-2026-1234")
    String matricule,
    
    @Schema(description = "Nom de famille", example = "Sall")
    String lastName,
    
    @Schema(description = "Prénom", example = "Mamadou")
    String firstName,
    
    @Schema(description = "Email", example = "etudiant@ecole.sn")
    String email,
    
    @Schema(description = "Statut de présence")
    String attendanceStatus,
    
    @Schema(description = "Moyenne dans le cours")
    Double averageGrade
) {
    public StudentDto {
        // Validation can be added here
    }
    
    /**
     * Retourne le nom complet.
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
