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
    
    @Schema(description = "Devise", example = "FCFA")
    String currency,
    
    @Schema(description = "Statut", example = "COMPLETED")
    String status,
    
    @Schema(description = "Date de la transaction", example = "2024-07-28T15:30:00")
    String transactionDate
) {
    public TransactionDto {
        // Validation can be added here
    }
}