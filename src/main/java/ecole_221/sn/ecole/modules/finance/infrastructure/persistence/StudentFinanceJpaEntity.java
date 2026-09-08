package ecole_221.sn.ecole.modules.finance.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_finances")
public class StudentFinanceJpaEntity {

    @Id
    private String id;
    
    @Column(name = "student_id")
    private String studentId;
    
    @Column(name = "student_name")
    private String studentName;
    
    @Column(name = "classe")
    private String classe;
    
    @Column(name = "status")
    private String status; // 'En Règle', 'En Avance', 'En Retard'
    
    @Column(name = "debt")
    private Long debt; // in FCFA
    
    @Column(name = "paid")
    private Long paid; // in FCFA
    
    @Column(name = "total_required")
    private Long totalRequired; // montant total requis
    
    @Column(name = "last_payment_date")
    private LocalDateTime lastPaymentDate;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public StudentFinanceJpaEntity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public StudentFinanceJpaEntity(String id, String studentId, String studentName, String classe, 
                                   String status, Long debt, Long paid, Long totalRequired) {
        this();
        this.id = id;
        this.studentId = studentId;
        this.studentName = studentName;
        this.classe = classe;
        this.status = status;
        this.debt = debt;
        this.paid = paid;
        this.totalRequired = totalRequired;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    
    public String getClasse() { return classe; }
    public void setClasse(String classe) { this.classe = classe; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Long getDebt() { return debt; }
    public void setDebt(Long debt) { this.debt = debt; }
    
    public Long getPaid() { return paid; }
    public void setPaid(Long paid) { this.paid = paid; }
    
    public Long getTotalRequired() { return totalRequired; }
    public void setTotalRequired(Long totalRequired) { this.totalRequired = totalRequired; }
    
    public LocalDateTime getLastPaymentDate() { return lastPaymentDate; }
    public void setLastPaymentDate(LocalDateTime lastPaymentDate) { this.lastPaymentDate = lastPaymentDate; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @jakarta.persistence.PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}