package ecole_221.sn.ecole.modules.finance.application.port.dto;

import java.time.LocalDateTime;

/**
 * DTO pour représenter une dépense dans l'API.
 * Simple POJO sans logique métier.
 */
public class ExpenseDto {
    
    private String id;
    private String label;
    private String category;
    private Long amount;
    private LocalDateTime date;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public ExpenseDto() {}
    
    public ExpenseDto(String id, String label, String category, Long amount, 
                     LocalDateTime date, String description, String status, 
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.label = label;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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
}