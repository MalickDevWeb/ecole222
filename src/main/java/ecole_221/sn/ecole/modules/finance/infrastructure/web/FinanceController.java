package ecole_221.sn.ecole.modules.finance.infrastructure.web;

import ecole_221.sn.ecole.modules.finance.application.port.dto.TransactionDto;
import ecole_221.sn.ecole.modules.finance.application.port.usecase.FinanceUseCase;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/finance")
@CrossOrigin(origins = "*")
@Tag(name = "Finance", description = "Gestion financière - écolages, dépenses, transactions")
public class FinanceController {
    
    private static final Logger logger = LoggerFactory.getLogger(FinanceController.class);
    private final FinanceUseCase financeUseCase;
    
    public FinanceController(FinanceUseCase financeUseCase) {
        this.financeUseCase = financeUseCase;
    }
    
    // ==================== ENDPOINTS FINANCES ÉTUDIANTS (compatibilité JPA) ====================
    
    @GetMapping("/students")
    @Operation(summary = "Liste des finances étudiantes", description = "Retourne toutes les finances étudiantes")
    public ResponseEntity<List<StudentFinanceJpaEntity>> getAllStudentFinances() {
        logger.info("GET /finance/students - Récupération finances étudiantes");
        return ResponseEntity.ok(financeUseCase.getAllStudentFinances());
    }
    
    @GetMapping("/students/{id}")
    @Operation(summary = "Détails finance étudiant", description = "Retourne les détails financiers d'un étudiant")
    public ResponseEntity<StudentFinanceJpaEntity> getStudentFinance(@PathVariable String id) {
        logger.info("GET /finance/students/{} - Récupération finance étudiant", id);
        StudentFinanceJpaEntity studentFinance = financeUseCase.getStudentFinanceById(id);
        if (studentFinance == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(studentFinance);
    }
    
    @GetMapping("/students/classe/{classe}")
    @Operation(summary = "Finances par classe", description = "Retourne les finances étudiantes par classe")
    public ResponseEntity<List<StudentFinanceJpaEntity>> getStudentFinancesByClasse(@PathVariable String classe) {
        logger.info("GET /finance/students/classe/{} - Récupération finances par classe", classe);
        return ResponseEntity.ok(financeUseCase.getStudentFinancesByClasse(classe));
    }
    
    @GetMapping("/students/debt")
    @Operation(summary = "Étudiants avec dette", description = "Retourne les étudiants ayant une dette")
    public ResponseEntity<List<StudentFinanceJpaEntity>> getStudentsWithDebt() {
        logger.info("GET /finance/students/debt - Récupération étudiants avec dette");
        return ResponseEntity.ok(financeUseCase.getStudentsWithDebt());
    }
    
    @PostMapping("/students")
    @Operation(summary = "Créer finance étudiant", description = "Crée un nouveau dossier financier étudiant")
    public ResponseEntity<StudentFinanceJpaEntity> createStudentFinance(@RequestBody StudentFinanceJpaEntity studentFinance) {
        logger.info("POST /finance/students - Création finance étudiant");
        return ResponseEntity.ok(financeUseCase.createStudentFinance(studentFinance));
    }
    
    @PutMapping("/students/{id}")
    @Operation(summary = "Mettre à jour finance étudiant", description = "Met à jour les informations financières d'un étudiant")
    public ResponseEntity<StudentFinanceJpaEntity> updateStudentFinance(@PathVariable String id, 
                                                                         @RequestBody StudentFinanceJpaEntity studentFinance) {
        logger.info("PUT /finance/students/{} - Mise à jour finance étudiant", id);
        return ResponseEntity.ok(financeUseCase.updateStudentFinance(id, studentFinance));
    }
    
    @PatchMapping("/students/{id}/status")
    @Operation(summary = "Mettre à jour statut étudiant", description = "Met à jour le statut de paiement d'un étudiant")
    public ResponseEntity<StudentFinanceJpaEntity> updateStudentStatus(@PathVariable String id, 
                                                                        @RequestBody Map<String, String> body) {
        logger.info("PATCH /finance/students/{}/status - Mise à jour statut", id);
        return ResponseEntity.ok(financeUseCase.updateStudentStatus(id, body.get("status")));
    }
    
    @DeleteMapping("/students/{id}")
    @Operation(summary = "Supprimer finance étudiant", description = "Supprime le dossier financier d'un étudiant")
    public ResponseEntity<Void> deleteStudentFinance(@PathVariable String id) {
        logger.info("DELETE /finance/students/{} - Suppression finance étudiant", id);
        financeUseCase.deleteStudentFinance(id);
        return ResponseEntity.ok().build();
    }
    
    // ==================== ENDPOINTS TRANSACTIONS ====================
    
    @GetMapping("/transactions")
    @Operation(summary = "Liste des transactions", description = "Retourne toutes les transactions")
    public ResponseEntity<List<TransactionDto>> getAllTransactions() {
        logger.info("GET /finance/transactions - Récupération transactions");
        return ResponseEntity.ok(financeUseCase.getAllTransactions());
    }
    
    @GetMapping("/transactions/student/{studentId}")
    @Operation(summary = "Transactions étudiant", description = "Retourne les transactions d'un étudiant")
    public ResponseEntity<List<TransactionDto>> getTransactionsByStudentId(@PathVariable String studentId) {
        logger.info("GET /finance/transactions/student/{} - Récupération transactions étudiant", studentId);
        return ResponseEntity.ok(financeUseCase.getTransactionsByStudentId(studentId));
    }
    
    @GetMapping("/transactions/recent")
    @Operation(summary = "Transactions récentes", description = "Retourne les transactions des 30 derniers jours")
    public ResponseEntity<List<TransactionDto>> getRecentTransactions() {
        logger.info("GET /finance/transactions/recent - Récupération transactions récentes");
        return ResponseEntity.ok(financeUseCase.getRecentTransactions());
    }
    
    @PostMapping("/transactions")
    @Operation(summary = "Créer transaction", description = "Crée une nouvelle transaction")
    public ResponseEntity<TransactionDto> createTransaction(@RequestBody TransactionDto transaction) {
        logger.info("POST /finance/transactions - Création transaction");
        return ResponseEntity.ok(financeUseCase.createTransaction(transaction));
    }
    
    // ==================== ENDPOINTS ANALYTIQUES (compatibilité JPA) ====================
    
    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard financier", description = "Retourne le dashboard financier")
    public ResponseEntity<Map<String, Object>> getFinanceDashboard() {
        logger.info("GET /finance/dashboard - Récupération dashboard financier");
        return ResponseEntity.ok(financeUseCase.getFinanceDashboard());
    }
    
    @GetMapping("/total-debt")
    @Operation(summary = "Dette totale", description = "Retourne la dette totale")
    public ResponseEntity<Long> getTotalDebt() {
        logger.info("GET /finance/total-debt - Récupération dette totale");
        return ResponseEntity.ok(financeUseCase.getTotalDebt());
    }
    
    @GetMapping("/total-collected")
    @Operation(summary = "Total collecté", description = "Retourne le total collecté")
    public ResponseEntity<Long> getTotalCollected() {
        logger.info("GET /finance/total-collected - Récupération total collecté");
        return ResponseEntity.ok(financeUseCase.getTotalCollected());
    }
    
    @GetMapping("/expenses-by-category")
    @Operation(summary = "Dépenses par catégorie", description = "Retourne les dépenses par catégorie")
    public ResponseEntity<Map<String, Long>> getExpensesByCategory() {
        logger.info("GET /finance/expenses-by-category - Récupération dépenses par catégorie");
        return ResponseEntity.ok(financeUseCase.getExpensesByCategory());
    }
    
    @GetMapping("/payment-trends")
    @Operation(summary = "Tendances de paiement", description = "Retourne les tendances de paiement")
    public ResponseEntity<List<Map<String, Object>>> getPaymentTrends() {
        logger.info("GET /finance/payment-trends - Récupération tendances de paiement");
        return ResponseEntity.ok(financeUseCase.getPaymentTrends());
    }
    
    // ==================== ENDPOINTS AUTOMATION (compatibilité JPA) ====================
    
    @PostMapping("/payment-reminders")
    @Operation(summary = "Rappels de paiement", description = "Déclenche les rappels de paiement")
    public ResponseEntity<Map<String, Object>> triggerPaymentReminders() {
        logger.info("POST /finance/payment-reminders - Déclenchement rappels de paiement");
        financeUseCase.triggerPaymentReminders();
        return ResponseEntity.ok(Map.of("success", true));
    }
    
    @PostMapping("/block-debtors")
    @Operation(summary = "Bloquer débiteurs", description = "Bloque les débiteurs")
    public ResponseEntity<Map<String, Object>> blockDebtors() {
        logger.info("POST /finance/block-debtors - Blocage débiteurs");
        financeUseCase.blockDebtors();
        return ResponseEntity.ok(Map.of("success", true));
    }
    
    @GetMapping("/financial-report/{period}")
    @Operation(summary = "Rapport financier", description = "Génère un rapport financier")
    public ResponseEntity<Map<String, Object>> getFinancialReport(@PathVariable String period) {
        logger.info("GET /finance/financial-report/{} - Génération rapport financier", period);
        return ResponseEntity.ok(financeUseCase.getFinancialReport(period));
    }
}