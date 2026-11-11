package ecole_221.sn.ecole.modules.admissions.infrastructure.web;

import ecole_221.sn.ecole.modules.admissions.application.service.EtatsCandidature;

import ecole_221.sn.ecole.modules.admissions.application.port.dto.CandidateDto;
import ecole_221.sn.ecole.modules.admissions.application.port.usecase.AdmissionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.ElementPortailAdmissionJpaEntity;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.ElementPortailAdmissionJpaRepository;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/admissions", "/v1/admissions"})
@Tag(name = "Admissions", description = "Gestion des admissions - candidats, workflow, paramètres")
public class AdmissionController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdmissionController.class);
    private static final com.fasterxml.jackson.databind.ObjectMapper JSON = new com.fasterxml.jackson.databind.ObjectMapper();
    private final AdmissionUseCase admissionUseCase;
    private final ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaRepository candidateJpaRepository;
    
    private final ElementPortailAdmissionJpaRepository elementsPortail;
    /** Source UNIQUE des réglages de candidature (table parametres_candidatures) — APP-BE-15. */
    private final ecole_221.sn.ecole.modules.admin.application.service.ParametresCandidaturesService parametres;

    public AdmissionController(AdmissionUseCase admissionUseCase) {
        this(admissionUseCase, null, null, null);
    }

    public AdmissionController(AdmissionUseCase admissionUseCase,
                               ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaRepository candidateJpaRepository,
                               ElementPortailAdmissionJpaRepository elementsPortail) {
        this(admissionUseCase, candidateJpaRepository, elementsPortail, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AdmissionController(AdmissionUseCase admissionUseCase, 
                               ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaRepository candidateJpaRepository,
                               ElementPortailAdmissionJpaRepository elementsPortail,
                               ecole_221.sn.ecole.modules.admin.application.service.ParametresCandidaturesService parametres) {
        this.admissionUseCase = admissionUseCase;
        this.candidateJpaRepository = candidateJpaRepository;
        this.elementsPortail = elementsPortail;
        this.parametres = parametres;
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
    
    // APP-BE-15 : ces trois routes lisent et écrivent la MÊME source que /candidatures/settings (table
    // parametres_candidatures). Avant : une seconde table (admission_settings), qu'aucun écran n'utilisait, pouvait
    // annoncer publiquement « ouvert » alors que l'école avait fermé ses candidatures.

    @GetMapping("/settings")
    @Operation(summary = "Paramètres admissions", description = "Mêmes réglages que GET /candidatures/settings (source unique)")
    public ResponseEntity<Map<String, Object>> getAdmissionSettings() {
        return ResponseEntity.ok(reglages().lire());
    }

    @PutMapping("/settings")
    @Operation(summary = "Mettre à jour paramètres", description = "Mêmes réglages que POST /candidatures/settings (source unique)")
    public ResponseEntity<Map<String, Object>> updateAdmissionSettings(@RequestBody Map<String, Object> corps) {
        return ResponseEntity.ok(reglages().enregistrer(corps));
    }

    @PatchMapping("/settings/toggle")
    @Operation(summary = "Ouvrir/Fermer candidatures", description = "Corps {isOpen} ; même source que /candidatures/settings")
    public ResponseEntity<Map<String, Object>> toggleAdmissions(@RequestBody Map<String, Boolean> body) {
        boolean ouvert = Boolean.TRUE.equals(body == null ? null : body.get("isOpen"));
        reglages().definirOuverture(ouvert);
        return ResponseEntity.ok(Map.of("success", true, "isOpen", ouvert));
    }

    private ecole_221.sn.ecole.modules.admin.application.service.ParametresCandidaturesService reglages() {
        if (parametres == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Réglages de candidature indisponibles.");
        }
        return parametres;
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
        // Avant : « Invitations d'entretien envoyées avec succès » alors qu'aucune invitation n'est envoyée.
        int passes = admissionUseCase.sendInterviewInvitations();
        return ResponseEntity.ok(Map.of("success", true, "dossiersPassesEnEntretien", passes,
                "message", passes + " dossier(s) passé(s) à l'étape « entretien ». Aucune invitation n'est envoyée automatiquement : prévenez les candidats."));
    }
    
    @PostMapping("/automation/matricules")
    @Operation(summary = "Générer matricules", description = "Génère les matricules pour les candidats acceptés")
    public ResponseEntity<Map<String, Object>> generateMatricules() {
        // APP-BE-24 : la méthode calculait des matricules sans rien enregistrer ni créer de compte élève, puis
        // répondait « Matricules générées avec succès ». Inscription réelle des admis : à concevoir (décision).
        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_IMPLEMENTED,
                "Génération des matricules non disponible : l'inscription automatique des candidats admis n'est pas encore en place. "
                        + "Créez les comptes élèves depuis Gestion Élèves (création ou import).");
    }

    // ==================== ENDPOINT CANDIDATURE PORTAL DB ====================
    // Avant : quatre listes statiques en mémoire, communes à toutes les écoles (dont deux campagnes inventées
    // avec images Unsplash) et perdues au redémarrage. Désormais : candidats = table candidates, campagnes /
    // messages / journal = table admissions_portail_elements ; tout est cloisonné par école (@TenantId).

    private void exigerStockage() {
        if (candidateJpaRepository == null || elementsPortail == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Stockage des admissions indisponible.");
        }
    }

    private List<Map<String, Object>> lireElements(String nature) {
        List<Map<String, Object>> res = new java.util.ArrayList<>();
        for (ElementPortailAdmissionJpaEntity e : elementsPortail.findByNatureOrderByPositionAsc(nature)) {
            try {
                res.add(JSON.readValue(e.getContenuJson(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}));
            } catch (Exception ex) {
                logger.warn("Élément {} du portail illisible, ignoré", e.getId());
            }
        }
        return res;
    }

    /** Remplace la liste (campagnes, messages ou journal) de l'école courante. */
    private void remplacerElements(String nature, List<?> liste) {
        elementsPortail.deleteAll(elementsPortail.findByNatureOrderByPositionAsc(nature));
        int position = 0;
        for (Object item : liste) {
            ElementPortailAdmissionJpaEntity e = new ElementPortailAdmissionJpaEntity();
            e.setId(java.util.UUID.randomUUID().toString());
            e.setNature(nature);
            e.setPosition(position++);
            try {
                e.setContenuJson(JSON.writeValueAsString(item));
            } catch (Exception ex) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Élément illisible dans la liste « " + nature + " ».");
            }
            e.setUpdatedAt(java.time.LocalDateTime.now());
            elementsPortail.save(e);
        }
    }

    private static List<?> listeDeMaps(Map<String, Object> body, String cle) {
        Object v = body.get(cle);
        if (v == null) return null;
        if (!(v instanceof List<?> l)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "« " + cle + " » doit être une liste.");
        }
        for (Object o : l) {
            if (!(o instanceof Map<?, ?>)) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "« " + cle + " » ne doit contenir que des objets.");
            }
        }
        return l;
    }

    @GetMapping("/db")
    @Operation(summary = "Base admissions globale", description = "Retourne les campagnes, candidats, messages et audits synchronisés avec PostgreSQL")
    public ResponseEntity<Map<String, Object>> getAdmissionsDb() {
        java.util.List<Map<String, Object>> candidatesList = new java.util.ArrayList<>();
        java.util.List<ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaEntity> entities = 
            candidateJpaRepository != null ? candidateJpaRepository.findAll() : List.of();

        {
            for (ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaEntity e : entities) {
                Map<String, Object> c = new java.util.HashMap<>();
                c.put("id", e.getId());
                c.put("name", e.getFullName() != null ? e.getFullName() : (e.getFirstName() + " " + e.getLastName()).trim());
                c.put("email", e.getEmail() != null ? e.getEmail() : "");
                // plus de formation « Licence Informatique » par défaut : null si le candidat n'en a pas choisi
                c.put("course", blankToNull(e.getCourseChoice()));
                c.put("type", e.getCourseChoice() == null || e.getCourseChoice().isBlank() ? null
                        : (e.getCourseChoice().contains("Master") ? "MASTER" : "LICENCE"));
                c.put("step", e.getCurrentStep() != null ? e.getCurrentStep() : "new");
                c.put("registrationFeePaid", Boolean.TRUE.equals(e.getRegistrationFeePaid()));
                c.put("docs", Map.of(
                    "idCard", Boolean.TRUE.equals(e.getIdCardUploaded()),
                    "diploma", Boolean.TRUE.equals(e.getDiplomaUploaded()),
                    // APP-BE-04 : aucune donnée réelle sur les relevés → jamais « true » en dur
                    "transcripts", false
                ));
                Map<String, Object> details = new java.util.HashMap<>();
                details.put("cniNumber", null);   // APP-BE-17 : plus de numéro « CNI-<id> » inventé (non collecté en base)
                // APP-BE-04 : plus d'URL CNI fictive ni de statut « verified » inventé
                details.put("cniUrl", null);
                details.put("cniStatus", "unknown");
                // APP-BE-04 : moyen et référence de paiement réels uniquement, sinon null (jamais de fausse transaction)
                details.put("paymentMethod", readPaymentMethod(e.getAdditionalInfo()));
                details.put("paymentTransactionRef", blankToNull(e.getRegistrationFeeReference()));
                details.put("paymentStatus", Boolean.TRUE.equals(e.getRegistrationFeePaid()) ? "approved" : "pending");
                c.put("details", details);
                candidatesList.add(c);
            }
        }

        boolean stockage = elementsPortail != null;
        return ResponseEntity.ok(Map.of(
            "candidates", candidatesList,
            "campaigns", stockage ? lireElements(ElementPortailAdmissionJpaEntity.CAMPAGNE) : List.of(),
            "auditLogs", stockage ? lireElements(ElementPortailAdmissionJpaEntity.AUDIT) : List.of(),
            "messages", stockage ? lireElements(ElementPortailAdmissionJpaEntity.MESSAGE) : List.of()
        ));
    }

    /** Lit la clé « paymentMethod » du JSON additionalInfo ; null si absente, vide ou JSON illisible. */
    static String readPaymentMethod(String additionalInfo) {
        if (additionalInfo == null || additionalInfo.isBlank()) {
            return null;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode node = JSON.readTree(additionalInfo).get("paymentMethod");
            if (node == null || node.isNull() || !node.isValueNode()) {
                return null;
            }
            return blankToNull(node.asText());
        } catch (Exception ex) {
            logger.warn("additionalInfo illisible pour la lecture du moyen de paiement");
            return null;
        }
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    @PostMapping("/db")
    @Operation(summary = "Mettre à jour la base admissions", description = "Synchronise les modifications du portail candidat avec PostgreSQL")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Map<String, Object>> saveAdmissionsDb(@RequestBody Map<String, Object> body) {
        exigerStockage();
        if (body == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Corps de requête manquant.");
        }
        List<?> candidats = listeDeMaps(body, "candidates");
        List<?> campagnes = listeDeMaps(body, "campaigns");
        List<?> journal = listeDeMaps(body, "auditLogs");
        List<?> messages = listeDeMaps(body, "messages");

        // Validation AVANT toute écriture : pas d'enregistrement partiel, pas de formation par défaut.
        if (candidats != null) {
            for (Object item : candidats) {
                Map<?, ?> m = (Map<?, ?>) item;
                String id = texte(m.get("id"));
                if (id == null) {
                    throw new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.BAD_REQUEST, "Candidat sans identifiant.");
                }
                if (texte(m.get("course")) == null) {
                    throw new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.BAD_REQUEST, "Formation demandée manquante pour le candidat " + id + ".");
                }
                if (texte(m.get("name")) == null) {
                    throw new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.BAD_REQUEST, "Nom manquant pour le candidat " + id + ".");
                }
            }
            for (Object item : candidats) {
                Map<?, ?> map = (Map<?, ?>) item;
                String id = texte(map.get("id"));
                String name = texte(map.get("name"));
                String email = texte(map.get("email"));
                String course = texte(map.get("course"));
                String step = texte(map.get("step"));
                boolean paid = Boolean.parseBoolean(String.valueOf(map.get("registrationFeePaid")));

                ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaEntity entity =
                    candidateJpaRepository.findById(id).orElseGet(() -> {
                        ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaEntity ne =
                            new ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaEntity();
                        ne.setId(id);
                        return ne;
                    });

                entity.setFullName(name);
                String[] parts = name.split("\\s+", 2);
                entity.setFirstName(parts[0]);
                entity.setLastName(parts.length > 1 ? parts[1] : parts[0]);
                entity.setEmail(email);
                entity.setCourseChoice(course);
                if (step != null && !step.equals(entity.getCurrentStep())) {
                    entity.setCurrentStep(EtatsCandidature.etape(step));
                    EtatsCandidature.apresEtape(entity);   // la décision suit l'étape du Kanban (APP-BE-24)
                }
                entity.setRegistrationFeePaid(paid);
                entity.setUpdatedAt(java.time.LocalDateTime.now());
                candidateJpaRepository.save(entity);
            }
        }
        if (campagnes != null) remplacerElements(ElementPortailAdmissionJpaEntity.CAMPAGNE, campagnes);
        if (journal != null) remplacerElements(ElementPortailAdmissionJpaEntity.AUDIT, journal);
        if (messages != null) remplacerElements(ElementPortailAdmissionJpaEntity.MESSAGE, messages);
        return ResponseEntity.ok(Map.of("success", true));
    }

    private static String texte(Object v) {
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
