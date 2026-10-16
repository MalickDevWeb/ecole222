package ecole_221.sn.ecole.modules.admin.infrastructure.web;

import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaEntity;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/candidatures")
@Tag(name = "Candidatures", description = "Gestion des paramètres et soumissions de candidature")
public class CandidaturesController {
    private static final Logger logger = LoggerFactory.getLogger(CandidaturesController.class);

    private final CandidateJpaRepository candidateJpaRepository;
    private static final Map<String, Object> settings = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>> candidatures = new ConcurrentHashMap<>();

    static {
        settings.put("ouvert", true);
        settings.put("dateOuverture", "2026-06-01");
        settings.put("dateFermeture", "2026-08-31");
        settings.put("messageAvis", "Les candidatures sont ouvertes");
        settings.put("showRegisterButton", true);
        Map<String, Boolean> docs = new HashMap<>();
        docs.put("cni", true);
        docs.put("bulletin", true);
        docs.put("diplome", true);
        docs.put("motivation", true);
        settings.put("docsRequis", docs);
    }

    public CandidaturesController() {
        this.candidateJpaRepository = null;
    }

    @Autowired
    public CandidaturesController(@Autowired(required = false) CandidateJpaRepository candidateJpaRepository) {
        this.candidateJpaRepository = candidateJpaRepository;
    }

    @GetMapping("/settings")
    @Operation(summary = "Obtenir paramètres candidature", description = "Retourne la configuration actuelle des admissions.")
    public ResponseEntity<Map<String, Object>> getSettings() {
        return ResponseEntity.ok(settings);
    }

    @PostMapping("/settings")
    @Operation(summary = "Mettre à jour paramètres candidature", description = "Met à jour la configuration des admissions.")
    public ResponseEntity<Map<String, Object>> updateSettings(@RequestBody Map<String, Object> body) {
        settings.putAll(body);
        logger.info("Settings candidatures mis à jour: {}", body);
        return ResponseEntity.ok(Map.of("success", true, "settings", settings));
    }

    @PostMapping("/submit")
    @Operation(summary = "Soumettre une candidature", description = "Soumet une nouvelle candidature d'admission avec persistance PostgreSQL.")
    public ResponseEntity<Map<String, Object>> submitCandidature(@RequestBody Map<String, Object> body) {
        String candidatureId = body.containsKey("id") && body.get("id") != null 
            ? String.valueOf(body.get("id")) 
            : "CAND-2026-" + UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> candidature = new HashMap<>(body);
        candidature.put("id", candidatureId);
        candidature.put("statut", "EN_ATTENTE");
        candidature.put("dateSoumission", LocalDateTime.now().toString());
        candidatures.put(candidatureId, candidature);

        if (candidateJpaRepository != null) {
            try {
                String nom = String.valueOf(body.getOrDefault("nom", ""));
                String prenom = String.valueOf(body.getOrDefault("prenom", ""));
                String email = String.valueOf(body.getOrDefault("email", ""));
                String telephone = String.valueOf(body.getOrDefault("telephone", ""));
                String filiere = String.valueOf(body.getOrDefault("filiere", ""));
                boolean fraisPayes = Boolean.parseBoolean(String.valueOf(body.getOrDefault("fraisPayes", false)));
                String modePaiement = String.valueOf(body.getOrDefault("modePaiement", "WAVE"));
                String refPaiement = String.valueOf(body.getOrDefault("referencePaiement", ""));

                CandidateJpaEntity entity = new CandidateJpaEntity(candidatureId, prenom, nom, email, telephone, filiere);
                entity.setRegistrationFeePaid(fraisPayes);
                entity.setRegistrationFeeReference(refPaiement);
                entity.setIdCardUploaded(true);
                entity.setDiplomaUploaded(true);
                entity.setAdditionalInfo("{\"paymentMethod\":\"" + modePaiement + "\"}");
                entity.setSubmittedAt(LocalDateTime.now());
                entity.setCurrentStep("new");
                entity.setAdmissionStatus("pending");

                candidateJpaRepository.save(entity);
                logger.info("Candidature {} persistée avec succès en PostgreSQL", candidatureId);
            } catch (Exception e) {
                logger.error("Erreur lors de la sauvegarde PostgreSQL de la candidature {}", candidatureId, e);
            }
        }
        
        logger.info("Nouvelle candidature soumise: {}", candidatureId);
        return ResponseEntity.ok(Map.of(
            "success", true, 
            "candidatureId", candidatureId,
            "id", candidatureId,
            "message", "Candidature soumise avec succès"
        ));
    }

    @GetMapping("/candidatures")
    @Operation(summary = "Liste des candidatures", description = "Retourne toutes les candidatures soumises.")
    public ResponseEntity<Map<String, Object>> getCandidatures() {
        List<Map<String, Object>> results = new ArrayList<>();
        if (candidateJpaRepository != null) {
            for (CandidateJpaEntity e : candidateJpaRepository.findAll()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", e.getId());
                m.put("nom", e.getLastName());
                m.put("prenom", e.getFirstName());
                m.put("email", e.getEmail());
                m.put("telephone", e.getPhone());
                m.put("filiere", e.getCourseChoice());
                m.put("statut", e.getAdmissionStatus());
                m.put("step", e.getCurrentStep());
                m.put("fraisPayes", e.getRegistrationFeePaid());
                results.add(m);
            }
        }
        if (results.isEmpty()) {
            results.addAll(candidatures.values());
        }
        return ResponseEntity.ok(Map.of("candidatures", results));
    }

    @GetMapping("/candidatures/{id}")
    @Operation(summary = "Détails d'une candidature", description = "Retourne les détails d'une candidature spécifique.")
    public ResponseEntity<Map<String, Object>> getCandidature(@PathVariable String id) {
        if (candidateJpaRepository != null) {
            Optional<CandidateJpaEntity> opt = candidateJpaRepository.findById(id);
            if (opt.isPresent()) {
                CandidateJpaEntity e = opt.get();
                Map<String, Object> m = new HashMap<>();
                m.put("id", e.getId());
                m.put("nom", e.getLastName());
                m.put("prenom", e.getFirstName());
                m.put("email", e.getEmail());
                m.put("telephone", e.getPhone());
                m.put("filiere", e.getCourseChoice());
                m.put("statut", e.getAdmissionStatus());
                return ResponseEntity.ok(m);
            }
        }
        Map<String, Object> candidature = candidatures.get(id);
        if (candidature == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(candidature);
    }

    @PatchMapping("/candidatures/{id}/statut")
    @Operation(summary = "Modifier statut candidature", description = "Met à jour le statut d'une candidature.")
    public ResponseEntity<Map<String, Object>> updateCandidatureStatut(
            @PathVariable String id, 
            @RequestBody Map<String, String> body) {
        String newStatut = body.get("statut");
        if (candidateJpaRepository != null) {
            candidateJpaRepository.findById(id).ifPresent(e -> {
                e.setAdmissionStatus(newStatut);
                candidateJpaRepository.save(e);
            });
        }
        Map<String, Object> candidature = candidatures.get(id);
        if (candidature != null) {
            candidature.put("statut", newStatut);
            candidatures.put(id, candidature);
        }
        logger.info("Statut candidature {} mis à jour: {}", id, newStatut);
        return ResponseEntity.ok(Map.of("success", true, "statut", newStatut));
    }
}
