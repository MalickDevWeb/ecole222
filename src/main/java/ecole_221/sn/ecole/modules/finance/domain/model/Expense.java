package ecole_221.sn.ecole.modules.finance.domain.model;

import ecole_221.sn.ecole.modules.finance.domain.valueobject.ExpenseCategory;
import ecole_221.sn.ecole.modules.finance.domain.valueobject.ExpenseStatus;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Agrégat racine représentant une dépense scolaire.
 * Constructeur package-private pour garantir l'invariance - utiliser ExpenseFactory.
 */
public class Expense {

    private final String id;
    private String label;
    private final ExpenseCategory category;
    private final Long amount; // en FCFA
    private final LocalDateTime date;
    private String description;
    private ExpenseStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructeur package-private
    Expense(String id, String label, ExpenseCategory category, Long amount,
                   LocalDateTime date, String description, ExpenseStatus status,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.label = Objects.requireNonNull(label, "Le libellé ne peut pas être null");
        this.category = Objects.requireNonNull(category, "La catégorie ne peut pas être null");
        this.amount = Objects.requireNonNull(amount, "Le montant ne peut pas être null");
        this.date = Objects.requireNonNull(date, "La date ne peut pas être null");
        this.description = description;
        this.status = Objects.requireNonNull(status, "Le statut ne peut pas être null");
        this.createdAt = Objects.requireNonNull(createdAt, "La date de création ne peut pas être null");
        this.updatedAt = updatedAt;
        
        // Règle métier: le montant doit être positif
        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }
    }
    
    // Getters
    public String getId() {
        return id;
    }
    
    public String getLabel() {
        return label;
    }
    
    public ExpenseCategory getCategory() {
        return category;
    }
    
    public Long getAmount() {
        return amount;
    }
    
    public LocalDateTime getDate() {
        return date;
    }
    
    public String getDescription() {
        return description;
    }
    
    public ExpenseStatus getStatus() {
        return status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    // Comportements métier
    public void updateLabel(String newLabel) {
        this.label = Objects.requireNonNull(newLabel, "Le libellé ne peut pas être null");
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateDescription(String newDescription) {
        this.description = newDescription;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void markAsPaid() {
        this.status = ExpenseStatus.PAYE;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void markAsPending() {
        this.status = ExpenseStatus.EN_ATTENTE;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void cancel() {
        this.status = ExpenseStatus.ANNULE;
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isPaid() {
        return this.status.equals(ExpenseStatus.PAYE);
    }
    
    public boolean isPending() {
        return this.status.equals(ExpenseStatus.EN_ATTENTE);
    }
    
    public boolean isCancelled() {
        return this.status.equals(ExpenseStatus.ANNULE);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Expense expense = (Expense) o;
        return Objects.equals(id, expense.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Expense{" +
                "id='" + id + '\'' +
                ", label='" + label + '\'' +
                ", category=" + category +
                ", amount=" + amount +
                ", date=" + date +
                ", status=" + status +
                '}';
    }
}