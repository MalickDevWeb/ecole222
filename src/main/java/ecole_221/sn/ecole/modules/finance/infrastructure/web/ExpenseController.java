package ecole_221.sn.ecole.modules.finance.infrastructure.web;

import ecole_221.sn.ecole.modules.finance.application.port.command.CreateExpenseCommand;
import ecole_221.sn.ecole.modules.finance.application.port.command.UpdateExpenseCommand;
import ecole_221.sn.ecole.modules.finance.application.port.command.UpdateExpenseStatusCommand;
import ecole_221.sn.ecole.modules.finance.application.port.usecase.CreateExpenseUseCase;
import ecole_221.sn.ecole.modules.finance.application.port.usecase.GetExpenseUseCase;
import ecole_221.sn.ecole.modules.finance.application.port.usecase.UpdateExpenseUseCase;
import ecole_221.sn.ecole.modules.finance.domain.model.Expense;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour la gestion des dépenses selon l'architecture hexagonale.
 * Utilise les UseCases et Commands de la couche application.
 */
@RestController
@RequestMapping("/finance/expenses")
@CrossOrigin(origins = "*")
@Tag(name = "Expense (DDD)", description = "Gestion des dépenses - Architecture Hexagonale")
public class ExpenseController {
    
    private static final Logger logger = LoggerFactory.getLogger(ExpenseController.class);
    
    private final CreateExpenseUseCase createExpenseUseCase;
    private final UpdateExpenseUseCase updateExpenseUseCase;
    private final GetExpenseUseCase getExpenseUseCase;
    
    public ExpenseController(CreateExpenseUseCase createExpenseUseCase,
                            UpdateExpenseUseCase updateExpenseUseCase,
                            GetExpenseUseCase getExpenseUseCase) {
        this.createExpenseUseCase = createExpenseUseCase;
        this.updateExpenseUseCase = updateExpenseUseCase;
        this.getExpenseUseCase = getExpenseUseCase;
    }
    
    // ==================== ENDPOINTS CRUD ====================
    
    @PostMapping
    @Operation(summary = "Créer une dépense", description = "Crée une nouvelle dépense avec le statut 'En Attente' par défaut")
    public ResponseEntity<Expense> createExpense(@RequestBody CreateExpenseCommand command) {
        logger.info("POST /finance/expenses - Création dépense: {}", command.label());
        Expense expense = createExpenseUseCase.create(command);
        return ResponseEntity.ok(expense);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Récupérer une dépense", description = "Récupère une dépense par son ID")
    public ResponseEntity<Expense> getExpense(@PathVariable String id) {
        logger.info("GET /finance/expenses/{} - Récupération dépense", id);
        return getExpenseUseCase.getById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    @Operation(summary = "Liste des dépenses", description = "Récupère toutes les dépenses")
    public ResponseEntity<List<Expense>> getAllExpenses() {
        logger.info("GET /finance/expenses - Récupération toutes dépenses");
        return ResponseEntity.ok(getExpenseUseCase.getAll());
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour une dépense", description = "Met à jour le libellé et la description d'une dépense")
    public ResponseEntity<Expense> updateExpense(@PathVariable String id, 
                                                @RequestBody UpdateExpenseCommand command) {
        logger.info("PUT /finance/expenses/{} - Mise à jour dépense", id);
        Expense expense = updateExpenseUseCase.update(command);
        return ResponseEntity.ok(expense);
    }
    
    @PatchMapping("/{id}/status")
    @Operation(summary = "Mettre à jour le statut", description = "Met à jour le statut d'une dépense (Payé, En Attente, Annulé)")
    public ResponseEntity<Expense> updateExpenseStatus(@PathVariable String id, 
                                                       @RequestBody Map<String, String> body) {
        logger.info("PATCH /finance/expenses/{}/status - Mise à jour statut: {}", id, body.get("status"));
        UpdateExpenseStatusCommand command = new UpdateExpenseStatusCommand(id, body.get("status"));
        Expense expense = updateExpenseUseCase.updateStatus(command);
        return ResponseEntity.ok(expense);
    }
    
    // ==================== ENDPOINTS RECHERCHE ====================
    
    @GetMapping("/category/{category}")
    @Operation(summary = "Dépenses par catégorie", description = "Récupère les dépenses par catégorie (RH, Loyer/Charges, Matériel, Autre)")
    public ResponseEntity<List<Expense>> getExpensesByCategory(@PathVariable String category) {
        logger.info("GET /finance/expenses/category/{} - Récupération par catégorie", category);
        return ResponseEntity.ok(getExpenseUseCase.getByCategory(category));
    }
    
    @GetMapping("/status/{status}")
    @Operation(summary = "Dépenses par statut", description = "Récupère les dépenses par statut (Payé, En Attente, Annulé)")
    public ResponseEntity<List<Expense>> getExpensesByStatus(@PathVariable String status) {
        logger.info("GET /finance/expenses/status/{} - Récupération par statut", status);
        return ResponseEntity.ok(getExpenseUseCase.getByStatus(status));
    }
    
    @GetMapping("/between")
    @Operation(summary = "Dépenses entre deux dates", description = "Récupère les dépenses entre deux dates")
    public ResponseEntity<List<Expense>> getExpensesBetweenDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        logger.info("GET /finance/expenses/between - Récupération entre {} et {}", startDate, endDate);
        return ResponseEntity.ok(getExpenseUseCase.getByDateBetween(startDate, endDate));
    }
    
    @GetMapping("/recent")
    @Operation(summary = "Dépenses récentes", description = "Récupère les dépenses récentes depuis une date")
    public ResponseEntity<List<Expense>> getRecentExpenses(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate) {
        logger.info("GET /finance/expenses/recent - Récupération récentes depuis {}", startDate);
        return ResponseEntity.ok(getExpenseUseCase.getRecentExpenses(startDate));
    }
    
    @GetMapping("/total/{category}")
    @Operation(summary = "Total payé par catégorie", description = "Calcule le total des dépenses payées pour une catégorie")
    public ResponseEntity<Long> getTotalPaidByCategory(@PathVariable String category) {
        logger.info("GET /finance/expenses/total/{} - Calcul total payé", category);
        return ResponseEntity.ok(getExpenseUseCase.getTotalPaidByCategory(category));
    }
}