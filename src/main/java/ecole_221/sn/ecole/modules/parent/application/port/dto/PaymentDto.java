package ecole_221.sn.ecole.modules.parent.application.port.dto;

import java.util.List;

/**
 * DTO représentant les paiements.
 */
public record PaymentDto(
    long totalAnnuel,
    List<PaymentInstallmentDto> echeances
) {
    public record PaymentInstallmentDto(
        String id,
        String label,
        long montant,
        String echeance,
        String statut,  // "PAYE", "EN_COURS", "RETARD"
        String datePaiement
    ) {}
}
