package ecole_221.sn.ecole.modules.admin.infrastructure.web;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/candidatures")
@Tag(name = "Candidatures", description = "Gestion des paramètres de candidature")
public class CandidaturesController {
    private static final Logger logger = LoggerFactory.getLogger(CandidaturesController.class);

    // Simule une base de données en mémoire pour l'instant
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
    @Operation(summary = "Soumettre une candidature", description = "Soumet une nouvelle candidature d'admission.")
    public ResponseEntity<Map<String, Object>> submitCandidature(@RequestBody Map<String, Object> body) {
        String candidatureId = "cand-" + System.currentTimeMillis();
        Map<String, Object> candidature = new HashMap<>(body);
        candidature.put("id", candidatureId);
        candidature.put("statut", "EN_ATTENTE");
        candidature.put("dateSoumission", java.time.LocalDateTime.now().toString());
        candidatures.put(candidatureId, candidature);
        
        logger.info("Nouvelle candidature soumise: {}", candidatureId);
        return ResponseEntity.ok(Map.of(
            "success", true, 
            "candidatureId", candidatureId,
            "message", "Candidature soumise avec succès"
        ));
    }

    @GetMapping("/candidatures")
    @Operation(summary = "Liste des candidatures", description = "Retourne toutes les candidatures soumisses.")
    public ResponseEntity<Map<String, Object>> getCandidatures() {
        return ResponseEntity.ok(Map.of("candidatures", candidatures.values()));
    }

    @GetMapping("/candidatures/{id}")
    @Operation(summary = "Détails d'une candidature", description = "Retourne les détails d'une candidature spécifique.")
    public ResponseEntity<Map<String, Object>> getCandidature(@PathVariable String id) {
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
        Map<String, Object> candidature = candidatures.get(id);
        if (candidature == null) {
            return ResponseEntity.notFound().build();
        }
        candidature.put("statut", body.get("statut"));
        candidatures.put(id, candidature);
        logger.info("Statut candidature {} mis à jour: {}", id, body.get("statut"));
        return ResponseEntity.ok(Map.of("success", true, "statut", body.get("statut")));
    }
}
