package ecole_221.sn.ecole.modules.admissions.application.port.usecase;

import ecole_221.sn.ecole.modules.admissions.application.port.dto.CandidateDto;
import ecole_221.sn.ecole.modules.admissions.application.port.dto.AdmissionSettingsDto;
import ecole_221.sn.ecole.partage.PageResult;

import java.util.List;
import java.util.Map;

/**
 * Interface des cas d'utilisation pour le module Admissions.
 * Gère la gestion des candidats, paramètres d'admission et workflow.
 */
public interface AdmissionUseCase {
    
    // ==================== GESTION DES CANDIDATS ====================
    
    /**
     * Récupère tous les candidats.
     */
    List<CandidateDto> getAllCandidates();
    
    /**
     * Récupère un candidat par son identifiant.
     */
    CandidateDto getCandidateById(String id);
    
    /**
     * Récupère les candidats par étape du processus.
     */
    List<CandidateDto> getCandidatesByStep(String step);
    
    /**
     * Récupère les candidats par statut.
     */
    List<CandidateDto> getCandidatesByStatus(String status);
    
    /**
     * Crée un nouveau candidat.
     */
    CandidateDto createCandidate(CandidateDto candidate);
    
    /**
     * Met à jour un candidat existant.
     */
    CandidateDto updateCandidate(String id, CandidateDto candidate);
    
    /**
     * Supprime un candidat.
     */
    void deleteCandidate(String id);
    
    /**
     * Met à jour l'étape d'un candidat.
     */
    CandidateDto updateCandidateStep(String id, String step);
    
    /**
     * Met à jour le statut d'un candidat.
     */
    CandidateDto updateCandidateStatus(String id, String status, String rejectionReason);
    
    // ==================== GESTION DES PARAMÈTRES ====================
    
    /**
     * Récupère les paramètres d'admission.
     */
    AdmissionSettingsDto getAdmissionSettings();
    
    /**
     * Met à jour les paramètres d'admission.
     */
    AdmissionSettingsDto updateAdmissionSettings(AdmissionSettingsDto settings);
    
    /**
     * Active ou désactive les admissions.
     */
    void toggleAdmissions(boolean isOpen);
    
    // ==================== WORKFLOW ====================
    
    /**
     * Récupère les données Kanban pour le workflow.
     */
    List<Map<String, Object>> getKanbanData();
    
    /**
     * Récupère les statistiques des candidats.
     */
    Map<String, Object> getCandidateStatistics();
    
    /**
     * Récupère les meilleurs candidats.
     */
    List<CandidateDto> getTopCandidates(int limit);
    
    // ==================== AUTOMATION ====================
    
    /**
     * Traite automatiquement les admissions.
     */
    void processAdmissions();
    
    /**
     * Envoie les invitations aux entretiens.
     */
    void sendInterviewInvitations();
    
    /**
     * Génère les matricules pour les candidats acceptés.
     */
    void generateMatricules();
}