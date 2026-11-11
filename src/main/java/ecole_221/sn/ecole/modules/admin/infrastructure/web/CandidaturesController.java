package ecole_221.sn.ecole.modules.admin.infrastructure.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ecole_221.sn.ecole.modules.admissions.domain.valueobject.CandidateStatus;
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
import ecole_221.sn.ecole.modules.admin.application.service.ParametresCandidaturesService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/candidatures")
@Tag(name = "Candidatures", description = "Gestion des paramètres et soumissions de candidature")
public class CandidaturesController {
    private static final Logger logger = LoggerFactory.getLogger(CandidaturesController.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final CandidateJpaRepository candidateJpaRepository;
    /**
     * Paramètres persistés par école (table parametres_candidatures). Avant : map statique commune à toutes
     * les écoles, pré-remplie d'une campagne « ouverte du 2026-06-01 au 2026-08-31 » inventée.
     */
    private final ParametresCandidaturesService parametres;
    /** Vérification du dépôt et suivi en ligne par le candidat (APP-BE-12). */
    private final ecole_221.sn.ecole.modules.admissions.application.service.SuiviCandidatureService suivi;

    @Autowired
    public CandidaturesController(CandidateJpaRepository candidateJpaRepository, ParametresCandidaturesService parametres,
                                  ecole_221.sn.ecole.modules.admissions.application.service.SuiviCandidatureService suivi) {
        this.candidateJpaRepository = candidateJpaRepository;
        this.parametres = parametres;
        this.suivi = suivi;
    }

    /** Compatibilité (tests) : sans vérification du dépôt ni suivi. */
    public CandidaturesController(CandidateJpaRepository candidateJpaRepository, ParametresCandidaturesService parametres) {
        this(candidateJpaRepository, parametres, null);
    }

    @GetMapping("/settings")
    @Operation(summary = "Obtenir paramètres candidature", description = "Configuration des admissions de l'école courante (fermée et sans dates tant qu'elle n'a rien enregistré).")
    public ResponseEntity<Map<String, Object>> getSettings() {
        return ResponseEntity.ok(parametres.lire());
    }

    @PostMapping("/settings")
    @Operation(summary = "Mettre à jour paramètres candidature", description = "Met à jour la configuration des admissions de l'école courante.")
    public ResponseEntity<Map<String, Object>> updateSettings(@RequestBody Map<String, Object> body) {
        Map<String, Object> enregistres = parametres.enregistrer(body);
        logger.info("Paramètres de candidature enregistrés pour l'école courante");
        Map<String, Object> rep = new HashMap<>();
        rep.put("success", true);
        rep.put("settings", enregistres);
        return ResponseEntity.ok(rep);
    }

    @PostMapping("/submit")
    @Operation(summary = "Soumettre une candidature", description = "Soumet une nouvelle candidature d'admission avec persistance PostgreSQL.")
    public ResponseEntity<Map<String, Object>> submitCandidature(@RequestBody Map<String, Object> body) {
        // L'identifiant est TOUJOURS attribué par le serveur. Avant : un « id » fourni par le client était
        // repris tel quel, et ce dépôt public (sans compte) écrasait le dossier existant portant cet identifiant.
        String candidatureId = "CAND-" + java.time.Year.now() + "-" + UUID.randomUUID().toString().substring(0, 8);
        // L'école doit accepter des candidatures aujourd'hui (403 sinon) — APP-BE-15
        if (parametres != null) {
            parametres.exigerOuverture();
        }
        // Vérifications AVANT toute écriture (400 / 409) ; le mot de passe de suivi est haché.
        ecole_221.sn.ecole.modules.admissions.application.service.SuiviCandidatureService.Depot depot =
                suivi == null ? null : suivi.verifierDepot(body);

        {
            try {
                String nom = String.valueOf(body.getOrDefault("nom", ""));
                String prenom = String.valueOf(body.getOrDefault("prenom", ""));
                String email = String.valueOf(body.getOrDefault("email", ""));
                String telephone = String.valueOf(body.getOrDefault("telephone", ""));
                String filiere = String.valueOf(body.getOrDefault("filiere", ""));
                boolean fraisPayes = Boolean.parseBoolean(String.valueOf(body.getOrDefault("fraisPayes", false)));
                // APP-BE-04 : aucun moyen de paiement par défaut (plus de « WAVE ») ; absent ou vide → null
                String modePaiement = textOrNull(body.get("modePaiement"));
                String refPaiement = textOrNull(body.get("referencePaiement"));

                CandidateJpaEntity entity = depot == null
                        ? new CandidateJpaEntity(candidatureId, prenom, nom, email, telephone, filiere)
                        : new CandidateJpaEntity(candidatureId, depot.prenom(), depot.nom(), depot.email(), telephone, depot.filiere());
                if (depot != null) {
                    entity.setMotDePasseSuivi(depot.motDePasseHache());
                }
                entity.setRegistrationFeePaid(fraisPayes);
                entity.setRegistrationFeeReference(refPaiement);
                // Pièces : déclarées par le portail (docs.idCard / docs.diploma), jamais présumées fournies
                Object docs = body.get("docs");
                Map<?, ?> pieces = docs instanceof Map<?, ?> m ? m : Map.of();
                entity.setIdCardUploaded(Boolean.parseBoolean(String.valueOf(pieces.get("idCard"))));
                entity.setDiplomaUploaded(Boolean.parseBoolean(String.valueOf(pieces.get("diploma"))));
                entity.setAdditionalInfo(buildAdditionalInfo(modePaiement));
                entity.setSubmittedAt(LocalDateTime.now());
                entity.setCurrentStep("new");
                entity.setAdmissionStatus(CandidateStatus.EN_ATTENTE.name());

                candidateJpaRepository.save(entity);
                logger.info("Candidature {} persistée avec succès en PostgreSQL", candidatureId);
            } catch (Exception e) {
                // Avant : l'erreur était avalée et le candidat recevait « Candidature soumise avec succès ».
                logger.error("Erreur lors de la sauvegarde PostgreSQL de la candidature {}", candidatureId, e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "La candidature n'a pas pu être enregistrée. Veuillez réessayer.");
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

    @PostMapping("/suivi")
    @Operation(summary = "Suivre sa candidature",
            description = "Public (portail candidat). Corps {email, motDePasse} : renvoie uniquement le(s) dossier(s) de ce candidat "
                    + "dans l'école courante. Échec : 401 avec un message générique ; 429 après 5 échecs en 15 minutes.")
    public ResponseEntity<List<Map<String, Object>>> suivre(@RequestBody(required = false) Map<String, Object> corps) {
        if (suivi == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Suivi de candidature indisponible.");
        }
        Map<String, Object> c = corps == null ? Map.of() : corps;
        Object mdp = c.get("motDePasse");
        return ResponseEntity.ok(suivi.suivre(textOrNull(c.get("email")), mdp == null ? null : String.valueOf(mdp)));
    }

    /** Valeur texte non vide, sinon null (évite aussi la chaîne « null » produite par String.valueOf(null)). */
    static String textOrNull(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    /**
     * APP-BE-04 : additionalInfo sérialisé par Jackson (échappement garanti, pas d'injection JSON par concaténation).
     * Retourne null si aucun moyen de paiement n'est connu.
     */
    static String buildAdditionalInfo(String modePaiement) throws JsonProcessingException {
        if (modePaiement == null) {
            return null;
        }
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("paymentMethod", modePaiement);
        return JSON.writeValueAsString(info);
    }

    @GetMapping("/candidatures")
    @Operation(summary = "Liste des candidatures", description = "Retourne toutes les candidatures soumises.")
    public ResponseEntity<Map<String, Object>> getCandidatures() {
        List<Map<String, Object>> results = new ArrayList<>();
        {
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
        return ResponseEntity.ok(Map.of("candidatures", results));
    }

    @GetMapping("/candidatures/{id}")
    @Operation(summary = "Détails d'une candidature", description = "Retourne les détails d'une candidature spécifique.")
    public ResponseEntity<Map<String, Object>> getCandidature(@PathVariable String id) {
        {
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
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/candidatures/{id}/statut")
    @Operation(summary = "Modifier statut candidature", description = "Met à jour le statut d'une candidature.")
    public ResponseEntity<Map<String, Object>> updateCandidatureStatut(
            @PathVariable String id, 
            @RequestBody Map<String, String> body) {
        // Validation + normalisation : « ACCEPTE », « VALIDE »… → ACCEPTED ; valeur inconnue → 400 (BUG-QA-01)
        String newStatut = CandidateStatus.from(body.get("statut")).name();
        CandidateJpaEntity e = candidateJpaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidature introuvable : " + id));
        e.setAdmissionStatus(newStatut);
        candidateJpaRepository.save(e);
        logger.info("Statut candidature {} mis à jour: {}", id, newStatut);
        return ResponseEntity.ok(Map.of("success", true, "statut", newStatut));
    }
}
