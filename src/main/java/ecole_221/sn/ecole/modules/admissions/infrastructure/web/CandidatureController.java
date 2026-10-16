package ecole_221.sn.ecole.modules.admissions.infrastructure.web;

import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaEntity;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/candidatures")
@Tag(name = "Candidatures", description = "Soumission publique et consultation des candidatures")
public class CandidatureController {
    private static final Logger logger = LoggerFactory.getLogger(CandidatureController.class);
    private final CandidateJpaRepository candidateJpaRepository;

    public CandidatureController(CandidateJpaRepository candidateJpaRepository) {
        this.candidateJpaRepository = candidateJpaRepository;
    }

    @PostMapping("/submit")
    @Operation(summary = "Dépôt officiel candidature", description = "Enregistre une candidature directement en base PostgreSQL.")
    public ResponseEntity<Map<String, Object>> submitCandidature(@RequestBody Map<String, Object> body) {
        logger.info("POST /candidatures/submit - Réception nouvelle candidature");
        String id = body.containsKey("id") && body.get("id") != null 
            ? String.valueOf(body.get("id")) 
            : "CAND-2026-" + UUID.randomUUID().toString().substring(0, 8);
        String nom = String.valueOf(body.getOrDefault("nom", ""));
        String prenom = String.valueOf(body.getOrDefault("prenom", ""));
        String email = String.valueOf(body.getOrDefault("email", ""));
        String telephone = String.valueOf(body.getOrDefault("telephone", ""));
        String filiere = String.valueOf(body.getOrDefault("filiere", ""));
        boolean fraisPayes = Boolean.parseBoolean(String.valueOf(body.getOrDefault("fraisPayes", false)));
        String modePaiement = String.valueOf(body.getOrDefault("modePaiement", "WAVE"));
        String refPaiement = String.valueOf(body.getOrDefault("referencePaiement", ""));

        CandidateJpaEntity entity = new CandidateJpaEntity(id, prenom, nom, email, telephone, filiere);
        entity.setRegistrationFeePaid(fraisPayes);
        entity.setRegistrationFeeReference(refPaiement);
        entity.setIdCardUploaded(true);
        entity.setDiplomaUploaded(true);
        entity.setAdditionalInfo("{\"paymentMethod\":\"" + modePaiement + "\"}");
        entity.setSubmittedAt(LocalDateTime.now());
        entity.setCurrentStep("new");
        entity.setAdmissionStatus("pending");

        candidateJpaRepository.save(entity);
        logger.info("Candidature {} persistée avec succès en base PostgreSQL", id);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "id", id,
            "message", "Candidature enregistrée avec succès en base PostgreSQL"
        ));
    }

    @GetMapping("/settings")
    @Operation(summary = "Paramètres candidatures", description = "Vérifie si les candidatures sont ouvertes.")
    public ResponseEntity<Map<String, Object>> getSettings() {
        return ResponseEntity.ok(Map.of(
            "ouvert", true,
            "enabled", true,
            "session", "2026-2027",
            "dateCloture", "30 Octobre 2026"
        ));
    }
}

