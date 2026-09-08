package ecole_221.sn.ecole.modules.platform.organization.domain.model;

import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus;
import ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * Agrégat racine représentant une organization (tenant) sur la plateforme.
 */
public class Organization {
    
    private final String id;
    private String name;
    private OrganizationType type;
    private OrganizationStatus status;
    
    // Informations établissement
    private String country;
    private String city;
    private String address;
    private String phone;
    private String email;
    private String website;
    private String logo;
    
    // Configuration
    private String subscriptionId;
    private String primaryDomainId;
    private Map<String, Object> settings;
    
    // Timestamps
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime validatedAt;
    private LocalDateTime suspendedAt;
    
    // Constructeur privé
    private Organization(String id, String name, OrganizationType type, OrganizationStatus status,
                      String country, String city, String address, String phone, String email, 
                      String website, String logo, String subscriptionId, String primaryDomainId,
                      Map<String, Object> settings, LocalDateTime createdAt, LocalDateTime updatedAt,
                      LocalDateTime validatedAt, LocalDateTime suspendedAt) {
        this.id = Objects.requireNonNull(id, "L'ID ne peut pas être null");
        this.name = Objects.requireNonNull(name, "Le nom ne peut pas être null");
        this.type = Objects.requireNonNull(type, "Le type ne peut pas être null");
        this.status = Objects.requireNonNull(status, "Le statut ne peut pas être null");
        this.country = country;
        this.city = city;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.website = website;
        this.logo = logo;
        this.subscriptionId = subscriptionId;
        this.primaryDomainId = primaryDomainId;
        this.settings = settings;
        this.createdAt = Objects.requireNonNull(createdAt, "La date de création ne peut pas être null");
        this.updatedAt = updatedAt;
        this.validatedAt = validatedAt;
        this.suspendedAt = suspendedAt;
    }
    
    // Méthode package-private pour la Factory
    static Organization createInternal(String id, String name, OrganizationType type, OrganizationStatus status,
                                     String country, String city, String address, String phone, String email,
                                     String website, String logo, String subscriptionId, String primaryDomainId,
                                     Map<String, Object> settings, LocalDateTime createdAt, LocalDateTime updatedAt,
                                     LocalDateTime validatedAt, LocalDateTime suspendedAt) {
        return new Organization(id, name, type, status, country, city, address, phone, email, website, logo,
                              subscriptionId, primaryDomainId, settings, createdAt, updatedAt, validatedAt, suspendedAt);
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public OrganizationType getType() { return type; }
    public OrganizationStatus getStatus() { return status; }
    public String getCountry() { return country; }
    public String getCity() { return city; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getWebsite() { return website; }
    public String getLogo() { return logo; }
    public String getSubscriptionId() { return subscriptionId; }
    public String getPrimaryDomainId() { return primaryDomainId; }
    public Map<String, Object> getSettings() { return settings; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getValidatedAt() { return validatedAt; }
    public LocalDateTime getSuspendedAt() { return suspendedAt; }
    
    // Setters (pour comportements métier)
    public void setName(String name) { 
        this.name = Objects.requireNonNull(name, "Le nom ne peut pas être null");
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setType(OrganizationType type) { 
        this.type = Objects.requireNonNull(type, "Le type ne peut pas être null");
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setStatus(OrganizationStatus status) { 
        this.status = Objects.requireNonNull(status, "Le statut ne peut pas être null");
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setCountry(String country) { 
        this.country = country;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setCity(String city) { 
        this.city = city;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setAddress(String address) { 
        this.address = address;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setPhone(String phone) { 
        this.phone = phone;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setEmail(String email) { 
        this.email = email;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setWebsite(String website) { 
        this.website = website;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setLogo(String logo) { 
        this.logo = logo;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setSubscriptionId(String subscriptionId) { 
        this.subscriptionId = subscriptionId;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setPrimaryDomainId(String primaryDomainId) { 
        this.primaryDomainId = primaryDomainId;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setSettings(Map<String, Object> settings) { 
        this.settings = settings;
        this.updatedAt = LocalDateTime.now();
    }
    
    // Comportements métier
    public void validate() {
        if (this.status != OrganizationStatus.PENDING) {
            throw new IllegalStateException("Seules les organizations en statut PENDING peuvent être validées");
        }
        this.status = OrganizationStatus.ACTIVE;
        this.validatedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public void suspend() {
        if (this.status != OrganizationStatus.ACTIVE) {
            throw new IllegalStateException("Seules les organizations ACTIVE peuvent être suspendues");
        }
        this.status = OrganizationStatus.SUSPENDED;
        this.suspendedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public void reactivate() {
        if (this.status != OrganizationStatus.SUSPENDED) {
            throw new IllegalStateException("Seules les organizations SUSPENDED peuvent être réactivées");
        }
        this.status = OrganizationStatus.ACTIVE;
        this.suspendedAt = null;
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isActive() {
        return this.status == OrganizationStatus.ACTIVE;
    }
    
    public boolean isPending() {
        return this.status == OrganizationStatus.PENDING;
    }
    
    public boolean isSuspended() {
        return this.status == OrganizationStatus.SUSPENDED;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Organization that = (Organization) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Organization{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", city='" + city + '\'' +
                ", country='" + country + '\'' +
                '}';
    }
}
