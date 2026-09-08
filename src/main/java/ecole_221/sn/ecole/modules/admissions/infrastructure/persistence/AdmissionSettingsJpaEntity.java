package ecole_221.sn.ecole.modules.admissions.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "admission_settings")
public class AdmissionSettingsJpaEntity {

    @Id
    private String id;
    
    @Column(name = "academic_year")
    private String academicYear; // '2025-2026', etc.
    
    @Column(name = "is_open")
    private Boolean isOpen;
    
    @Column(name = "opening_date")
    private LocalDateTime openingDate;
    
    @Column(name = "closing_date")
    private LocalDateTime closingDate;
    
    @Column(name = "announcement_message")
    private String announcementMessage;
    
    @Column(name = "show_register_button")
    private Boolean showRegisterButton;
    
    @Column(name = "registration_fee_amount")
    private Long registrationFeeAmount;
    
    @Column(name = "max_candidates")
    private Integer maxCandidates;
    
    @Column(name = "current_candidates_count")
    private Integer currentCandidatesCount;
    
    @Column(name = "required_documents")
    private String requiredDocuments; // JSON: {"cni": true, "bulletin": true, "diplome": true, "motivation": true}
    
    @Column(name = "interview_required")
    private Boolean interviewRequired;
    
    @Column(name = "min_academic_score")
    private Double minAcademicScore;
    
    @Column(name = "passing_score")
    private Double passingScore;
    
    @Column(name = "additional_settings")
    private String additionalSettings; // JSON pour paramètres supplémentaires
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public AdmissionSettingsJpaEntity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isOpen = false;
        this.showRegisterButton = true;
        this.interviewRequired = true;
        this.currentCandidatesCount = 0;
    }

    public AdmissionSettingsJpaEntity(String id, String academicYear, Boolean isOpen, 
                                       LocalDateTime openingDate, LocalDateTime closingDate) {
        this();
        this.id = id;
        this.academicYear = academicYear;
        this.isOpen = isOpen;
        this.openingDate = openingDate;
        this.closingDate = closingDate;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    
    public Boolean getIsOpen() { return isOpen; }
    public void setIsOpen(Boolean isOpen) { this.isOpen = isOpen; }
    
    public LocalDateTime getOpeningDate() { return openingDate; }
    public void setOpeningDate(LocalDateTime openingDate) { this.openingDate = openingDate; }
    
    public LocalDateTime getClosingDate() { return closingDate; }
    public void setClosingDate(LocalDateTime closingDate) { this.closingDate = closingDate; }
    
    public String getAnnouncementMessage() { return announcementMessage; }
    public void setAnnouncementMessage(String announcementMessage) { this.announcementMessage = announcementMessage; }
    
    public Boolean getShowRegisterButton() { return showRegisterButton; }
    public void setShowRegisterButton(Boolean showRegisterButton) { this.showRegisterButton = showRegisterButton; }
    
    public Long getRegistrationFeeAmount() { return registrationFeeAmount; }
    public void setRegistrationFeeAmount(Long registrationFeeAmount) { this.registrationFeeAmount = registrationFeeAmount; }
    
    public Integer getMaxCandidates() { return maxCandidates; }
    public void setMaxCandidates(Integer maxCandidates) { this.maxCandidates = maxCandidates; }
    
    public Integer getCurrentCandidatesCount() { return currentCandidatesCount; }
    public void setCurrentCandidatesCount(Integer currentCandidatesCount) { this.currentCandidatesCount = currentCandidatesCount; }
    
    public String getRequiredDocuments() { return requiredDocuments; }
    public void setRequiredDocuments(String requiredDocuments) { this.requiredDocuments = requiredDocuments; }
    
    public Boolean getInterviewRequired() { return interviewRequired; }
    public void setInterviewRequired(Boolean interviewRequired) { this.interviewRequired = interviewRequired; }
    
    public Double getMinAcademicScore() { return minAcademicScore; }
    public void setMinAcademicScore(Double minAcademicScore) { this.minAcademicScore = minAcademicScore; }
    
    public Double getPassingScore() { return passingScore; }
    public void setPassingScore(Double passingScore) { this.passingScore = passingScore; }
    
    public String getAdditionalSettings() { return additionalSettings; }
    public void setAdditionalSettings(String additionalSettings) { this.additionalSettings = additionalSettings; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @jakarta.persistence.PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}