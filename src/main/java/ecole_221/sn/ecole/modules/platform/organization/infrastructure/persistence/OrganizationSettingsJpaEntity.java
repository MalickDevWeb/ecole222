package ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "organization_settings")
public class OrganizationSettingsJpaEntity {

    @Id
    @Column(name = "organization_id")
    private String organizationId;

    private String timezone;
    
    @Column(name = "date_format")
    private String dateFormat;
    
    @Column(name = "time_format")
    private String timeFormat;
    
    private String language;
    
    @Column(name = "academic_year_start")
    private LocalDate academicYearStart;
    
    @Column(name = "academic_year_end")
    private LocalDate academicYearEnd;

    // Getters and Setters
    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public String getDateFormat() { return dateFormat; }
    public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }

    public String getTimeFormat() { return timeFormat; }
    public void setTimeFormat(String timeFormat) { this.timeFormat = timeFormat; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public LocalDate getAcademicYearStart() { return academicYearStart; }
    public void setAcademicYearStart(LocalDate academicYearStart) { this.academicYearStart = academicYearStart; }

    public LocalDate getAcademicYearEnd() { return academicYearEnd; }
    public void setAcademicYearEnd(LocalDate academicYearEnd) { this.academicYearEnd = academicYearEnd; }
}

