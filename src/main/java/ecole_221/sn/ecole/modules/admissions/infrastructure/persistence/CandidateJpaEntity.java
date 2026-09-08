package ecole_221.sn.ecole.modules.admissions.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidates")
public class CandidateJpaEntity {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "organization_id")
    private String organizationId;

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }


    @Id
    private String id;
    
    @Column(name = "first_name")
    private String firstName;
    
    @Column(name = "last_name")
    private String lastName;
    
    @Column(name = "full_name")
    private String fullName;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "phone")
    private String phone;
    
    @Column(name = "course_choice")
    private String courseChoice; // 'L2 Génie Civil', 'Master 1 Spécialité IA', etc.
    
    @Column(name = "current_step")
    private String currentStep; // 'new', 'docs', 'interview', 'admitted', 'rejected'
    
    @Column(name = "diploma_uploaded")
    private Boolean diplomaUploaded;
    
    @Column(name = "id_card_uploaded")
    private Boolean idCardUploaded;
    
    @Column(name = "motivation_letter_uploaded")
    private Boolean motivationLetterUploaded;
    
    @Column(name = "registration_fee_paid")
    private Boolean registrationFeePaid;
    
    @Column(name = "registration_fee_amount")
    private Long registrationFeeAmount;
    
    @Column(name = "registration_fee_reference")
    private String registrationFeeReference;
    
    @Column(name = "admission_status")
    private String admissionStatus; // 'pending', 'accepted', 'rejected', 'waitlist'
    
    @Column(name = "rejection_reason")
    private String rejectionReason;
    
    @Column(name = "interview_date")
    private LocalDateTime interviewDate;
    
    @Column(name = "interview_score")
    private Double interviewScore;
    
    @Column(name = "academic_score")
    private Double academicScore;
    
    @Column(name = "total_score")
    private Double totalScore;
    
    @Column(name = "additional_info")
    private String additionalInfo; // JSON pour données supplémentaires
    
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public CandidateJpaEntity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.currentStep = "new";
        this.admissionStatus = "pending";
        this.registrationFeePaid = false;
    }

    public CandidateJpaEntity(String id, String firstName, String lastName, String email, 
                             String phone, String courseChoice) {
        this();
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = firstName + " " + lastName;
        this.email = email;
        this.phone = phone;
        this.courseChoice = courseChoice;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getCourseChoice() { return courseChoice; }
    public void setCourseChoice(String courseChoice) { this.courseChoice = courseChoice; }
    
    public String getCurrentStep() { return currentStep; }
    public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
    
    public Boolean getDiplomaUploaded() { return diplomaUploaded; }
    public void setDiplomaUploaded(Boolean diplomaUploaded) { this.diplomaUploaded = diplomaUploaded; }
    
    public Boolean getIdCardUploaded() { return idCardUploaded; }
    public void setIdCardUploaded(Boolean idCardUploaded) { this.idCardUploaded = idCardUploaded; }
    
    public Boolean getMotivationLetterUploaded() { return motivationLetterUploaded; }
    public void setMotivationLetterUploaded(Boolean motivationLetterUploaded) { this.motivationLetterUploaded = motivationLetterUploaded; }
    
    public Boolean getRegistrationFeePaid() { return registrationFeePaid; }
    public void setRegistrationFeePaid(Boolean registrationFeePaid) { this.registrationFeePaid = registrationFeePaid; }
    
    public Long getRegistrationFeeAmount() { return registrationFeeAmount; }
    public void setRegistrationFeeAmount(Long registrationFeeAmount) { this.registrationFeeAmount = registrationFeeAmount; }
    
    public String getRegistrationFeeReference() { return registrationFeeReference; }
    public void setRegistrationFeeReference(String registrationFeeReference) { this.registrationFeeReference = registrationFeeReference; }
    
    public String getAdmissionStatus() { return admissionStatus; }
    public void setAdmissionStatus(String admissionStatus) { this.admissionStatus = admissionStatus; }
    
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    
    public LocalDateTime getInterviewDate() { return interviewDate; }
    public void setInterviewDate(LocalDateTime interviewDate) { this.interviewDate = interviewDate; }
    
    public Double getInterviewScore() { return interviewScore; }
    public void setInterviewScore(Double interviewScore) { this.interviewScore = interviewScore; }
    
    public Double getAcademicScore() { return academicScore; }
    public void setAcademicScore(Double academicScore) { this.academicScore = academicScore; }
    
    public Double getTotalScore() { return totalScore; }
    public void setTotalScore(Double totalScore) { this.totalScore = totalScore; }
    
    public String getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }
    
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    @jakarta.persistence.PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}