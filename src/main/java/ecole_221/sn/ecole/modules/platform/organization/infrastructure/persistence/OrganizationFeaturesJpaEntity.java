package ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "organization_features")
public class OrganizationFeaturesJpaEntity {

    @Id
    @Column(name = "organization_id")
    private String organizationId;

    @Column(name = "enable_parent_portal")
    private Boolean enableParentPortal;
    
    @Column(name = "enable_online_payment")
    private Boolean enableOnlinePayment;
    
    @Column(name = "enable_live_classes")
    private Boolean enableLiveClasses;
    
    @Column(name = "enable_sms_notifications")
    private Boolean enableSmsNotifications;

    // Getters and Setters
    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

    public Boolean getEnableParentPortal() { return enableParentPortal; }
    public void setEnableParentPortal(Boolean enableParentPortal) { this.enableParentPortal = enableParentPortal; }

    public Boolean getEnableOnlinePayment() { return enableOnlinePayment; }
    public void setEnableOnlinePayment(Boolean enableOnlinePayment) { this.enableOnlinePayment = enableOnlinePayment; }

    public Boolean getEnableLiveClasses() { return enableLiveClasses; }
    public void setEnableLiveClasses(Boolean enableLiveClasses) { this.enableLiveClasses = enableLiveClasses; }

    public Boolean getEnableSmsNotifications() { return enableSmsNotifications; }
    public void setEnableSmsNotifications(Boolean enableSmsNotifications) { this.enableSmsNotifications = enableSmsNotifications; }
}

