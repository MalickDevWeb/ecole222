package ecole_221.sn.ecole.modules.parent.application.port.dto;

/**
 * DTO représentant un message.
 */
public record MessageDto(
    String id,
    String de,
    String role,
    String sujet,
    String message,
    String date,  // YYYY-MM-DD
    boolean lu,
    String avatar
) {
    public MessageDto {
        // Validation can be added here
    }
}
