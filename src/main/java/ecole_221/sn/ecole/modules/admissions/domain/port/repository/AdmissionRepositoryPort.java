package ecole_221.sn.ecole.modules.admissions.domain.port.repository;

import ecole_221.sn.ecole.modules.admissions.domain.model.Candidate;
import ecole_221.sn.ecole.modules.admissions.domain.valueobject.CandidateStatus;

import java.util.List;
import java.util.Optional;

/**
 * Output Port pour la persistance des candidats.
 * Définit les opérations de persistance pour l'agrégat Candidate.
 */
public interface AdmissionRepositoryPort {
    
    /**
     * Sauvegarde un candidat (création ou modification).
     */
    Candidate saveCandidate(Candidate candidate);
    
    /**
     * Récupère un candidat par son identifiant.
     */
    Optional<Candidate> findCandidateById(String candidateId);
    
    /**
     * Récupère tous les candidats.
     */
    List<Candidate> findAllCandidates();
    
    /**
     * Supprime un candidat par son ID.
     */
    void deleteCandidate(String candidateId);
    
    /**
     * Récupère les candidats par statut.
     */
    List<Candidate> findCandidatesByStatut(CandidateStatus statut);
    
    /**
     * Récupère les candidats par filière demandée.
     */
    List<Candidate> findCandidatesByFiliere(String filiere);
    
    /**
     * Récupère les candidats acceptés.
     */
    List<Candidate> findAcceptedCandidates();
    
    /**
     * Récupère les candidats rejetés.
     */
    List<Candidate> findRejectedCandidates();
    
    /**
     * Récupère les candidats en attente.
     */
    List<Candidate> findPendingCandidates();
}