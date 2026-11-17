package ecole_221.sn.ecole.modules.parent.application.port.dto;

/**
 * DTO représentant une note.
 */
public record GradeDto(
    String id,
    String module,
    String professeur,
    Double cc,
    Double examen,
    Double moyenne,
    String mention,  // "Excellent", "Bien", "Assez Bien", "Passable", "Insuffisant"
    Integer ects
) {
    public GradeDto {
        // Validation can be added here
    }
}
