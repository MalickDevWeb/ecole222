package ecole_221.sn.ecole.modules.finance.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
public class ExpenseJpaEntity {

    @Id
    private String id;
    
    @Column(name = "label")
    private String label;
    
    @Column(name = "category")
    private String category; // 'RH', 'Loyer/Charges', 'Matériel', 'Autre'
    
    @Column(name = "amount")
    private Long amount; // in FCFA
    
    @Column(name = "date")
    private LocalDateTime date;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "status")
    private String status; // 'Payé', 'En Attente', 'Annulé'
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ExpenseJpaEntity() {
    }

    public ExpenseJpaEntity(String id, String label, String category, Long amount, 
                           LocalDateTime date, String description, String status) {
        this();
        this.id = id;
        this.label = label;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.description = description;
        this.status = status;
    }
    
    public ExpenseJpaEntity(String id, String label, String category, Long amount, 
                           LocalDateTime date, String description, String status,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.label = label;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @jakarta.persistence.PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @jakarta.persistence.PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}