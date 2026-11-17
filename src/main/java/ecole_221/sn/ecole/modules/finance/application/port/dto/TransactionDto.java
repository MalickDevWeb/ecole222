package ecole_221.sn.ecole.modules.finance.application.port.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO représentant une transaction financière.
 */
@Schema(description = "Transaction financière")
public record TransactionDto(
    @Schema(description = "Identifiant de la transaction", example = "txn-001")
    String id,
    
    @Schema(description = "Identifiant de l'étudiant", example = "std-001")
    String studentId,
    
    @Schema(description = "Type de transaction", example = "PAYMENT")
    String type,
    
    @Schema(description = "Montant", example = "50000")
    String amount,
    
    @Schema(description = "Devise : toujours FCFA, unité unique des montants enregistrés (colonne amount, aucune autre devise "
            + "n'est stockée)", example = "FCFA")
    String currency,
    
    @Schema(description = "Statut : toujours COMPLETED — aucun statut n'est stocké, chaque ligne enregistrée est un mouvement "
            + "déjà effectué (aucune transaction « en attente » n'existe)", example = "COMPLETED")
    String status,
    
    @Schema(description = "Date de la transaction", example = "2024-07-28T15:30:00")
    String transactionDate,

    // Écran Encaissements et reçus : champs enregistrés par le serveur.
    // Ignorés en entrée (POST /finance/transactions) ; null quand la donnée n'existe pas (jamais de valeur inventée).
    @Schema(description = "Moyen de paiement (Espèces, Wave, Orange Money…)", example = "Wave")
    String paymentMethod,

    @Schema(description = "Référence du paiement saisie", example = "WV-20261115-0042")
    String reference,

    @Schema(description = "Nom de l'élève")
    String studentName,

    @Schema(description = "Classe de l'élève (dossier financier)")
    String classe,

    @Schema(description = "Identifiant du dossier financier de l'élève")
    String dossierId,

    @Schema(description = "Prénom et nom de l'auteur de la saisie (null pour les versements saisis avant le 15/11)")
    String saisiPar,

    @Schema(description = "Date et heure de la saisie", example = "2026-11-15T10:42:00")
    String saisiLe
) {
    public TransactionDto {
        // Validation can be added here
    }
}