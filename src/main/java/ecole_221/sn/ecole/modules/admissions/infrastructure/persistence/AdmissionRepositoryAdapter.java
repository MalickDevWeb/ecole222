package ecole_221.sn.ecole.modules.admissions.infrastructure.persistence;

import ecole_221.sn.ecole.modules.admissions.domain.exception.CandidateNotFoundException;
import ecole_221.sn.ecole.modules.admissions.domain.model.Candidate;
import ecole_221.sn.ecole.modules.admissions.domain.model.CandidateFactory;
import ecole_221.sn.ecole.modules.admissions.domain.port.repository.AdmissionRepositoryPort;
import ecole_221.sn.ecole.modules.admissions.domain.valueobject.CandidateStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptateur de repository pour le module Admissions.
 * Implémente AdmissionRepositoryPort en utilisant les repositories JPA existants.
 */
@Component
public class AdmissionRepositoryAdapter implements AdmissionRepositoryPort {
    
    private static final Logger logger = LoggerFactory.getLogger(AdmissionRepositoryAdapter.class);
    
    private final CandidateJpaRepository candidateJpaRepository;
    
    public AdmissionRepositoryAdapter(CandidateJpaRepository candidateJpaRepository) {
        this.candidateJpaRepository = candidateJpaRepository;
    }
    
    @Override
    public Candidate saveCandidate(Candidate candidate) {
        logger.debug("Sauvegarde du candidat: {}", candidate.getId());
        CandidateJpaEntity entity = toCandidateEntity(candidate);
        CandidateJpaEntity savedEntity = candidateJpaRepository.save(entity);
        return toCandidate(savedEntity);
    }
    
    @Override
    public Optional<Candidate> findCandidateById(String candidateId) {
        logger.debug("Récupération du candidat: {}", candidateId);
        return candidateJpaRepository.findById(candidateId)
            .map(this::toCandidate);
    }
    
    @Override
    public List<Candidate> findAllCandidates() {
        logger.debug("Récupération de tous les candidats depuis JPA");
        return candidateJpaRepository.findAll().stream()
            .map(this::toCandidate)
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteCandidate(String candidateId) {
        logger.debug("Suppression du candidat: {}", candidateId);
        if (!candidateJpaRepository.existsById(candidateId)) {
            throw new CandidateNotFoundException(candidateId);
        }
        candidateJpaRepository.deleteById(candidateId);
    }
    
    @Override
    public List<Candidate> findCandidatesByStatut(CandidateStatus statut) {
        logger.debug("Récupération des candidats par statut: {}", statut);
        return candidateJpaRepository.findByAdmissionStatus(statut.name()).stream()
            .map(this::toCandidate)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Candidate> findCandidatesByFiliere(String filiere) {
        logger.debug("Récupération des candidats par filière: {}", filiere);
        return candidateJpaRepository.findByCourseChoice(filiere).stream()
            .map(this::toCandidate)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Candidate> findAcceptedCandidates() {
        logger.debug("Récupération des candidats acceptés");
        return candidateJpaRepository.findByAdmissionStatus(CandidateStatus.ACCEPTED.name()).stream()
            .map(this::toCandidate)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Candidate> findRejectedCandidates() {
        logger.debug("Récupération des candidats rejetés");
        return candidateJpaRepository.findByAdmissionStatus(CandidateStatus.REJECTED.name()).stream()
            .map(this::toCandidate)
            .collect(Collectors.toList());
    }

    @Override
    public List<Candidate> findPendingCandidates() {
        logger.debug("Récupération des candidats en attente");
        return candidateJpaRepository.findByAdmissionStatus(CandidateStatus.EN_ATTENTE.name()).stream()
            .map(this::toCandidate)
            .collect(Collectors.toList());
    }
    
    // ==================== MAPPERS JPA ↔ DOMAIN ====================
    
    private Candidate toCandidate(CandidateJpaEntity entity) {
        return CandidateFactory.reconstruct(
            entity.getId(),
            entity.getLastName(),
            entity.getFirstName(),
            entity.getEmail(),
            entity.getPhone(),
            CandidateStatus.valueOf(entity.getAdmissionStatus()),
            entity.getTotalScore(),
            entity.getCourseChoice()
        );
    }

    private CandidateJpaEntity toCandidateEntity(Candidate candidate) {
        CandidateJpaEntity entity = new CandidateJpaEntity();
        entity.setId(candidate.getId());
        entity.setLastName(candidate.getNom());
        entity.setFirstName(candidate.getPrenom());
        entity.setEmail(candidate.getEmail());
        entity.setPhone(candidate.getTelephone());
        entity.setAdmissionStatus(candidate.getStatut().name());
        entity.setTotalScore(candidate.getNote());
        entity.setCourseChoice(candidate.getFiliereDemandee());
        return entity;
    }
}