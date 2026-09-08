package ecole_221.sn.ecole.modules.professor.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO représentant une note d'étudiant (pour un cours spécifique).
 * Standardisé avec le contrat BACKEND_DATA_CONTRACT.md (§6.1)
 */
@Schema(description = "Note d'un étudiant dans un cours")
public record GradeDto(
    @Schema(description = "Identifiant de la note")
    String id,
    
    @Schema(description = "Identifiant de l'étudiant")
    String studentId,
    
    @Schema(description = "Nom de l'étudiant")
    String nom,
    
    @Schema(description = "Prénom de l'étudiant")
    String prenom,
    
    @Schema(description = "Note de contrôle continu")
    Double cc,
    
    @Schema(description = "Note d'examen")
    Double examen,
    
    @Schema(description = "Note finale calculée")
    Double finalGrade,
    
    @Schema(description = "Mention")
    String mention
) {
    public GradeDto {
        // Validation can be added here
    }
    
    /**
     * Calcule la note finale (40% CC + 60% Examen).
     */
    public static double calculateFinalGrade(Double cc, Double examen) {
        double ccVal = cc != null ? cc : 0;
        double examVal = examen != null ? examen : 0;
        return (ccVal * 0.4) + (examVal * 0.6);
    }
    
    /**
     * Calcule la mention à partir de la note finale.
     */
    public static String calculateMention(double finalGrade) {
        if (finalGrade >= 16) return "A";
        if (finalGrade >= 14) return "B";
        if (finalGrade >= 12) return "C";
        if (finalGrade >= 10) return "D";
        return "F";
    }
}
