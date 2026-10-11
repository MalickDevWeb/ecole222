package ecole_221.sn.ecole.modules.platform.organization.application.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class SchoolRegistrationRequest {

    @JsonAlias("name")
    private String schoolName;

    /** Accepte "slug" (frontend) ou "subdomain" (legacy) */
    @JsonAlias("subdomain")
    private String slug;

    private String adminEmail;
    private String adminFirstName;
    private String adminLastName;
    private String adminPhone;

    /** Couleur primaire de l'établissement (ex: "#ef8427") */
    private String primaryColor;

    // Getters and Setters
    public String getSchoolName() { return schoolName; }
    public void setSchoolName(String schoolName) { this.schoolName = schoolName; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    /** Alias pour la compatibilité ascendante */
    public String getSubdomain() { return slug; }
    public void setSubdomain(String subdomain) { this.slug = subdomain; }

    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }

    public String getAdminFirstName() { return adminFirstName; }
    public void setAdminFirstName(String adminFirstName) { this.adminFirstName = adminFirstName; }

    public String getAdminLastName() { return adminLastName; }
    public void setAdminLastName(String adminLastName) { this.adminLastName = adminLastName; }

    public String getAdminPhone() { return adminPhone; }
    public void setAdminPhone(String adminPhone) { this.adminPhone = adminPhone; }

    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
}


