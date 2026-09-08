package ecole_221.sn.ecole.modules.parent.application.port.dto;

/**
 * DTO pour demander un rendez-vous.
 */
public record AppointmentRequestDto(
    String professeurId,
    String dateSouhaitee,
    String motif
) {
    public AppointmentRequestDto {
        // Validation can be added here
    }
}
