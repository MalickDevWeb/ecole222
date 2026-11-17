package ecole_221.sn.ecole.modules.admissions.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaEntity;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaRepository;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.ElementPortailAdmissionJpaEntity;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.ElementPortailAdmissionJpaRepository;
import ecole_221.sn.ecole.modules.email.application.port.usecase.EmailUseCase;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Espace du candidat (17/11) : après connexion au suivi (email + mot de passe), le candidat reçoit un JETON DE SUIVI
 * signé (2 h) qui lui donne accès à SON dossier seulement : ses pièces (consultées dans l'application, ajoutées ou
 * remplacées tant que la décision n'est pas rendue) et les messages échangés avec l'école. Quand l'école écrit, le
 * candidat est prévenu par e-mail. Avant : page de suivi en lecture seule, « contactez directement l'école ».
 * <p>
 * Messages : table admissions_portail_elements, nature ECHANGE (@TenantId), jamais touchée par POST /admissions/db.
 */
@Service
public class EspaceCandidatService {

    private static final Logger log = LoggerFactory.getLogger(EspaceCandidatService.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ZoneId DAKAR = ZoneId.of("Africa/Dakar");
    public static final String ECHANGE = "ECHANGE";
    static final long DUREE_JETON_SECONDES = 2 * 3600;
    static final int LONGUEUR_MAX_MESSAGE = 2000;

    /** Pièces du formulaire : code → [clé dans additional_info, libellé]. */
    static final Map<String, String[]> PIECES = new LinkedHashMap<>();
    static {
        PIECES.put("pieceIdentite", new String[]{"pieceIdentiteUrl", "Pièce d'identité"});
        PIECES.put("diplome", new String[]{"diplomeUrl", "Diplôme ou attestation"});
        PIECES.put("releves", new String[]{"relevesNotesUrl", "Relevés de notes"});
        PIECES.put("photo", new String[]{"photoIdentiteUrl", "Photo d'identité"});
    }

    private final CandidateJpaRepository candidats;
    private final ElementPortailAdmissionJpaRepository elements;
    private final byte[] cle;
    private final String urlFrontend;
    private final Clock horloge;
    private EmailUseCase emails;

    @Autowired
    public EspaceCandidatService(CandidateJpaRepository candidats, ElementPortailAdmissionJpaRepository elements,
                                 @Value("${jwt.secret:}") String secret,
                                 @Value("${app.url-frontend:http://localhost:5173}") String urlFrontend) {
        this(candidats, elements, secret, urlFrontend, Clock.systemUTC());
    }

    EspaceCandidatService(CandidateJpaRepository candidats, ElementPortailAdmissionJpaRepository elements, String secret,
                          String urlFrontend, Clock horloge) {
        this.candidats = candidats;
        this.elements = elements;
        this.urlFrontend = urlFrontend == null ? "" : urlFrontend.replaceAll("/+$", "");
        this.horloge = horloge;
        byte[] graine = new byte[32];
        if (secret == null || secret.isBlank()) new SecureRandom().nextBytes(graine);   // jetons valables pour ce processus
        else graine = ("suivi-candidat:" + secret).getBytes(StandardCharsets.UTF_8);
        this.cle = graine;
    }

    @Autowired(required = false)
    void setEmails(EmailUseCase emails) {
        this.emails = emails;
    }

    /** Envoi des e-mails hors de la requête (tests : exécution immédiate). */
    private java.util.concurrent.Executor envoiEmails = java.util.concurrent.CompletableFuture::runAsync;

    void setEnvoiEmails(java.util.concurrent.Executor envoiEmails) {
        this.envoiEmails = envoiEmails;
    }

    // ───────────────────────────── Jeton de suivi ─────────────────────────────

    /** Jeton signé : dossier + école + échéance. */
    public String emettreJeton(String candidatId) {
        String ecole = TenantContext.getCurrentOrganizationId().orElse("");
        long echeance = horloge.instant().getEpochSecond() + DUREE_JETON_SECONDES;
        String charge = b64(candidatId + "|" + ecole + "|" + echeance);
        return charge + "." + signer(charge);
    }

    /** Dossier du jeton, ou 401 si le jeton est absent, falsifié, expiré ou d'une autre école. */
    public CandidateJpaEntity dossierDuJeton(String jeton) {
        String id = idDuJeton(jeton);
        return candidats.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ACCES));
    }

    private static final String ACCES = "Votre session de suivi a expiré : reconnectez-vous avec votre email et votre mot de passe.";

    String idDuJeton(String jeton) {
        if (jeton == null || !jeton.contains(".")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ACCES);
        String[] parts = jeton.split("\\.", 2);
        if (!MessageDigest.isEqual(signer(parts[0]).getBytes(StandardCharsets.UTF_8), parts[1].getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ACCES);
        }
        String[] champs;
        try {
            champs = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8).split("\\|");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ACCES);
        }
        String ecole = TenantContext.getCurrentOrganizationId().orElse("");
        if (champs.length != 3 || !champs[1].equals(ecole) || Long.parseLong(champs[2]) < horloge.instant().getEpochSecond()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ACCES);
        }
        return champs[0];
    }

    private String signer(String charge) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(cle, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(charge.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Signature du jeton de suivi impossible", e);
        }
    }

    private static String b64(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    // ───────────────────────────── Dossier enrichi ─────────────────────────────

    /** Ajoute au dossier renvoyé par le suivi : jeton, pièces (jointes ou non), messages non lus. */
    public Map<String, Object> enrichir(Map<String, Object> vue) {
        String id = String.valueOf(vue.get("id"));
        candidats.findById(id).ifPresent(c -> {
            vue.put("jeton", emettreJeton(id));
            vue.put("pieces", pieces(c));
            vue.put("messagesNonLus", messages(id).stream()
                    .filter(m -> "ECOLE".equals(m.get("auteur")) && !Boolean.TRUE.equals(m.get("lu"))).count());
            vue.put("modifiable", modifiable(c));
        });
        return vue;
    }

    /** Dossier du candidat (même forme que le suivi), enrichi : pour rafraîchir la page sans redemander le mot de passe. */
    public Map<String, Object> dossier(CandidateJpaEntity c) {
        return enrichir(new LinkedHashMap<>(SuiviCandidatureService.vue(c)));
    }

    /** Pièces du formulaire et pièces exigées par une règle : code, libellé, jointe ou non. */
    public List<Map<String, Object>> pieces(CandidateJpaEntity c) {
        Map<String, Object> info = info(c);
        List<Map<String, Object>> res = new ArrayList<>();
        PIECES.forEach((code, def) -> res.add(Map.of("code", code, "libelle", def[1], "jointe", texte(info.get(def[0])) != null)));
        info.forEach((cle, v) -> {
            if (cle.startsWith("pieceSuppLibelle.") && texte(v) != null) {
                String code = cle.substring("pieceSuppLibelle.".length());
                res.add(Map.of("code", code, "libelle", String.valueOf(v), "jointe", texte(info.get("pieceSupp." + code)) != null));
            }
        });
        return res;
    }

    /** Lien d'une pièce du dossier (pour la remettre au candidat) ; null si non jointe. */
    public String lienPiece(CandidateJpaEntity c, String code) {
        return texte(info(c).get(cleLien(code)));
    }

    private static String cleLien(String code) {
        if (PIECES.containsKey(code)) return PIECES.get(code)[0];
        if (code != null && code.matches("[a-z0-9-]{1,40}")) return "pieceSupp." + code;
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pièce inconnue.");
    }

    /** Pièces modifiables tant que l'école n'a pas rendu sa décision. */
    static boolean modifiable(CandidateJpaEntity c) {
        String s = SuiviCandidatureService.statut(c.getAdmissionStatus());
        return !"ACCEPTE".equals(s) && !"REFUSE".equals(s);
    }

    /** Ajoute ou remplace une pièce du dossier (lien renvoyé par /media/upload) ; l'école en est informée. */
    @Transactional
    public List<Map<String, Object>> deposerPiece(CandidateJpaEntity c, String code, String lienBrut) {
        if (!modifiable(c)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La décision sur votre dossier est rendue : les pièces ne peuvent plus être modifiées.");
        }
        String cle = cleLien(code);
        Map<String, Object> info = info(c);
        String libelle = PIECES.containsKey(code) ? PIECES.get(code)[1] : texte(info.get("pieceSuppLibelle." + code));
        if (libelle == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cette pièce n'est pas demandée pour votre dossier.");
        String lien = texte(lienBrut);
        if (lien == null || !lien.matches("(?i)^https?://\\S+$") || lien.length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le lien du fichier n'est pas valide : envoyez de nouveau le fichier.");
        }
        boolean remplace = texte(info.get(cle)) != null;
        try {
            ObjectNode json = c.getAdditionalInfo() == null || c.getAdditionalInfo().isBlank()
                    ? JSON.createObjectNode() : (ObjectNode) JSON.readTree(c.getAdditionalInfo());
            json.put(cle, lien);
            c.setAdditionalInfo(JSON.writeValueAsString(json));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Le dossier n'a pas pu être mis à jour.");
        }
        if ("pieceIdentite".equals(code)) c.setIdCardUploaded(true);
        if ("diplome".equals(code)) c.setDiplomaUploaded(true);
        candidats.save(c);
        ajouterMessage(c.getId(), "CANDIDAT", nomCandidat(c), (remplace ? "J'ai remplacé la pièce « " : "J'ai ajouté la pièce « ") + libelle + " ».");
        return pieces(c);
    }

    // ───────────────────────────── Messages ─────────────────────────────

    /** Messages d'un dossier, du plus ancien au plus récent. */
    public List<Map<String, Object>> messages(String candidatId) {
        List<Map<String, Object>> res = new ArrayList<>();
        for (ElementPortailAdmissionJpaEntity e : elements.findByNatureOrderByPositionAsc(ECHANGE)) {
            Map<String, Object> m = lire(e);
            if (m != null && candidatId.equals(m.get("candidatId"))) res.add(m);
        }
        return res;
    }

    /** Messages vus par un lecteur (CANDIDAT ou ECOLE) : ceux de l'autre partie deviennent « lus ». */
    @Transactional
    public List<Map<String, Object>> lireMessages(String candidatId, String lecteur) {
        List<Map<String, Object>> res = new ArrayList<>();
        for (ElementPortailAdmissionJpaEntity e : elements.findByNatureOrderByPositionAsc(ECHANGE)) {
            Map<String, Object> m = lire(e);
            if (m == null || !candidatId.equals(m.get("candidatId"))) continue;
            if (!lecteur.equals(m.get("auteur")) && !Boolean.TRUE.equals(m.get("lu"))) {
                m.put("lu", true);
                ecrire(e, m);
            }
            res.add(m);
        }
        return res;
    }

    /** Message de l'école : le candidat est prévenu par e-mail (lien vers son suivi). */
    @Transactional
    public Map<String, Object> messageEcole(String candidatId, String auteurNom, String texte) {
        CandidateJpaEntity c = candidats.findById(candidatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier de candidature introuvable."));
        Map<String, Object> m = ajouterMessage(candidatId, "ECOLE", auteurNom, texte);
        prevenirParEmail(c, texte);
        return m;
    }

    @Transactional
    public Map<String, Object> messageCandidat(CandidateJpaEntity c, String texte) {
        return ajouterMessage(c.getId(), "CANDIDAT", nomCandidat(c), texte);
    }

    /** Nombre de messages du candidat pas encore lus par l'école (badge du dossier). */
    public Map<String, Long> nonLusParEcole() {
        Map<String, Long> res = new HashMap<>();
        for (ElementPortailAdmissionJpaEntity e : elements.findByNatureOrderByPositionAsc(ECHANGE)) {
            Map<String, Object> m = lire(e);
            if (m != null && "CANDIDAT".equals(m.get("auteur")) && !Boolean.TRUE.equals(m.get("lu"))) {
                res.merge(String.valueOf(m.get("candidatId")), 1L, Long::sum);
            }
        }
        return res;
    }

    private Map<String, Object> ajouterMessage(String candidatId, String auteur, String auteurNom, String texteBrut) {
        String texte = texte(texteBrut);
        if (texte == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Écrivez votre message.");
        if (texte.length() > LONGUEUR_MAX_MESSAGE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message trop long (" + LONGUEUR_MAX_MESSAGE + " caractères au maximum).");
        }
        List<ElementPortailAdmissionJpaEntity> tous = elements.findByNatureOrderByPositionAsc(ECHANGE);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", "msg-" + UUID.randomUUID().toString().substring(0, 12));
        m.put("candidatId", candidatId);
        m.put("auteur", auteur);
        m.put("auteurNom", auteurNom == null || auteurNom.isBlank() ? (auteur.equals("ECOLE") ? "L'école" : "Candidat") : auteurNom);
        m.put("texte", texte);
        m.put("envoyeLe", LocalDateTime.now(horloge.withZone(DAKAR)).toString());
        m.put("lu", false);
        ElementPortailAdmissionJpaEntity e = new ElementPortailAdmissionJpaEntity();
        e.setId(String.valueOf(m.get("id")));
        e.setNature(ECHANGE);
        e.setPosition(tous.stream().mapToInt(ElementPortailAdmissionJpaEntity::getPosition).max().orElse(-1) + 1);
        ecrire(e, m);
        return m;
    }

    private void prevenirParEmail(CandidateJpaEntity c, String texte) {
        if (emails == null || c.getEmail() == null || c.getEmail().isBlank()) return;
        String ecole = TenantContext.getCurrentOrganization().map(o -> o.getName()).orElse("L'école");
        String slug = TenantContext.getCurrentOrganization().map(o -> o.getSlug()).orElse(null);
        String lien = urlFrontend + (slug == null || slug.isBlank() ? "" : "/t/" + slug) + "/candidature";
        String extrait = texte.length() > 300 ? texte.substring(0, 300) + "…" : texte;
        String contenu = "Bonjour " + Optional.ofNullable(c.getFirstName()).orElse("") + ",\n\n"
                + ecole + " vous a écrit au sujet de votre dossier " + c.getId() + " :\n\n« " + extrait + " »\n\n"
                + "Pour répondre ou suivre votre dossier, connectez-vous à votre suivi : " + lien + "\n"
                + "(email du dossier et mot de passe choisi au dépôt).";
        // En arrière-plan : le serveur de messagerie peut mettre plusieurs secondes à répondre (20 s constatées en QA) ;
        // l'administration ne l'attend pas. Échec : journalisé, le message reste lisible dans le suivi du candidat.
        String destinataire = c.getEmail(), dossierId = c.getId(), sujet = ecole + " — nouveau message sur votre candidature";
        envoiEmails.execute(() -> {
            try {
                if (!emails.sendStudentNotification(destinataire, sujet, contenu)) {
                    log.warn("Message de l'école au candidat {} : e-mail refusé par le service d'envoi", dossierId);
                }
            } catch (Exception ex) {
                log.warn("Message de l'école au candidat {} : e-mail non envoyé ({})", dossierId, ex.getMessage());
            }
        });
    }

    // ───────────────────────────── Outils ─────────────────────────────

    private static String nomCandidat(CandidateJpaEntity c) {
        String n = (Optional.ofNullable(c.getFirstName()).orElse("") + " " + Optional.ofNullable(c.getLastName()).orElse("")).trim();
        return n.isEmpty() ? "Candidat" : n;
    }

    private static Map<String, Object> info(CandidateJpaEntity c) {
        try {
            return c.getAdditionalInfo() == null || c.getAdditionalInfo().isBlank() ? new LinkedHashMap<>()
                    : JSON.readValue(c.getAdditionalInfo(), new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private static Map<String, Object> lire(ElementPortailAdmissionJpaEntity e) {
        try {
            return JSON.readValue(e.getContenuJson(), new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            return null;
        }
    }

    private void ecrire(ElementPortailAdmissionJpaEntity e, Map<String, Object> m) {
        try {
            e.setContenuJson(JSON.writeValueAsString(m));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Message illisible.");
        }
        e.setUpdatedAt(LocalDateTime.now(horloge.withZone(DAKAR)));
        elements.save(e);
    }

    private static String texte(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }
}
