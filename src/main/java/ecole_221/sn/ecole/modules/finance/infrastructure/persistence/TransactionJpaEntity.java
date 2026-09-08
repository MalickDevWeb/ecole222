package ecole_221.sn.ecole.modules.finance.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class TransactionJpaEntity {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "organization_id")
    private String organizationId;

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }


    @Id
    private String id;
    
    @Column(name = "student_finance_id")
    private String studentFinanceId;
    
    @Column(name = "student_id")
    private String studentId;
    
    @Column(name = "student_name")
    private String studentName;
    
    @Column(name = "amount")
    private Long amount; // in FCFA
    
    @Column(name = "transaction_type")
    private String transactionType; // 'Paiement', 'Remboursement', 'Avoir'
    
    @Column(name = "payment_method")
    private String paymentMethod; // 'Espèces', 'Carte', 'Mobile Money', 'Virement'
    
    @Column(name = "reference")
    private String reference; // référence du paiement
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public TransactionJpaEntity() {
        this.createdAt = LocalDateTime.now();
        this.transactionDate = LocalDateTime.now();
    }

    public TransactionJpaEntity(String id, String studentFinanceId, String studentId, String studentName,
                                 Long amount, String transactionType, String paymentMethod, 
                                 String reference, String description) {
        this();
        this.id = id;
        this.studentFinanceId = studentFinanceId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.amount = amount;
        this.transactionType = transactionType;
        this.paymentMethod = paymentMethod;
        this.reference = reference;
        this.description = description;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getStudentFinanceId() { return studentFinanceId; }
    public void setStudentFinanceId(String studentFinanceId) { this.studentFinanceId = studentFinanceId; }
    
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}