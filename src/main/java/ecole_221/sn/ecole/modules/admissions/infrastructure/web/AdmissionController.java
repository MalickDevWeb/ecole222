package ecole_221.sn.ecole.modules.admissions.infrastructure.web;

import ecole_221.sn.ecole.modules.admissions.application.port.dto.CandidateDto;
import ecole_221.sn.ecole.modules.admissions.application.port.dto.AdmissionSettingsDto;
import ecole_221.sn.ecole.modules.admissions.application.port.usecase.AdmissionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admissions")
@CrossOrigin(origins = "*")
@Tag(name = "Admissions", description = "Gestion des admissions - candidats, workflow, paramètres")
public class AdmissionController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdmissionController.class);
    private final AdmissionUseCase admissionUseCase;
    
    public AdmissionController(AdmissionUseCase admissionUseCase) {
        this.admissionUseCase = admissionUseCase;
    }
    
    // ==================== ENDPOINTS CANDIDATS ====================
    
    @GetMapping("/candidates")
    @Operation(summary = "Liste des candidats", description = "Retourne tous les candidats")
    public ResponseEntity<List<CandidateDto>> getAllCandidates() {
        logger.info("GET /admissions/candidates - Récupération candidats");
        return ResponseEntity.ok(admissionUseCase.getAllCandidates());
    }
    
    @GetMapping("/candidates/{id}")
    @Operation(summary = "Détails candidat", description = "Retourne les détails d'un candidat")
    public ResponseEntity<CandidateDto> getCandidate(@PathVariable String id) {
        logger.info("GET /admissions/candidates/{} - Récupération candidat", id);
        CandidateDto candidate = admissionUseCase.getCandidateById(id);
        if (candidate == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(candidate);
    }
    
    @GetMapping("/candidates/step/{step}")
    @Operation(summary = "Candidats par étape", description = "Retourne les candidats par étape du workflow")
    public ResponseEntity<List<CandidateDto>> getCandidatesByStep(@PathVariable String step) {
        logger.info("GET /admissions/candidates/step/{} - Récupération candidats par étape", step);
        return ResponseEntity.ok(admissionUseCase.getCandidatesByStep(step));
    }
    
    @GetMapping("/candidates/status/{status}")
    @Operation(summary = "Candidats par statut", description = "Retourne les candidats par statut d'admission")
    public ResponseEntity<List<CandidateDto>> getCandidatesByStatus(@PathVariable String status) {
        logger.info("GET /admissions/candidates/status/{} - Récupération candidats par statut", status);
        return ResponseEntity.ok(admissionUseCase.getCandidatesByStatus(status));
    }
    
    @PostMapping("/candidates")
    @Operation(summary = "Créer candidat", description = "Crée un nouveau candidat")
    public ResponseEntity<CandidateDto> createCandidate(@RequestBody CandidateDto candidate) {
        logger.info("POST /admissions/candidates - Création candidat");
        return ResponseEntity.ok(admissionUseCase.createCandidate(candidate));
    }
    
    @PutMapping("/candidates/{id}")
    @Operation(summary = "Mettre à jour candidat", description = "Met à jour les informations d'un candidat")
    public ResponseEntity<CandidateDto> updateCandidate(@PathVariable String id, 
                                                        @RequestBody CandidateDto candidate) {
        logger.info("PUT /admissions/candidates/{} - Mise à jour candidat", id);
        return ResponseEntity.ok(admissionUseCase.updateCandidate(id, candidate));
    }
    
    @DeleteMapping("/candidates/{id}")
    @Operation(summary = "Supprimer candidat", description = "Supprime un candidat")
    public ResponseEntity<Void> deleteCandidate(@PathVariable String id) {
        logger.info("DELETE /admissions/candidates/{} - Suppression candidat", id);
        admissionUseCase.deleteCandidate(id);
        return ResponseEntity.ok().build();
    }
    
    @PatchMapping("/candidates/{id}/step")
    @Operation(summary = "Mettre à jour étape candidat", description = "Met à jour l'étape du workflow d'un candidat")
    public ResponseEntity<CandidateDto> updateCandidateStep(@PathVariable String id, 
                                                             @RequestBody Map<String, String> body) {
        logger.info("PATCH /admissions/candidates/{}/step - Mise à jour étape", id);
        return ResponseEntity.ok(admissionUseCase.updateCandidateStep(id, body.get("step")));
    }
    
    @PatchMapping("/candidates/{id}/status")
    @Operation(summary = "Mettre à jour statut admission", description = "Met à jour le statut d'admission d'un candidat")
    public ResponseEntity<CandidateDto> updateCandidateStatus(@PathVariable String id, 
                                                               @RequestBody Map<String, String> body) {
        logger.info("PATCH /admissions/candidates/{}/status - Mise à jour statut admission", id);
        return ResponseEntity.ok(admissionUseCase.updateCandidateStatus(id, body.get("status"), body.get("rejectionReason")));
    }
    
    // ==================== ENDPOINTS PARAMÈTRES ====================
    
    @GetMapping("/settings")
    @Operation(summary = "Paramètres admissions", description = "Retourne les paramètres des admissions")
    public ResponseEntity<AdmissionSettingsDto> getAdmissionSettings() {
        logger.info("GET /admissions/settings - Récupération paramètres admissions");
        AdmissionSettingsDto settings = admissionUseCase.getAdmissionSettings();
        if (settings == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(settings);
    }
    
    @PutMapping("/settings")
    @Operation(summary = "Mettre à jour paramètres", description = "Met à jour les paramètres des admissions")
    public ResponseEntity<AdmissionSettingsDto> updateAdmissionSettings(@RequestBody AdmissionSettingsDto settings) {
        logger.info("PUT /admissions/settings - Mise à jour paramètres admissions");
        return ResponseEntity.ok(admissionUseCase.updateAdmissionSettings(settings));
    }
    
    @PatchMapping("/settings/toggle")
    @Operation(summary = "Ouvrir/Fermer candidatures", description = "Active ou désactive le système de candidature")
    public ResponseEntity<Map<String, Object>> toggleAdmissions(@RequestBody Map<String, Boolean> body) {
        logger.info("PATCH /admissions/settings/toggle - Toggle candidatures");
        admissionUseCase.toggleAdmissions(body.get("isOpen"));
        return ResponseEntity.ok(Map.of("success", true, "isOpen", body.get("isOpen")));
    }
    
    // ==================== ENDPOINTS WORKFLOW ====================
    
    @GetMapping("/kanban")
    @Operation(summary = "Données Kanban", description = "Retourne les données organisées par colonnes Kanban")
    public ResponseEntity<List<Map<String, Object>>> getKanbanData() {
        logger.info("GET /admissions/kanban - Récupération données Kanban");
        return ResponseEntity.ok(admissionUseCase.getKanbanData());
    }
    
    @GetMapping("/statistics")
    @Operation(summary = "Statistiques candidats", description = "Retourne les statistiques des candidatures")
    public ResponseEntity<Map<String, Object>> getCandidateStatistics() {
        logger.info("GET /admissions/statistics - Récupération statistiques candidats");
        return ResponseEntity.ok(admissionUseCase.getCandidateStatistics());
    }
    
    @GetMapping("/top-candidates")
    @Operation(summary = "Meilleurs candidats", description = "Retourne les meilleurs candidats par score")
    public ResponseEntity<List<CandidateDto>> getTopCandidates(@RequestParam(defaultValue = "10") int limit) {
        logger.info("GET /admissions/top-candidates - Récupération meilleurs candidats");
        return ResponseEntity.ok(admissionUseCase.getTopCandidates(limit));
    }
    
    // ==================== ENDPOINTS AUTOMATION ====================
    
    @PostMapping("/automation/process")
    @Operation(summary = "Traiter admissions", description = "Traite automatiquement les admissions")
    public ResponseEntity<Map<String, Object>> processAdmissions() {
        logger.info("POST /admissions/automation/process - Traitement admissions");
        admissionUseCase.processAdmissions();
        return ResponseEntity.ok(Map.of("success", true, "message", "Admissions traitées avec succès"));
    }
    
    @PostMapping("/automation/interviews")
    @Operation(summary = "Envoyer invitations entretien", description = "Envoie les invitations d'entretien")
    public ResponseEntity<Map<String, Object>> sendInterviewInvitations() {
        logger.info("POST /admissions/automation/interviews - Envoi invitations entretien");
        admissionUseCase.sendInterviewInvitations();
        return ResponseEntity.ok(Map.of("success", true, "message", "Invitations d'entretien envoyées avec succès"));
    }
    
    @PostMapping("/automation/matricules")
    @Operation(summary = "Générer matricules", description = "Génère les matricules pour les candidats acceptés")
    public ResponseEntity<Map<String, Object>> generateMatricules() {
        logger.info("POST /admissions/automation/matricules - Génération matricules");
        admissionUseCase.generateMatricules();
        return ResponseEntity.ok(Map.of("success", true, "message", "Matricules générées avec succès"));
    }
}