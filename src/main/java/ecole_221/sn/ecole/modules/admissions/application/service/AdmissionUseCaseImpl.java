package ecole_221.sn.ecole.modules.admissions.application.service;

import ecole_221.sn.ecole.modules.admissions.application.port.dto.CandidateDto;
import ecole_221.sn.ecole.modules.admissions.application.port.dto.AdmissionSettingsDto;
import ecole_221.sn.ecole.modules.admissions.application.port.usecase.AdmissionUseCase;
import ecole_221.sn.ecole.modules.admissions.domain.model.Candidate;
import ecole_221.sn.ecole.modules.admissions.domain.model.CandidateFactory;
import ecole_221.sn.ecole.modules.admissions.domain.port.repository.AdmissionRepositoryPort;
import ecole_221.sn.ecole.modules.admissions.domain.valueobject.CandidateStatus;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdmissionUseCaseImpl implements AdmissionUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(AdmissionUseCaseImpl.class);
    
    private final AdmissionRepositoryPort admissionRepositoryPort;
    private final CandidateJpaRepository candidateRepository;
    private final AdmissionSettingsJpaRepository settingsRepository;
    
    public AdmissionUseCaseImpl(AdmissionRepositoryPort admissionRepositoryPort,
                                CandidateJpaRepository candidateRepository,
                                AdmissionSettingsJpaRepository settingsRepository) {
        this.admissionRepositoryPort = admissionRepositoryPort;
        this.candidateRepository = candidateRepository;
        this.settingsRepository = settingsRepository;
    }
    
    // ==================== GESTION DES CANDIDATS ====================
    
    @Override
    public List<CandidateDto> getAllCandidates() {
        logger.info("Récupération de tous les candidats");
        return admissionRepositoryPort.findAllCandidates().stream()
            .map(this::toCandidateDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public CandidateDto getCandidateById(String id) {
        logger.info("Récupération du candidat: {}", id);
        return admissionRepositoryPort.findCandidateById(id)
            .map(this::toCandidateDto)
            .orElse(null);
    }
    
    @Override
    public List<CandidateDto> getCandidatesByStep(String step) {
        logger.info("Récupération des candidats par étape: {}", step);
        return candidateRepository.findByCurrentStep(step).stream()
            .map(this::toCandidateDtoFromEntity)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<CandidateDto> getCandidatesByStatus(String status) {
        logger.info("Récupération des candidats par statut: {}", status);
        return candidateRepository.findByAdmissionStatus(status).stream()
            .map(this::toCandidateDtoFromEntity)
            .collect(Collectors.toList());
    }
    
    @Override
    public CandidateDto createCandidate(CandidateDto candidateDto) {
        logger.info("Création candidat: {}", candidateDto.nom());
        Candidate candidate = toCandidate(candidateDto);
        Candidate savedCandidate = admissionRepositoryPort.saveCandidate(candidate);
        return toCandidateDto(savedCandidate);
    }
    
    @Override
    public CandidateDto updateCandidate(String id, CandidateDto candidateDto) {
        logger.info("Mise à jour candidat: {}", id);
        Candidate candidate = toCandidate(candidateDto);
        // Use factory reconstruct to create Candidate with existing ID
        Candidate updatedCandidate = CandidateFactory.reconstruct(
            id,
            candidateDto.nom(),
            candidateDto.prenom(),
            candidateDto.email(),
            candidateDto.telephone(),
            CandidateStatus.from(candidateDto.statut()),
            candidateDto.note(),
            candidateDto.filiereDemandee()
        );
        Candidate savedCandidate = admissionRepositoryPort.saveCandidate(updatedCandidate);
        return toCandidateDto(savedCandidate);
    }
    
    @Override
    public void deleteCandidate(String id) {
        logger.info("Suppression candidat: {}", id);
        admissionRepositoryPort.deleteCandidate(id);
    }
    
    @Override
    public CandidateDto updateCandidateStep(String id, String step) {
        // Avant : l'étape (« docs », « interview »…) était rangée dans le STATUT, d'où un refus pour toute vraie étape.
        String etape = EtatsCandidature.etape(step);
        CandidateJpaEntity c = dossier(id);
        c.setCurrentStep(etape);
        EtatsCandidature.apresEtape(c);
        candidateRepository.save(c);
        return relire(id);
    }
    
    @Override
    public CandidateDto updateCandidateStatus(String id, String status, String rejectionReason) {
        CandidateStatus statut = CandidateStatus.from(status);   // valeur inconnue → 400
        CandidateJpaEntity c = dossier(id);
        c.setAdmissionStatus(statut.name());
        // Avant : le motif de refus reçu était ignoré
        c.setRejectionReason(statut == CandidateStatus.REJECTED && rejectionReason != null && !rejectionReason.isBlank()
                ? rejectionReason.trim() : null);
        EtatsCandidature.apresDecision(c);
        candidateRepository.save(c);
        return relire(id);
    }

    private CandidateJpaEntity dossier(String id) {
        return candidateRepository.findById(id == null ? "" : id).orElseThrow(() ->
                new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,
                        "Candidature introuvable : " + id));
    }

    private CandidateDto relire(String id) {
        return admissionRepositoryPort.findCandidateById(id).map(this::toCandidateDto).orElse(null);
    }
    
    // ==================== GESTION DES PARAMÈTRES ====================
    
    @Override
    public AdmissionSettingsDto getAdmissionSettings() {
        logger.info("Récupération des paramètres d'admission");
        AdmissionSettingsJpaEntity entity = settingsRepository.findActiveAdmissionSettings().orElse(null);
        if (entity != null) {
            return toAdmissionSettingsDto(entity);
        }
        return null;
    }
    
    @Override
    public AdmissionSettingsDto updateAdmissionSettings(AdmissionSettingsDto settingsDto) {
        logger.info("Mise à jour paramètres admissions");
        AdmissionSettingsJpaEntity entity = toAdmissionSettingsEntity(settingsDto);
        AdmissionSettingsJpaEntity savedEntity = settingsRepository.save(entity);
        return toAdmissionSettingsDto(savedEntity);
    }
    
    @Override
    public void toggleAdmissions(boolean isOpen) {
        logger.info("Toggle admissions: {}", isOpen);
        AdmissionSettingsJpaEntity settings = settingsRepository.findActiveAdmissionSettings().orElse(null);
        if (settings != null) {
            settings.setIsOpen(isOpen);
            settingsRepository.save(settings);
            logger.info("Candidatures {}: {}", isOpen ? "ouvertes" : "fermées", isOpen);
        }
    }
    
    // ==================== WORKFLOW ====================
    
    @Override
    public List<Map<String, Object>> getKanbanData() {
        logger.info("Récupération des données Kanban");
        List<Map<String, Object>> kanbanData = new ArrayList<>();
        String[] steps = {"new", "docs", "interview", "admitted", "rejected"};
        
        for (String step : steps) {
            List<CandidateJpaEntity> candidates = candidateRepository.findByCurrentStepOrderBySubmittedAtDesc(step);
            Map<String, Object> column = new HashMap<>();
            column.put("step", step);
            column.put("candidates", candidates);
            column.put("count", candidates.size());
            kanbanData.add(column);
        }
        
        return kanbanData;
    }
    
    @Override
    public Map<String, Object> getCandidateStatistics() {
        logger.info("Récupération des statistiques candidats");
        Map<String, Object> stats = new HashMap<>();
        
        List<CandidateJpaEntity> allCandidates = candidateRepository.findAll();
        
        stats.put("total", allCandidates.size());
        stats.put("new", candidateRepository.countByAdmissionStatus(CandidateStatus.EN_ATTENTE.name()));
        stats.put("accepted", candidateRepository.countByAdmissionStatus(CandidateStatus.ACCEPTED.name()));
        stats.put("rejected", candidateRepository.countByAdmissionStatus(CandidateStatus.REJECTED.name()));
        stats.put("inProgress", candidateRepository.countByAdmissionStatus(CandidateStatus.EN_COURS.name()));
        stats.put("finalized", candidateRepository.countByAdmissionStatus(CandidateStatus.FINALISE.name()));
        stats.put("paid", candidateRepository.findPaidPendingCandidates().size());
        
        // Par cours
        Map<String, Long> byCourse = allCandidates.stream()
            .collect(Collectors.groupingBy(CandidateJpaEntity::getCourseChoice, Collectors.counting()));
        stats.put("byCourse", byCourse);
        
        return stats;
    }
    
    @Override
    public List<CandidateDto> getTopCandidates(int limit) {
        logger.info("Récupération des {} meilleurs candidats", limit);
        return candidateRepository.findTopCandidatesByScore(10.0).stream()
            .limit(limit)
            .map(this::toCandidateDtoFromEntity)
            .collect(Collectors.toList());
    }
    
    // ==================== AUTOMATION ====================
    
    @Override
    public void processAdmissions() {
        logger.info("Traitement automatique des admissions");
        
        // Logique de traitement automatique basée sur les scores
        List<CandidateJpaEntity> pendingCandidates = candidateRepository.findByAdmissionStatus(CandidateStatus.EN_ATTENTE.name());
        
        for (CandidateJpaEntity candidate : pendingCandidates) {
            if (candidate.getTotalScore() != null && candidate.getTotalScore() >= 12.0) {
                candidate.setAdmissionStatus(CandidateStatus.ACCEPTED.name());
                EtatsCandidature.apresDecision(candidate);
                candidateRepository.save(candidate);
                logger.info("Candidat accepté automatiquement: {}", candidate.getFullName());
            }
        }
    }
    
    @Override
    public int sendInterviewInvitations() {
        logger.info("Envoi des invitations d'entretien");
        
        List<CandidateJpaEntity> readyForInterview = candidateRepository.findByCurrentStep("docs");
        int passes = 0;
        
        for (CandidateJpaEntity candidate : readyForInterview) {
            if (candidate.getDiplomaUploaded() && candidate.getIdCardUploaded()) {
                candidate.setCurrentStep("interview");
                EtatsCandidature.apresEtape(candidate);
                candidate.setInterviewDate(LocalDateTime.now().plusDays(7));
                candidateRepository.save(candidate);
                passes++;
            }
        }
        return passes;
    }
    
    
    // ==================== MAPPERS ====================
    
    private CandidateDto toCandidateDto(Candidate candidate) {
        return new CandidateDto(
            candidate.getId(),
            candidate.getNom(),
            candidate.getPrenom(),
            candidate.getEmail(),
            candidate.getTelephone(),
            candidate.getStatut().name(),
            candidate.getNote(),
            candidate.getFiliereDemandee()
        );
    }
    
    private Candidate toCandidate(CandidateDto dto) {
        // Use CandidateFactory to create/reconstruct Candidate instances
        if (dto.id() != null && !dto.id().isEmpty()) {
            return CandidateFactory.reconstruct(
                dto.id(),
                dto.nom(),
                dto.prenom(),
                dto.email(),
                dto.telephone(),
                CandidateStatus.from(dto.statut()),
                dto.note(),
                dto.filiereDemandee()
            );
        } else {
            return CandidateFactory.create(
                dto.nom(),
                dto.prenom(),
                dto.email(),
                dto.telephone(),
                CandidateStatus.from(dto.statut()),
                dto.note(),
                dto.filiereDemandee()
            );
        }
    }
    
    private AdmissionSettingsDto toAdmissionSettingsDto(AdmissionSettingsJpaEntity entity) {
        return new AdmissionSettingsDto(
            entity.getId(),
            entity.getOpeningDate() != null ? entity.getOpeningDate().toLocalDate().toString() : null,
            entity.getClosingDate() != null ? entity.getClosingDate().toLocalDate().toString() : null,
            entity.getMinAcademicScore(),
            entity.getMaxCandidates() != null ? entity.getMaxCandidates() : 0,
            entity.getRequiredDocuments(),
            entity.getIsOpen() ? "OUVERT" : "FERME"
        );
    }
    
    private AdmissionSettingsJpaEntity toAdmissionSettingsEntity(AdmissionSettingsDto dto) {
        AdmissionSettingsJpaEntity entity = new AdmissionSettingsJpaEntity();
        entity.setId(dto.id());
        if (dto.startDate() != null) {
            entity.setOpeningDate(java.time.LocalDateTime.parse(dto.startDate() + "T00:00:00"));
        }
        if (dto.endDate() != null) {
            entity.setClosingDate(java.time.LocalDateTime.parse(dto.endDate() + "T23:59:59"));
        }
        entity.setMinAcademicScore(dto.minNote());
        entity.setMaxCandidates(dto.placesDisponibles());
        entity.setRequiredDocuments(dto.documentsRequis());
        entity.setIsOpen("OUVERT".equals(dto.statut()));
        return entity;
    }
    
    // Mapper pour conversion directe depuis CandidateJpaEntity
    private CandidateDto toCandidateDtoFromEntity(CandidateJpaEntity entity) {
        // Convert JPA entity status String to CandidateStatus then to String for DTO
        String status = entity.getAdmissionStatus();
        return new CandidateDto(
            entity.getId(),
            entity.getLastName(),
            entity.getFirstName(),
            entity.getEmail(),
            entity.getPhone(),
            status,
            entity.getTotalScore(),
            entity.getCourseChoice()
        );
    }
}