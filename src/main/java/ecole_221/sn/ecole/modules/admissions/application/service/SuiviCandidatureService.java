package ecole_221.sn.ecole.modules.admissions.application.service;

import ecole_221.sn.ecole.modules.admissions.domain.valueobject.CandidateStatus;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaEntity;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Suivi en ligne de sa candidature par le candidat (APP-BE-12).
 * <ul>
 *   <li>au dépôt : identité, email, formation et mot de passe de suivi vérifiés ; un seul dossier par email et par école ;</li>
 *   <li>au suivi : email + mot de passe → uniquement SON dossier (école courante, @TenantId) ; échec générique,
 *       tentatives répétées freinées.</li>
 * </ul>
 * Avant : le mot de passe saisi au dépôt n'était jamais envoyé et la « connexion » se faisait dans le navigateur
 * à partir de la base complète des candidats (GET /admissions/db, désormais fermé).
 */
@Service
public class SuiviCandidatureService {

    public static final int MOT_DE_PASSE_MIN = 8;
    static final int ECHECS_MAX = 5;
    static final Duration FENETRE = Duration.ofMinutes(15);
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String ECHEC = "Email ou mot de passe incorrect.";

    private final CandidateJpaRepository candidats;
    private final PasswordEncoder encodeur;
    private final Clock horloge;
    private final Map<String, Deque<Instant>> echecs = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public SuiviCandidatureService(CandidateJpaRepository candidats, PasswordEncoder encodeur) {
        this(candidats, encodeur, Clock.systemUTC());
    }

    SuiviCandidatureService(CandidateJpaRepository candidats, PasswordEncoder encodeur, Clock horloge) {
        this.candidats = candidats;
        this.encodeur = encodeur;
        this.horloge = horloge;
    }

    /** Données vérifiées d'un dépôt ; {@code motDePasseHache} est prêt à être enregistré. */
    public record Depot(String prenom, String nom, String email, String filiere, String motDePasseHache) {}

    /** Vérifie un dépôt public AVANT toute écriture (400 / 409 sinon). */
    @Transactional(readOnly = true)
    public Depot verifierDepot(Map<String, Object> corps) {
        String prenom = texte(corps.get("prenom"));
        String nom = texte(corps.get("nom"));
        if (prenom == null || nom == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le prénom et le nom du candidat sont obligatoires.");
        }
        String email = texte(corps.get("email"));
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'adresse e-mail est obligatoire : elle sert au suivi du dossier.");
        }
        email = email.toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(email).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'adresse e-mail « " + email + " » n'est pas valide.");
        }
        String filiere = texte(corps.get("filiere"));
        if (filiere == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La formation demandée est obligatoire.");
        }
        Object mdp = corps.get("motDePasse");
        String motDePasse = mdp == null ? "" : String.valueOf(mdp);
        if (motDePasse.length() < MOT_DE_PASSE_MIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le mot de passe de suivi doit contenir au moins " + MOT_DE_PASSE_MIN + " caractères.");
        }
        if (candidats.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une candidature existe déjà avec l'email " + email
                    + " dans cette école : connectez-vous au suivi de candidature avec votre mot de passe.");
        }
        return new Depot(prenom, nom, email, filiere, encodeur.encode(motDePasse));
    }

    /** Dossier(s) du candidat, uniquement si l'email et le mot de passe correspondent. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> suivre(String emailBrut, String motDePasse) {
        String email = emailBrut == null ? "" : emailBrut.trim().toLowerCase(Locale.ROOT);
        if (email.isEmpty() || motDePasse == null || motDePasse.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saisissez l'email du dossier et votre mot de passe de suivi.");
        }
        Instant maintenant = horloge.instant();
        Deque<Instant> recents = echecs.computeIfAbsent(email, k -> new ArrayDeque<>());
        synchronized (recents) {
            while (!recents.isEmpty() && recents.peekFirst().isBefore(maintenant.minus(FENETRE))) recents.pollFirst();
            if (recents.size() >= ECHECS_MAX) {
                long minutes = Math.max(1, (Duration.between(maintenant, recents.peekFirst().plus(FENETRE)).getSeconds() + 59) / 60);
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Trop de tentatives pour ce dossier. Réessayez dans " + minutes + " minute" + (minutes > 1 ? "s" : "") + ".");
            }
        }
        List<CandidateJpaEntity> dossiers = candidats.findByEmailIgnoreCase(email).stream()
                .filter(c -> c.getMotDePasseSuivi() != null && encodeur.matches(motDePasse, c.getMotDePasseSuivi()))
                .sorted(Comparator.comparing(CandidateJpaEntity::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        if (dossiers.isEmpty()) {
            synchronized (recents) { recents.addLast(maintenant); }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ECHEC);   // même message : rien n'indique si l'email existe
        }
        echecs.remove(email);
        return dossiers.stream().map(SuiviCandidatureService::vue).toList();
    }

    static Map<String, Object> vue(CandidateJpaEntity c) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", c.getId());
        v.put("prenom", c.getFirstName());
        v.put("nom", c.getLastName());
        v.put("email", c.getEmail());
        v.put("formation", c.getCourseChoice());
        v.put("dateDepot", c.getSubmittedAt() == null ? null : c.getSubmittedAt().toString());
        v.put("statut", statut(c.getAdmissionStatus()));
        v.put("etape", c.getCurrentStep());
        v.put("motifRefus", "REFUSE".equals(statut(c.getAdmissionStatus())) ? c.getRejectionReason() : null);
        v.put("pieceIdentiteDeclaree", Boolean.TRUE.equals(c.getIdCardUploaded()));
        v.put("diplomeDeclare", Boolean.TRUE.equals(c.getDiplomaUploaded()));
        // CONFIRMES : vérifiés par l'école ; DECLARES : référence de paiement saisie, pas encore vérifiée
        v.put("frais", Boolean.TRUE.equals(c.getRegistrationFeePaid()) ? "CONFIRMES"
                : (c.getRegistrationFeeReference() != null && !c.getRegistrationFeeReference().isBlank()) ? "DECLARES" : "NON_DECLARES");
        return v;
    }

    /** EN_ATTENTE, EN_COURS, ACCEPTE ou REFUSE (clé affichable, quelle que soit la variante stockée). */
    static String statut(String brut) {
        if (brut == null || brut.isBlank()) return "EN_ATTENTE";
        try {
            return switch (CandidateStatus.from(brut)) {
                case ACCEPTED, FINALISE -> "ACCEPTE";
                case REJECTED -> "REFUSE";
                case EN_COURS -> "EN_COURS";
                case EN_ATTENTE -> "EN_ATTENTE";
            };
        } catch (IllegalArgumentException e) {
            return "EN_ATTENTE";
        }
    }

    private static String texte(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }
}
