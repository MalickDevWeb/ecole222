package ecole_221.sn.ecole.modules.finance.infrastructure.persistence;

import ecole_221.sn.ecole.modules.finance.domain.model.Transaction;
import ecole_221.sn.ecole.modules.finance.domain.port.repository.FinanceRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptateur de repository pour le module Finance.
 * Implémente FinanceRepositoryPort en utilisant les repositories JPA existants.
 */
@Component
public class FinanceRepositoryAdapter implements FinanceRepositoryPort {
    
    private static final Logger logger = LoggerFactory.getLogger(FinanceRepositoryAdapter.class);
    
    private final TransactionJpaRepository transactionJpaRepository;
    
    public FinanceRepositoryAdapter(TransactionJpaRepository transactionJpaRepository) {
        this.transactionJpaRepository = transactionJpaRepository;
    }
    
    @Override
    public List<Transaction> findAllTransactions() {
        logger.debug("Récupération de toutes les transactions depuis JPA");
        return transactionJpaRepository.findAll().stream()
            .map(this::toTransaction)
            .collect(Collectors.toList());
    }
    
    @Override
    public Optional<Transaction> findTransactionById(String id) {
        logger.debug("Récupération de la transaction: {}", id);
        return transactionJpaRepository.findById(id)
            .map(this::toTransaction);
    }
    
    @Override
    public Transaction saveTransaction(Transaction transaction) {
        logger.debug("Sauvegarde de la transaction: {}", transaction.getId());
        TransactionJpaEntity entity = toTransactionEntity(transaction);
        TransactionJpaEntity savedEntity = transactionJpaRepository.save(entity);
        return toTransaction(savedEntity);
    }
    
    @Override
    public void deleteTransaction(String id) {
        logger.debug("Suppression de la transaction: {}", id);
        transactionJpaRepository.deleteById(id);
    }
    
    @Override
    public List<Transaction> findTransactionsByStudentId(String studentId) {
        logger.debug("Récupération des transactions par étudiant: {}", studentId);
        return transactionJpaRepository.findByStudentId(studentId).stream()
            .map(this::toTransaction)
            .collect(Collectors.toList());
    }
    
    // ==================== MAPPERS ENTITIES VERS DOMAIN ====================
    
    private Transaction toTransaction(TransactionJpaEntity entity) {
        Transaction transaction = new Transaction();
        transaction.setId(entity.getId());
        transaction.setStudentId(entity.getStudentId());
        transaction.setType(entity.getTransactionType());
        transaction.setAmount(entity.getAmount() != null ? entity.getAmount().toString() : "0");
        transaction.setCurrency("FCFA"); // Devise par défaut
        transaction.setStatus("COMPLETED"); // Statut par défaut
        transaction.setTransactionDate(entity.getTransactionDate() != null ? entity.getTransactionDate().toString() : null);
        return transaction;
    }
    
    private TransactionJpaEntity toTransactionEntity(Transaction transaction) {
        TransactionJpaEntity entity = new TransactionJpaEntity();
        entity.setId(transaction.getId());
        entity.setStudentId(transaction.getStudentId());
        entity.setTransactionType(transaction.getType());
        try {
            entity.setAmount(Long.parseLong(transaction.getAmount()));
        } catch (NumberFormatException e) {
            entity.setAmount(0L);
        }
        if (transaction.getTransactionDate() != null) {
            entity.setTransactionDate(java.time.LocalDateTime.parse(transaction.getTransactionDate()));
        }
        return entity;
    }
}