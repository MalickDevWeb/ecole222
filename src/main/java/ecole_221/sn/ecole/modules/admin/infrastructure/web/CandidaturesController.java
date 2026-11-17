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
     * Paramètres persistés par école (table parametres_candidatures).
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

    /** Règles d'admission de l'école ; absentes dans les anciens tests unitaires (aucune règle appliquée). */
    private ecole_221.sn.ecole.modules.admissions.application.service.ReglesAdmissionService regles;

    @Autowired(required = false)
    void setRegles(ecole_221.sn.ecole.modules.admissions.application.service.ReglesAdmissionService regles) {
        this.regles = regles;
    }

    /** Espace du candidat : jeton de suivi, pièces, échanges avec l'école. */
    private ecole_221.sn.ecole.modules.admissions.application.service.EspaceCandidatService espace;

    @Autowired(required = false)
    void setEspace(ecole_221.sn.ecole.modules.admissions.application.service.EspaceCandidatService espace) {
        this.espace = espace;
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
        // L'identifiant est TOUJOURS attribué par le serveur : ce dépôt public (sans compte) ne doit jamais
        // écraser un dossier existant avec un « id » fourni par le client.
        String candidatureId = "CAND-" + java.time.Year.now() + "-" + UUID.randomUUID().toString().substring(0, 8);
        // L'école doit accepter des candidatures aujourd'hui (403 sinon) — APP-BE-15
        if (parametres != null) {
            parametres.exigerOuverture();
        }
        // Vérifications AVANT toute écriture (400 / 409) ; le mot de passe de suivi est haché.
        ecole_221.sn.ecole.modules.admissions.application.service.SuiviCandidatureService.Depot depot =
                suivi == null ? null : suivi.verifierDepot(body);

        lireInformations(body);   // 400 précis avant toute écriture (sinon transformé en 500 ci-dessous)
        if (depot == null) {
            ecole_221.sn.ecole.modules.admissions.application.service.SuiviCandidatureService.lirePieces(body.get("pieces"), false);
        }
        // Règles de l'école : blocage, ou pièce exigée absente / au mauvais format → 400 AVANT toute écriture.
        // Ajoute au dossier : session (campagneId), pièces exigées par les règles, avertissements pour l'administration.
        Map<String, String> ajoutsRegles = regles == null ? Map.of()
                : regles.appliquerAuDepot(ecole_221.sn.ecole.modules.admissions.application.service.ReglesAdmissionService.Dossier.depuis(body, false),
                        body.get("piecesSupplementaires") instanceof Map<?, ?> m ? castPieces(m) : Map.of());
        {
            try {
                String nom = String.valueOf(body.getOrDefault("nom", ""));
                String prenom = String.valueOf(body.getOrDefault("prenom", ""));
                String email = String.valueOf(body.getOrDefault("email", ""));
                String telephone = String.valueOf(body.getOrDefault("telephone", ""));
                String filiere = String.valueOf(body.getOrDefault("filiere", ""));
                // APP-TL-23 : les frais de dossier sont confirmés par l'école (guichet, Wave, Orange Money), jamais par
                // la déclaration du candidat.
                boolean fraisPayes = false;
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
                // Pièces : reçues seulement avec le lien du fichier réellement envoyé.
                Map<String, String> pieces = depot != null ? depot.pieces()
                        : ecole_221.sn.ecole.modules.admissions.application.service.SuiviCandidatureService.lirePieces(body.get("pieces"), false);
                entity.setIdCardUploaded(pieces.containsKey("pieceIdentiteUrl"));
                entity.setDiplomaUploaded(pieces.containsKey("diplomeUrl"));
                Map<String, String> informations = lireInformations(body);
                informations.putAll(pieces);
                informations.putAll(ajoutsRegles);
                entity.setAdditionalInfo(buildAdditionalInfo(modePaiement, informations));
                entity.setSubmittedAt(LocalDateTime.now());
                entity.setCurrentStep("new");
                entity.setAdmissionStatus(CandidateStatus.EN_ATTENTE.name());

                candidateJpaRepository.save(entity);
                logger.info("Candidature {} persistée avec succès en PostgreSQL", candidatureId);
            } catch (Exception e) {
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
        List<Map<String, Object>> dossiers = suivi.suivre(textOrNull(c.get("email")), mdp == null ? null : String.valueOf(mdp));
        // Jeton de suivi (2 h), pièces et messages non lus — espace du candidat
        return ResponseEntity.ok(espace == null ? dossiers : dossiers.stream().map(d -> espace.enrichir(new LinkedHashMap<>(d))).toList());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> castPieces(Map<?, ?> m) {
        return (Map<String, ?>) m;
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
        return buildAdditionalInfo(modePaiement, Map.of());
    }

    /** Moyen de paiement + informations saisies par le candidat ; null si rien n'est connu. */
    static String buildAdditionalInfo(String modePaiement, Map<String, String> informations) throws JsonProcessingException {
        Map<String, Object> info = new LinkedHashMap<>();
        if (modePaiement != null) {
            info.put("paymentMethod", modePaiement);
        }
        info.putAll(informations);
        return info.isEmpty() ? null : JSON.writeValueAsString(info);
    }

    /** Champs texte du dossier conservés tels quels (liste fermée : rien d'autre n'est enregistré). */
    private static final List<String> CHAMPS_DOSSIER = List.of("dateNaissance", "lieuNaissance", "sexe", "nationalite",
            "numeroPiece", "adresse", "niveau", "etablissementPrecedent", "serieBac", "anneeBac", "mentionBac", "sessionRentree");
    private static final Map<String, String> CHAMPS_GARANT = new LinkedHashMap<>(Map.of(
            "nom", "garantNom", "lien", "garantLien", "telephone", "garantTelephone", "email", "garantEmail",
            "profession", "garantProfession"));
    private static final int LONGUEUR_MAX = 200;

    /**
     * Informations saisies dans le portail (identité, garant, cursus).
     * Valeurs vides ignorées ; 400 si une valeur est trop longue, un sexe inconnu ou un email de garant invalide.
     */
    static Map<String, String> lireInformations(Map<String, Object> body) {
        Map<String, String> info = new LinkedHashMap<>();
        for (String champ : CHAMPS_DOSSIER) {
            ajouter(info, champ, champ, body.get(champ));
        }
        if (body.get("garant") instanceof Map<?, ?> garant) {
            CHAMPS_GARANT.forEach((source, cle) -> ajouter(info, cle, "garant." + source, garant.get(source)));
        }
        String sexe = info.get("sexe");
        if (sexe != null && !sexe.equals("M") && !sexe.equals("F")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sexe inconnu : « M » ou « F » attendu.");
        }
        String emailGarant = info.get("garantEmail");
        if (emailGarant != null && !emailGarant.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'adresse e-mail du garant « " + emailGarant + " » n'est pas valide.");
        }
        return info;
    }

    private static void ajouter(Map<String, String> info, String cle, String libelle, Object valeur) {
        String texte = textOrNull(valeur);
        if (texte == null) {
            return;
        }
        if (texte.length() > LONGUEUR_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "« " + libelle + " » est trop long (" + LONGUEUR_MAX + " caractères au maximum).");
        }
        info.put(cle, texte);
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
        CandidateStatus decision = CandidateStatus.from(body.get("statut"));
        ecole_221.sn.ecole.modules.admissions.application.service.EtatsCandidature.refuserFinaliseManuel(decision);   // APP-TL-23
        String newStatut = decision.name();
        CandidateJpaEntity e = candidateJpaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidature introuvable : " + id));
        ecole_221.sn.ecole.modules.admissions.application.service.EtatsCandidature.exigerModifiable(e);   // dossier inscrit → 409
        e.setAdmissionStatus(newStatut);
        ecole_221.sn.ecole.modules.admissions.application.service.EtatsCandidature.apresDecision(e);   // l'étape suit (APP-BE-24)
        candidateJpaRepository.save(e);
        logger.info("Statut candidature {} mis à jour: {}", id, newStatut);
        return ResponseEntity.ok(Map.of("success", true, "statut", newStatut));
    }
}
