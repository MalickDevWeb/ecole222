package ecole_221.sn.ecole.modules.collaboration.domain.valueobject;

public record MessageStatus(String status) {
    public static final MessageStatus SENT = new MessageStatus("SENT");
    public static final MessageStatus DELIVERED = new MessageStatus("DELIVERED");
    public static final MessageStatus READ = new MessageStatus("READ");
    public static final MessageStatus FAILED = new MessageStatus("FAILED");
    
    public MessageStatus {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Le statut du message ne peut pas être vide");
        }
    }
    
    public boolean isSent() {
        return "SENT".equals(status);
    }
    
    public boolean isDelivered() {
        return "DELIVERED".equals(status);
    }
    
    public boolean isRead() {
        return "READ".equals(status);
    }
    
    public boolean isFailed() {
        return "FAILED".equals(status);
    }
    
    public boolean isSuccess() {
        return isSent() || isDelivered() || isRead();
    }
}