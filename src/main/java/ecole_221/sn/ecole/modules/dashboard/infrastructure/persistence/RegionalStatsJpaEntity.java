package ecole_221.sn.ecole.modules.dashboard.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "regional_stats")
public class RegionalStatsJpaEntity {

    @Id
    private String id;
    
    @Column(name = "region_name")
    private String regionName; // 'Dakar Centre', 'Thiès & Mbour', 'Saint-Louis / Nord', etc.
    
    @Column(name = "percentage")
    private Double percentage; // pourcentage d'étudiants
    
    @Column(name = "student_count")
    private Integer studentCount;
    
    @Column(name = "color_code")
    private String colorCode; // code couleur pour affichage frontend
    
    @Column(name = "academic_year")
    private String academicYear; // '2025-2026', etc.
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public RegionalStatsJpaEntity() {
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }

    public RegionalStatsJpaEntity(String id, String regionName, Double percentage, 
                                  Integer studentCount, String colorCode, String academicYear) {
        this();
        this.id = id;
        this.regionName = regionName;
        this.percentage = percentage;
        this.studentCount = studentCount;
        this.colorCode = colorCode;
        this.academicYear = academicYear;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getRegionName() { return regionName; }
    public void setRegionName(String regionName) { this.regionName = regionName; }
    
    public Double getPercentage() { return percentage; }
    public void setPercentage(Double percentage) { this.percentage = percentage; }
    
    public Integer getStudentCount() { return studentCount; }
    public void setStudentCount(Integer studentCount) { this.studentCount = studentCount; }
    
    public String getColorCode() { return colorCode; }
    public void setColorCode(String colorCode) { this.colorCode = colorCode; }
    
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    @jakarta.persistence.PreUpdate
    public void preUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }
}