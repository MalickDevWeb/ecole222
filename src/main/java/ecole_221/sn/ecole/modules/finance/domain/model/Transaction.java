package ecole_221.sn.ecole.modules.finance.domain.model;

/**
 * Modèle de domaine représentant une transaction financière.
 */
public class Transaction {
    private String id;
    private String studentId;
    private String type;
    private String amount;
    private String currency;
    private String status;
    private String transactionDate;
    
    public Transaction() {}
    
    public Transaction(String id, String studentId, String type, String amount, String currency, String status, String transactionDate) {
        this.id = id;
        this.studentId = studentId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.transactionDate = transactionDate;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getTransactionDate() { return transactionDate; }
    public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }
}