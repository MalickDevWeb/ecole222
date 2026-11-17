package ecole_221.sn.ecole.modules.parent.application.port.dto;

import java.util.List;

/**
 * DTO représentant les paiements.
 * APP-BE-18 : {@code statut} indique si les données proviennent d'une source réelle
 * ({@link #STATUT_DISPONIBLE}) ou si aucune source n'existe encore ({@link #STATUT_NON_DISPONIBLE}).
 * Le backend ne renvoie jamais de transaction fictive.
 */
public record PaymentDto(
    long totalAnnuel,
    List<PaymentInstallmentDto> echeances,
    String statut,
    /** Situation de CHAQUE enfant (le parent voit et règle enfant par enfant) ; vide sans dossier. */
    List<EnfantPaiementDto> enfants
) {
    /** Compatibilité : réponse sans détail par enfant. */
    public PaymentDto(long totalAnnuel, List<PaymentInstallmentDto> echeances, String statut) {
        this(totalAnnuel, echeances, statut, List.of());
    }

    public static final String STATUT_DISPONIBLE = "disponible";
    public static final String STATUT_NON_DISPONIBLE = "non_disponible";

    /** Réponse honnête lorsqu'aucune source de paiements réelle n'existe. */
    public static PaymentDto nonDisponible() {
        return new PaymentDto(0L, List.of(), STATUT_NON_DISPONIBLE);
    }

    /** Situation d'un enfant : montants réels du dossier financier, et ses lignes (versements, reste à payer). */
    public record EnfantPaiementDto(
        String id,
        String nom,
        String matricule,
        String classe,
        long totalDu,
        long paye,
        long resteDu,
        List<PaymentInstallmentDto> lignes
    ) {}

    public record PaymentInstallmentDto(
        String id,
        String label,
        long montant,
        String echeance,
        String statut,  // "PAYE", "EN_COURS", "RETARD"
        String datePaiement
    ) {}
}
