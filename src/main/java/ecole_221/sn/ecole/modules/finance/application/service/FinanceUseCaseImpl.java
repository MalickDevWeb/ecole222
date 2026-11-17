package ecole_221.sn.ecole.modules.finance.application.service;

import ecole_221.sn.ecole.modules.finance.application.port.dto.TransactionDto;
import ecole_221.sn.ecole.modules.finance.application.port.usecase.FinanceUseCase;
import ecole_221.sn.ecole.modules.finance.domain.model.Transaction;
import ecole_221.sn.ecole.modules.finance.domain.port.repository.FinanceRepositoryPort;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class FinanceUseCaseImpl implements FinanceUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(FinanceUseCaseImpl.class);
    
    private final FinanceRepositoryPort financeRepositoryPort;
    private final StudentFinanceJpaRepository studentFinanceRepository;
    private final ExpenseJpaRepository expenseRepository;
    private final TransactionJpaRepository transactionRepository;
    
    public FinanceUseCaseImpl(FinanceRepositoryPort financeRepositoryPort,
                              StudentFinanceJpaRepository studentFinanceRepository,
                              ExpenseJpaRepository expenseRepository,
                              TransactionJpaRepository transactionRepository) {
        this.financeRepositoryPort = financeRepositoryPort;
        this.studentFinanceRepository = studentFinanceRepository;
        this.expenseRepository = expenseRepository;
        this.transactionRepository = transactionRepository;
    }
    
    // ==================== GESTION DES FINANCES ÉTUDIANTS (compatibilité JPA) ====================
    
    @Override
    public List<StudentFinanceJpaEntity> getAllStudentFinances() {
        logger.info("Récupération de toutes les finances étudiantes");
        return studentFinanceRepository.findAll();
    }
    
    @Override
    public StudentFinanceJpaEntity getStudentFinanceById(String id) {
        return studentFinanceRepository.findById(id).orElse(null);
    }
    
    @Override
    public StudentFinanceJpaEntity getStudentFinanceByStudentId(String studentId) {
        // Dossier le plus récent (findByStudentId échoue si l'élève en a plusieurs)
        return studentFinanceRepository.findAllByStudentIdOrderByCreatedAtDesc(studentId).stream().findFirst().orElse(null);
    }
    
    @Override
    public List<StudentFinanceJpaEntity> getStudentFinancesByClasse(String classe) {
        return studentFinanceRepository.findByClasse(classe);
    }
    
    @Override
    public List<StudentFinanceJpaEntity> getStudentsWithDebt() {
        return studentFinanceRepository.findStudentsWithDebt();
    }
    
    @Override
    public StudentFinanceJpaEntity createStudentFinance(StudentFinanceJpaEntity studentFinance) {
        logger.info("Création finance étudiant: {}", studentFinance.getStudentName());
        // APP-TL-22 : statut jamais saisi à la main, recalculé à partir du dû et du payé
        studentFinance.setStatus(EcolagesService.statut(nul0(studentFinance.getTotalRequired()), nul0(studentFinance.getPaid())));
        return studentFinanceRepository.save(studentFinance);
    }
    
    @Override
    public StudentFinanceJpaEntity updateStudentFinance(String id, StudentFinanceJpaEntity studentFinance) {
        logger.info("Mise à jour finance étudiant: {}", id);
        // APP-QA-04 : fiche inexistante → 404 ; seuls les champs éditables, bornés
        StudentFinanceJpaEntity existante = studentFinanceRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Fiche financière introuvable."));
        if (studentFinance == null) {
            throw ecole_221.sn.ecole.partage.ChampsRequete.refus("Aucune donnée reçue.");
        }
        if (studentFinance.getStudentName() != null) {
            existante.setStudentName(texteBorne(studentFinance.getStudentName(), "Le nom de l'élève", 200));
        }
        if (studentFinance.getClasse() != null) {
            existante.setClasse(texteBorne(studentFinance.getClasse(), "La classe", 100));
        }
        if (studentFinance.getStatus() != null && !studentFinance.getStatus().strip().equals(existante.getStatus())) {
            // APP-TL-22 : le statut n'est pas saisissable
            throw ecole_221.sn.ecole.partage.ChampsRequete.refus(
                    "Le statut n'est pas modifiable : il est calculé à partir du montant dû et des versements enregistrés.");
        }
        existante.setDebt(montant(studentFinance.getDebt(), "La dette", existante.getDebt()));
        existante.setPaid(montant(studentFinance.getPaid(), "Le montant payé", existante.getPaid()));
        existante.setTotalRequired(montant(studentFinance.getTotalRequired(), "Le montant total", existante.getTotalRequired()));
        existante.setStatus(EcolagesService.statut(nul0(existante.getTotalRequired()), nul0(existante.getPaid())));
        return studentFinanceRepository.save(existante);
    }
    
    @Override
    public void deleteStudentFinance(String id) {
        logger.info("Suppression finance étudiant: {}", id);
        // APP-TL-05 : dossier inexistant ou d'une autre école → 404 ; un dossier qui porte des versements n'est pas
        // supprimé (409) : ce serait effacer l'historique comptable de l'élève.
        StudentFinanceJpaEntity d = studentFinanceRepository.findById(id)   // @TenantId : école courante
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Dossier financier introuvable dans cet établissement."));
        boolean versements = (d.getPaid() != null && d.getPaid() > 0) || transactionRepository.findByStudentId(d.getStudentId()).stream()
                .anyMatch(t -> id.equals(t.getStudentFinanceId()));
        if (versements) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                    "Ce dossier porte des versements enregistrés : il ne peut pas être supprimé (historique comptable).");
        }
        studentFinanceRepository.delete(d);
    }
    
    // ==================== GESTION DES DÉPENSES (compatibilité JPA) ====================
    
    @Override
    public List<ExpenseJpaEntity> getAllExpenses() {
        logger.info("Récupération de toutes les dépenses");
        return expenseRepository.findAll();
    }
    
    @Override
    public ExpenseJpaEntity getExpenseById(String id) {
        return expenseRepository.findById(id).orElse(null);
    }
    
    @Override
    public List<ExpenseJpaEntity> getExpensesByCategory(String category) {
        return expenseRepository.findByCategory(category);
    }
    
    @Override
    public List<ExpenseJpaEntity> getRecentExpenses() {
        return expenseRepository.findRecentExpenses(LocalDateTime.now().minusDays(30));
    }
    
    @Override
    public ExpenseJpaEntity createExpense(ExpenseJpaEntity expense) {
        logger.info("Création dépense: {}", expense.getLabel());
        return expenseRepository.save(expense);
    }
    
    @Override
    public ExpenseJpaEntity updateExpense(String id, ExpenseJpaEntity expense) {
        logger.info("Mise à jour dépense: {}", id);
        expense.setId(id);
        return expenseRepository.save(expense);
    }
    
    @Override
    public void deleteExpense(String id) {
        logger.info("Suppression dépense: {}", id);
        expenseRepository.deleteById(id);
    }
    
    // ==================== GESTION DES TRANSACTIONS ====================
    
    @Override
    public List<TransactionDto> getAllTransactions() {
        logger.info("Récupération de toutes les transactions");
        return financeRepositoryPort.findAllTransactions().stream()
            .map(this::toTransactionDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<TransactionDto> getTransactionsByStudentId(String studentId) {
        logger.info("Récupération des transactions par étudiant: {}", studentId);
        return financeRepositoryPort.findTransactionsByStudentId(studentId).stream()
            .map(this::toTransactionDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<TransactionDto> getRecentTransactions() {
        logger.info("Récupération des transactions récentes");
        return transactionRepository.findRecentTransactions(LocalDateTime.now().minusDays(30)).stream()
            .map(this::toTransactionDtoFromEntity)
            .collect(Collectors.toList());
    }
    
    @Override
    public TransactionDto createTransaction(TransactionDto transactionDto) {
        logger.info("Création transaction: {} {}", transactionDto.amount(), transactionDto.currency());
        Transaction transaction = toTransaction(transactionDto);
        Transaction savedTransaction = financeRepositoryPort.saveTransaction(transaction);
        
        // Mettre à jour la finance étudiante si c'est un paiement (compatibilité)
        if ("PAYMENT".equals(transactionDto.type()) && transactionDto.studentId() != null) {
            // Dossier le plus récent (findByStudentId échoue si l'élève en a plusieurs)
            StudentFinanceJpaEntity studentFinance = studentFinanceRepository.findAllByStudentIdOrderByCreatedAtDesc(transactionDto.studentId())
                    .stream().findFirst().orElse(null);
            if (studentFinance != null) {
                try {
                    long amount = Long.parseLong(transactionDto.amount());
                    studentFinance.setPaid(studentFinance.getPaid() + amount);
                    studentFinance.setDebt(Math.max(0, studentFinance.getDebt() - amount));
                    studentFinance.setLastPaymentDate(LocalDateTime.now());
                    
                    // APP-TL-22 : statut recalculé comme pour un encaissement (En Règle, En Avance ou Non soldé)
                    studentFinance.setStatus(EcolagesService.statut(nul0(studentFinance.getTotalRequired()), nul0(studentFinance.getPaid())));
                    
                    studentFinanceRepository.save(studentFinance);
                } catch (NumberFormatException e) {
                    logger.warn("Impossible de convertir le montant: {}", transactionDto.amount());
                }
            }
        }
        
        return toTransactionDto(savedTransaction);
    }
    
    // ==================== ANALYTIQUES (compatibilité JPA) ====================
    
    @Override
    public Map<String, Object> getFinanceDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        
        List<StudentFinanceJpaEntity> allStudents = studentFinanceRepository.findAll();
        long totalDebt = allStudents.stream()
            .mapToLong(sf -> sf.getDebt() != null ? sf.getDebt() : 0L)
            .sum();
        long totalPaid = allStudents.stream()
            .mapToLong(sf -> sf.getPaid() != null ? sf.getPaid() : 0L)
            .sum();
        long totalRequired = allStudents.stream()
            .mapToLong(sf -> sf.getTotalRequired() != null ? sf.getTotalRequired() : 0L)
            .sum();
        
        double recoveryRate = totalRequired > 0 ? (totalPaid * 100.0 / totalRequired) : 0;
        
        dashboard.put("totalDebt", totalDebt);
        dashboard.put("totalPaid", totalPaid);
        dashboard.put("totalRequired", totalRequired);
        dashboard.put("recoveryRate", String.format("%.1f%%", recoveryRate));
        dashboard.put("studentsWithDebt", studentFinanceRepository.findStudentsWithDebt().size());
        dashboard.put("totalStudents", allStudents.size());
        
        return dashboard;
    }
    
    @Override
    public Long getTotalDebt() {
        return studentFinanceRepository.totalDebtByStatus();
    }
    
    @Override
    public Long getTotalCollected() {
        return transactionRepository.totalPaymentsSince(LocalDateTime.now().minusMonths(1));
    }
    
    @Override
    public Map<String, Long> getExpensesByCategory() {
        Map<String, Long> expensesByCategory = new HashMap<>();
        List<String> categories = Arrays.asList("RH", "Loyer/Charges", "Matériel", "Autre");
        
        for (String category : categories) {
            Long total = expenseRepository.totalByCategory(category);
            expensesByCategory.put(category, total != null ? total : 0L);
        }
        
        return expensesByCategory;
    }
    
    @Override
    public List<Map<String, Object>> getPaymentTrends() {
        logger.info("Récupération des tendances de paiement");
        // Simplification pour l'exemple
        return new ArrayList<>();
    }
    
    // ==================== AUTOMATION (compatibilité JPA) ====================
    

    @Override
    public Map<String, Object> getFinancialReport(String period) {
        logger.info("Génération du rapport financier pour la période: {}", period);
        Map<String, Object> report = new HashMap<>();
        report.put("period", period);
        report.put("generatedAt", LocalDateTime.now().toString());
        return report;
    }
    
    // ==================== MAPPERS ====================
    
    private TransactionDto toTransactionDto(Transaction transaction) {
        return new TransactionDto(
            transaction.getId(),
            transaction.getStudentId(),
            transaction.getType(),
            transaction.getAmount(),
            transaction.getCurrency(),
            transaction.getStatus(),
            transaction.getTransactionDate()
        );
    }
    
    private Transaction toTransaction(TransactionDto dto) {
        return new Transaction(
            dto.id(),
            dto.studentId(),
            dto.type(),
            dto.amount(),
            dto.currency(),
            dto.status(),
            dto.transactionDate()
        );
    }
    
    private TransactionDto toTransactionDtoFromEntity(TransactionJpaEntity entity) {
        return new TransactionDto(
            entity.getId(),
            entity.getStudentId(),
            entity.getTransactionType(),
            entity.getAmount() != null ? entity.getAmount().toString() : "0",
            "FCFA", // Devise par défaut
            "COMPLETED", // Statut par défaut
            entity.getTransactionDate() != null ? entity.getTransactionDate().toString() : null
        );
    }

    // ==================== CONTRÔLES DES CHAMPS (APP-QA-04) ====================
    // APP-TL-22 : statuts d'un dossier tous calculés par EcolagesService.statut (En Règle, En Avance, Non soldé) ;
    // « En Retard » saisi à la main est supprimé.
    private static long nul0(Long v) {
        return v == null ? 0 : v;
    }

    private static String texteBorne(String v, String libelle, int max) {
        if (v.isBlank()) {
            throw ecole_221.sn.ecole.partage.ChampsRequete.refus(libelle + " ne peut pas être vide.");
        }
        if (v.strip().length() > max) {
            throw ecole_221.sn.ecole.partage.ChampsRequete.refus(libelle + " ne doit pas dépasser " + max + " caractères.");
        }
        return v.strip();
    }

    private static Long montant(Long v, String libelle, Long actuel) {
        if (v == null) {
            return actuel;
        }
        if (v < 0 || v > 1_000_000_000_000L) {
            throw ecole_221.sn.ecole.partage.ChampsRequete.refus(libelle + " doit être un montant positif (FCFA).");
        }
        return v;
    }
}
