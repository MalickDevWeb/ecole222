package ecole_221.sn.ecole.modules.email.domain.model;

import ecole_221.sn.ecole.modules.email.domain.valueobject.EmailFormat;

import java.util.Map;
import java.util.Objects;

/**
 * Agrégat racine représentant une requête d'envoi d'email.
 * Constructeur package-private pour garantir l'invariance - utiliser EmailRequestFactory.
 */
public class EmailRequest {
    
    private final String to;
    private final String subject;
    private final String content;
    private final EmailFormat format;
    private final Map<String, Object> metadata;
    
    // Constructeur package-private
    EmailRequest(String to, String subject, String content, EmailFormat format, Map<String, Object> metadata) {
        this.to = Objects.requireNonNull(to, "Le destinataire ne peut pas être null");
        this.subject = Objects.requireNonNull(subject, "Le sujet ne peut pas être null");
        this.content = Objects.requireNonNull(content, "Le contenu ne peut pas être null");
        this.format = Objects.requireNonNull(format, "Le format ne peut pas être null");
        this.metadata = metadata != null ? metadata : Map.of();
        
        // Validation de l'adresse email
        if (!isValidEmail(to)) {
            throw new IllegalArgumentException("Adresse email invalide: " + to);
        }
    }
    
    // Getters
    public String getTo() { return to; }
    public String getSubject() { return subject; }
    public String getContent() { return content; }
    public EmailFormat getFormat() { return format; }
    public Map<String, Object> getMetadata() { return metadata; }
    
    // Comportements métier
    public boolean isHtml() {
        return this.format.equals(EmailFormat.HTML);
    }
    
    public boolean isText() {
        return this.format.equals(EmailFormat.TEXT);
    }
    
    public boolean hasMetadata() {
        return this.metadata != null && !this.metadata.isEmpty();
    }
    
    public Object getMetadataValue(String key) {
        return this.metadata.get(key);
    }
    
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(emailRegex);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmailRequest that = (EmailRequest) o;
        return Objects.equals(to, that.to) && Objects.equals(subject, that.subject);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(to, subject);
    }
    
    @Override
    public String toString() {
        return "EmailRequest{" +
                "to='" + to + '\'' +
                ", subject='" + subject + '\'' +
                ", format=" + format +
                ", hasMetadata=" + hasMetadata() +
                '}';
    }
}