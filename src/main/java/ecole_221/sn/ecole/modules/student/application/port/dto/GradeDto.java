package ecole_221.sn.ecole.modules.student.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO représentant une note étudiante.
 */
@Schema(description = "Note d'un étudiant pour un module")
public record GradeDto(
    @Schema(description = "Identifiant de la note", example = "g-1")
    String id,
    
    @Schema(description = "Nom du module", example = "Architecture Hexagonale")
    String module,
    
    @Schema(description = "Nom du professeur", example = "Dr. Aly Diatta")
    String prof,
    
    @Schema(description = "Nombre de crédits ECTS", example = "6")
    int ects,
    
    @Schema(description = "Note de contrôle continu", example = "16.5")
    double cc,
    
    @Schema(description = "Note d'examen", example = "18.0")
    double examen,
    
    @Schema(description = "Moyenne de la promotion", example = "13.5")
    double moyPromo
) {
    public GradeDto {
        // Validation can be added here
    }
    
    /**
     * Calcule la note finale (40% CC + 60% Examen).
     */
    public double getFinalGrade() {
        return (cc * 0.4) + (examen * 0.6);
    }
    
    /**
     * Vérifie si la note est validée (moyenne >= 10).
     */
    public boolean isValidated() {
        return getFinalGrade() >= 10.0;
    }
    
    /**
     * Calcule la mention (A, B, C, D, F).
     */
    public String getMention() {
        double finalGrade = getFinalGrade();
        if (finalGrade >= 16) return "A";
        if (finalGrade >= 14) return "B";
        if (finalGrade >= 12) return "C";
        if (finalGrade >= 10) return "D";
        return "F";
    }
}
