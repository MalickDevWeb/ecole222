package ecole_221.sn.ecole.modules.parent.application.port.dto;

/**
 * DTO pour envoyer un message.
 */
public record SendMessageRequestDto(
    String destinataireId,
    String sujet,
    String message
) {
    public SendMessageRequestDto {
        // Validation can be added here
    }
}
