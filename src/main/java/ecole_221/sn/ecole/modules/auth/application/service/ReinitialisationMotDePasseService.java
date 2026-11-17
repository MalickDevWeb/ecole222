package ecole_221.sn.ecole.modules.auth.application.service;

import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.ReinitialisationMotDePasseJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.ReinitialisationMotDePasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.email.domain.port.EmailServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Réinitialisation du mot de passe en ligne (APP-FEAT-02).
 *
 * <ul>
 *   <li>jeton aléatoire de 256 bits, à usage unique, valable {@value #VALIDITE_MINUTES} minutes, stocké HACHÉ ;</li>
 *   <li>réponse identique que l'email corresponde à un compte ou non, et envoi de l'email hors de la requête
 *       (pas de différence de temps de réponse) : aucune fuite de l'existence des comptes ;</li>
 *   <li>au plus {@value #DEMANDES_MAX} demandes par adresse en 15 minutes ; une nouvelle demande annule les liens
 *       précédents non utilisés ;</li>
 *   <li>lien construit sur l'adresse configurée du site (app.url-frontend), jamais sur la requête (Host,
 *       X-Forwarded-Host, Origin, Referer, X-Tenant-Slug) ni sur un domaine propre d'école ; l'école du lien
 *       (« /t/&lt;slug&gt; ») est l'école d'APPARTENANCE du compte (revue DevSecOps).</li>
 * </ul>
 */
@Service
public class ReinitialisationMotDePasseService {

    private static final Logger logger = LoggerFactory.getLogger(ReinitialisationMotDePasseService.class);
    public static final int VALIDITE_MINUTES = 30;
    public static final int DEMANDES_MAX = 3;
    private static final Duration FENETRE = Duration.ofMinutes(15);
    public static final String LIEN_INVALIDE = "Ce lien de réinitialisation n'est plus valide (déjà utilisé ou expiré) : demandez-en un nouveau.";

    private final UserJpaRepository comptes;
    private final ReinitialisationMotDePasseJpaRepository demandes;
    private final PasswordEncoder encodeur;
    /** Un jeton volé ne survit pas à la réinitialisation du mot de passe. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.infrastructure.security.revocation.RevocationJetons revocation;
    private final EmailServicePort email;
    private final String urlFrontend;
    private final SecureRandom aleatoire = new SecureRandom();
    private final Map<String, Deque<Instant>> recentes = new ConcurrentHashMap<>();
    Clock horloge = Clock.systemDefaultZone();
    Executor envoi = ForkJoinPool.commonPool();

    @Autowired
    public ReinitialisationMotDePasseService(UserJpaRepository comptes, ReinitialisationMotDePasseJpaRepository demandes,
                                             PasswordEncoder encodeur, EmailServicePort email,
                                             @Value("${app.url-frontend:http://localhost:5173}") String urlFrontend) {
        this.comptes = comptes;
        this.demandes = demandes;
        this.encodeur = encodeur;
        this.email = email;
        this.urlFrontend = urlFrontend.replaceAll("/+$", "");
    }

    /**
     * Demande d'un lien. Ne lève jamais d'erreur selon l'existence du compte : seule la limite de demandes
     * (429) et une adresse vide ou mal formée (400) sont signalées.
     */
    /** Appartenances et écoles : école du lien (facultatives dans les tests construits sans Spring). */
    @Autowired(required = false)
    private ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort appartenances;
    @Autowired(required = false)
    private ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort ecoles;

    void setAppartenances(ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort a,
                          ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort e) {
        this.appartenances = a;
        this.ecoles = e;
    }

    @Transactional
    public void demander(String emailBrut) {
        String adresse = emailBrut == null ? "" : emailBrut.trim().toLowerCase(Locale.ROOT);
        if (adresse.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saisissez l'adresse e-mail de votre compte.");
        }
        if (!adresse.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adresse e-mail invalide : vérifiez le format.");
        }
        limiter(adresse);
        if (adresse.endsWith(".invalid")) {
            // APP-TL-23 : identifiant technique d'un élève sans e-mail (domaine réservé, jamais livrable) : aucun lien
            // créé ni envoyé ; même réponse que pour une adresse sans compte. L'élève s'adresse à son école.
            logger.info("Demande de réinitialisation pour un compte sans e-mail (aucun envoi)");
            return;
        }
        UserJpaEntity compte = comptes.findByEmail(adresse).orElse(null);
        if (compte == null) {
            logger.info("Demande de réinitialisation pour une adresse sans compte (réponse identique)");
            return;
        }
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        for (ReinitialisationMotDePasseJpaEntity ancienne : demandes.findByUserIdAndUtiliseLeIsNull(compte.getId())) {
            ancienne.setUtiliseLe(maintenant);   // un seul lien valide à la fois
            demandes.save(ancienne);
        }
        String jeton = nouveauJeton();
        ReinitialisationMotDePasseJpaEntity d = new ReinitialisationMotDePasseJpaEntity();
        d.setId(UUID.randomUUID().toString());
        d.setUserId(compte.getId());
        d.setJetonHache(hacher(jeton));
        d.setCreeLe(maintenant);
        d.setExpireLe(maintenant.plusMinutes(VALIDITE_MINUTES));
        demandes.save(d);

        String slugEcole = ecoleDuCompte(compte.getId());
        String lien = urlFrontend + (slugEcole == null || slugEcole.isBlank() ? "" : "/t/" + slugEcole)
                + "/reinitialiser-mot-de-passe?jeton=" + jeton;
        String prenom = compte.getPrenom() == null ? "" : " " + compte.getPrenom();
        String contenu = "Bonjour" + prenom + ",\n\n"
                + "Une réinitialisation du mot de passe de votre compte École 221 a été demandée.\n"
                + "Pour choisir un nouveau mot de passe, ouvrez ce lien (valable " + VALIDITE_MINUTES + " minutes, une seule fois) :\n\n"
                + lien + "\n\n"
                + "Si vous n'êtes pas à l'origine de cette demande, ignorez cet e-mail : votre mot de passe reste inchangé.\n";
        String destinataire = compte.getEmail();
        envoi.execute(() -> {
            if (!email.sendEmail(destinataire, "Réinitialisation de votre mot de passe", contenu)) {
                logger.warn("E-mail de réinitialisation NON envoyé (canal d'envoi indisponible)");
            }
        });
    }

    /** Slug de l'école d'appartenance du compte (même choix qu'à la connexion), ou null. */
    private String ecoleDuCompte(String userId) {
        if (appartenances == null || ecoles == null) {
            return null;
        }
        try {
            var choix = ecole_221.sn.ecole.modules.platform.organization.application.EcoleDuCompte
                    .choisir(appartenances.findByUserId(userId), ecoles::findById);
            return choix.retenue() == null ? null : choix.retenue().getSlug();
        } catch (RuntimeException e) {
            logger.warn("École du compte introuvable pour le lien de réinitialisation : {}", e.getMessage());
            return null;
        }
    }

    /** Nouveau mot de passe avec un jeton valide ; 400 précis sinon. */
    @Transactional
    public void reinitialiser(String jeton, String nouveau) {
        reinitialiser(jeton, nouveau, null);
    }

    /** Essais du code oral (lien d'un compte à privilèges) avant annulation du lien. */
    public static final int ESSAIS_CODE_MAX = 5;

    /**
     * Nouveau mot de passe avec un jeton valide. V20 : un lien annulé ou en attente de l'accord d'un second
     * administrateur est refusé comme un lien expiré ; un lien de compte à privilèges exige en plus le code oral de
     * 6 chiffres (5 essais, puis le lien est annulé).
     */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void reinitialiser(String jeton, String nouveau, String code) {
        if (jeton == null || jeton.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, LIEN_INVALIDE);
        }
        ReinitialisationMotDePasseJpaEntity d = demandes.findByJetonHache(hacher(jeton.trim()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, LIEN_INVALIDE));
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        if (d.getUtiliseLe() != null || d.getAnnuleLe() != null || !maintenant.isBefore(d.getExpireLe())
                || (d.isAccordRequis() && d.getAccordePar() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, LIEN_INVALIDE);
        }
        if (d.getCodeHache() != null) {
            String saisi = code == null ? "" : code.replaceAll("\\s", "");
            if (saisi.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Saisissez le code de 6 chiffres que l'école vous a communiqué de vive voix.");
            }
            if (!encodeur.matches(saisi, d.getCodeHache())) {
                d.setEssaisCode(d.getEssaisCode() + 1);
                if (d.getEssaisCode() >= ESSAIS_CODE_MAX) {
                    d.setAnnuleLe(maintenant);
                    demandes.save(d);
                    logger.warn("[ALERTE SÉCURITÉ] Lien {} annulé après {} codes erronés", d.getId(), ESSAIS_CODE_MAX);
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Trop de codes erronés : ce lien est annulé. Demandez un nouveau lien à votre école.");
                }
                demandes.save(d);
                int reste = ESSAIS_CODE_MAX - d.getEssaisCode();
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Code incorrect : " + reste + " essai" + (reste > 1 ? "s" : "") + " restant" + (reste > 1 ? "s" : "") + ".");
            }
        }
        UserJpaEntity compte = comptes.findById(d.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, LIEN_INVALIDE));
        String regle = ChangementMotDePasseService.erreurRegles(nouveau, compte);   // règle unique
        if (regle != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, regle);   // le lien reste utilisable
        }
        compte.setPassword(encodeur.encode(nouveau));
        compte.setDoitChangerMotDePasse(null);
        comptes.save(compte);
        if (revocation != null) {
            revocation.invaliderTout(compte.getId());
        }
        d.setUtiliseLe(maintenant);
        demandes.save(d);
        logger.info("Mot de passe réinitialisé pour le compte {}", compte.getId());
    }

    private void limiter(String adresse) {
        Instant t = horloge.instant();
        Deque<Instant> liste = recentes.computeIfAbsent(adresse, k -> new ArrayDeque<>());
        synchronized (liste) {
            while (!liste.isEmpty() && liste.peekFirst().isBefore(t.minus(FENETRE))) liste.pollFirst();
            if (liste.size() >= DEMANDES_MAX) {
                long minutes = Math.max(1, (Duration.between(t, liste.peekFirst().plus(FENETRE)).getSeconds() + 59) / 60);
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Trop de demandes pour cette adresse. Réessayez dans " + minutes + " minute" + (minutes > 1 ? "s" : "") + ".");
            }
            liste.addLast(t);
        }
    }

    private String nouveauJeton() {
        byte[] octets = new byte[32];
        aleatoire.nextBytes(octets);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
    }

    public static String hacher(String jeton) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(jeton.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
