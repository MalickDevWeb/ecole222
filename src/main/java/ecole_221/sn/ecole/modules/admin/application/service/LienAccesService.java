package ecole_221.sn.ecole.modules.admin.application.service;

import ecole_221.sn.ecole.modules.auth.application.service.ReinitialisationMotDePasseService;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.ReinitialisationMotDePasseJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.ReinitialisationMotDePasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.email.domain.port.EmailServicePort;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaEntity;
import ecole_221.sn.ecole.partage.NumeroTelephone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Liens d'accès décidés par l'école (décisions du Tech Lead du 15/11) : aucun mot de passe n'est plus écrit dans un
 * message. L'administration émet un lien à usage unique (jeton de 256 bits, stocké haché, table de V7 complétée par
 * V20) que le titulaire ouvre pour choisir lui-même son mot de passe.
 *
 * <ul>
 *   <li>ACTIVATION (compte jamais utilisé) : valable {@value #ACTIVATION_HEURES} h ; REINITIALISATION : {@value #REINIT_HEURES} h ;</li>
 *   <li>canal WHATSAPP ou SMS : le serveur n'envoie rien (aucun fournisseur) ; il renvoie le message prérempli et l'adresse
 *       {@code https://wa.me/…} ou {@code sms:…} que l'administrateur ouvre sur son téléphone ; canal EMAIL : envoi par
 *       le service d'e-mail (jamais vers un identifiant technique « .invalid ») ;</li>
 *   <li>un nouveau lien annule les précédents ;</li>
 *   <li>compte à privilèges (administrateur, directeur) ou numéro modifié depuis moins de 24 h : le lien n'est délivré
 *       qu'après l'ACCORD d'un second administrateur ; pour un compte à privilèges, un code de 6 chiffres est en plus
 *       demandé à l'ouverture du lien (à communiquer de vive voix, jamais dans le même message) ;</li>
 *   <li>route réservée à l'administration (hors matrice des droits du personnel).</li>
 * </ul>
 */
@Service
public class LienAccesService {

    private static final Logger log = LoggerFactory.getLogger(LienAccesService.class);
    public static final int ACTIVATION_HEURES = 72;
    public static final int REINIT_HEURES = 2;
    /** Délai pendant lequel un numéro modifié demande l'accord d'un second administrateur. */
    public static final int DELAI_NUMERO_HEURES = 24;
    static final Set<String> ROLES_PRIVILEGIES = Set.of("ADMIN", "ADMINISTRATOR", "DIRECTEUR");
    private static final Set<String> CANAUX = Set.of("WHATSAPP", "SMS", "EMAIL");

    private final ComptesEcoleService comptes;
    private final ReinitialisationMotDePasseJpaRepository liens;
    private final OrganizationRepositoryPort ecoles;
    private final EmailServicePort email;
    private final PasswordEncoder encodeur;
    private final String urlFrontend;
    private final SecureRandom aleatoire = new SecureRandom();
    Clock horloge = Clock.systemDefaultZone();

    public LienAccesService(ComptesEcoleService comptes, ReinitialisationMotDePasseJpaRepository liens,
                            OrganizationRepositoryPort ecoles, EmailServicePort email, PasswordEncoder encodeur,
                            @Value("${app.url-frontend:http://localhost:5173}") String urlFrontend) {
        this.comptes = comptes;
        this.liens = liens;
        this.ecoles = ecoles;
        this.email = email;
        this.encodeur = encodeur;
        this.urlFrontend = urlFrontend.replaceAll("/+$", "");
    }

    /** Corps : {canal : WHATSAPP | SMS | EMAIL}. */
    @Transactional
    public Map<String, Object> emettre(String userId, Map<String, Object> corps) {
        String ecole = comptes.ecoleCourante();
        String auteur = auteur();
        OrganizationUserJpaEntity appartenance = comptes.appartenance(userId, null)
                .or(() -> comptes.appartenance(userId, "ETUDIANT"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte introuvable dans cet établissement."));
        UserJpaEntity compte = comptes.membre(userId, appartenance.getRole());
        if (userId.equals(auteur)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Vous ne pouvez pas émettre de lien d'accès pour votre propre compte : utilisez « Mot de passe oublié ».");
        }
        String canal = corps == null || corps.get("canal") == null ? null : corps.get("canal").toString().trim().toUpperCase();
        if (canal == null || !CANAUX.contains(canal)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choisissez le canal d'envoi : WHATSAPP, SMS ou EMAIL.");
        }
        String numero = null;
        if (!"EMAIL".equals(canal)) {
            numero = NumeroTelephone.e164(compte.getTelephone()).orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce compte n'a pas de numéro de mobile valide : corrigez-le dans sa fiche, ou choisissez l'e-mail."));
        } else if (compte.getEmail() == null || InscriptionAdmisService.sansEmail(compte.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce compte n'a pas d'adresse e-mail : choisissez WhatsApp ou SMS.");
        }

        LocalDateTime maintenant = LocalDateTime.now(horloge);
        boolean privilegie = ROLES_PRIVILEGIES.contains(appartenance.getRole() == null ? "" : appartenance.getRole().toUpperCase());
        boolean numeroRecent = numero != null && compte.getTelephoneModifieLe() != null
                && compte.getTelephoneModifieLe().isAfter(maintenant.minusHours(DELAI_NUMERO_HEURES));
        boolean accordRequis = privilegie || numeroRecent;

        annulerPrecedents(userId, maintenant);
        ReinitialisationMotDePasseJpaEntity l = new ReinitialisationMotDePasseJpaEntity();
        l.setId(UUID.randomUUID().toString());
        l.setUserId(userId);
        l.setType(compte.getLastLoginAt() == null ? "ACTIVATION" : "REINITIALISATION");
        l.setOrganizationId(ecole);
        l.setCreePar(auteur);
        l.setCanal(canal);
        l.setNumeroMasque(numero == null ? null : NumeroTelephone.masquer(numero));
        l.setCreeLe(maintenant);
        l.setAccordRequis(accordRequis);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("lienId", l.getId());
        res.put("type", l.getType());
        res.put("canal", canal);
        res.put("destinataire", numero == null ? masquerEmail(compte.getEmail()) : l.getNumeroMasque());
        res.put("accordRequis", accordRequis);
        if (accordRequis) {
            // jeton inconnu de tous (aucun lien utilisable) jusqu'à l'accord ; le vrai jeton est tiré à l'accord
            l.setJetonHache(ReinitialisationMotDePasseService.hacher(nouveauJeton()));
            l.setExpireLe(maintenant.plusHours(ACTIVATION_HEURES));   // délai laissé au second administrateur
            liens.save(l);
            res.put("motif", privilegie
                    ? "Compte à privilèges : un second administrateur de l'école doit donner son accord avant l'envoi."
                    : "Numéro modifié depuis moins de " + DELAI_NUMERO_HEURES + " h : un second administrateur doit donner son accord.");
            log.info("[LIEN D'ACCÈS] Lien {} pour le compte {} en attente d'accord (émis par {})", l.getId(), userId, auteur);
            return res;
        }
        res.putAll(delivrer(l, compte, numero, privilegie, maintenant));
        log.info("[LIEN D'ACCÈS] Lien {} ({}, {}) émis pour le compte {} par {}", l.getId(), l.getType(), canal, userId, auteur);
        return res;
    }

    /** Accord du second administrateur : tire le jeton et renvoie le message à transmettre. */
    @Transactional
    public Map<String, Object> accorder(String lienId) {
        String ecole = comptes.ecoleCourante();
        String auteur = auteur();
        ReinitialisationMotDePasseJpaEntity l = liens.findById(lienId)
                .filter(x -> ecole.equals(x.getOrganizationId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande de lien introuvable."));
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        if (!l.isAccordRequis() || l.getAccordePar() != null || l.getUtiliseLe() != null || l.getAnnuleLe() != null
                || !maintenant.isBefore(l.getExpireLe())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette demande n'attend plus d'accord (déjà accordée, annulée ou expirée).");
        }
        if (auteur == null || auteur.equals(l.getCreePar()) || auteur.equals(l.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "L'accord doit venir d'un autre administrateur que celui qui a demandé le lien (et que le titulaire du compte).");
        }
        OrganizationUserJpaEntity appartenance = comptes.appartenance(l.getUserId(), null)
                .or(() -> comptes.appartenance(l.getUserId(), "ETUDIANT"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte introuvable dans cet établissement."));
        UserJpaEntity compte = comptes.membre(l.getUserId(), appartenance.getRole());
        String numero = "EMAIL".equals(l.getCanal()) ? null : NumeroTelephone.e164(compte.getTelephone()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.CONFLICT, "Le numéro de mobile du compte n'est plus valide."));
        boolean privilegie = ROLES_PRIVILEGIES.contains(appartenance.getRole() == null ? "" : appartenance.getRole().toUpperCase());
        l.setAccordePar(auteur);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("lienId", l.getId());
        res.put("type", l.getType());
        res.put("canal", l.getCanal());
        res.put("destinataire", numero == null ? masquerEmail(compte.getEmail()) : NumeroTelephone.masquer(numero));
        res.putAll(delivrer(l, compte, numero, privilegie, maintenant));
        log.info("[LIEN D'ACCÈS] Lien {} accordé par {} (demandé par {})", l.getId(), auteur, l.getCreePar());
        return res;
    }

    /** Demandes de l'école en attente d'accord (sans jeton). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> enAttente() {
        String ecole = comptes.ecoleCourante();
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        return liens.findByOrganizationIdAndAccordRequisTrueAndAccordeParIsNullAndUtiliseLeIsNullAndAnnuleLeIsNull(ecole).stream()
                .filter(l -> maintenant.isBefore(l.getExpireLe()))
                .map(l -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("lienId", l.getId());
                    m.put("userId", l.getUserId());
                    m.put("type", l.getType());
                    m.put("canal", l.getCanal());
                    m.put("destinataire", l.getNumeroMasque());
                    m.put("demandePar", l.getCreePar());
                    m.put("demandeLe", l.getCreeLe().toString());
                    return m;
                }).toList();
    }

    private Map<String, Object> delivrer(ReinitialisationMotDePasseJpaEntity l, UserJpaEntity compte, String numero,
                                         boolean privilegie, LocalDateTime maintenant) {
        String jeton = nouveauJeton();
        l.setJetonHache(ReinitialisationMotDePasseService.hacher(jeton));
        int heures = "ACTIVATION".equals(l.getType()) ? ACTIVATION_HEURES : REINIT_HEURES;
        l.setExpireLe(maintenant.plusHours(heures));
        String code = null;
        if (privilegie) {
            code = String.format("%06d", aleatoire.nextInt(1_000_000));
            l.setCodeHache(encodeur.encode(code));
            l.setEssaisCode(0);
        }
        liens.save(l);

        String slug = ecoles.findById(l.getOrganizationId()).map(o -> o.getSlug()).orElse(null);
        String nomEcole = ecoles.findById(l.getOrganizationId()).map(o -> o.getName()).orElse("votre école");
        String lien = urlFrontend + (slug == null || slug.isBlank() ? "" : "/t/" + slug) + "/reinitialiser-mot-de-passe?jeton=" + jeton;
        String prenom = compte.getPrenom() == null ? "" : " " + compte.getPrenom();
        String message = "Bonjour" + prenom + ", " + nomEcole + " vous envoie votre lien "
                + ("ACTIVATION".equals(l.getType()) ? "d'activation de compte" : "pour choisir un nouveau mot de passe")
                + " (valable " + heures + " h, une seule fois) : " + lien
                + (privilegie ? " Un code de 6 chiffres vous sera communiqué de vive voix." : "")
                + " Si vous n'attendiez pas ce message, ignorez-le.";

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("expireLe", l.getExpireLe().toString());
        switch (l.getCanal()) {
            case "WHATSAPP" -> {
                res.put("message", message);
                res.put("adresseEnvoi", "https://wa.me/" + NumeroTelephone.chiffres(numero) + "?text=" + encoder(message));
            }
            case "SMS" -> {
                res.put("message", message);
                res.put("adresseEnvoi", "sms:" + numero + "?body=" + encoder(message));
            }
            default -> {
                boolean envoye = email.sendEmail(compte.getEmail(),
                        "ACTIVATION".equals(l.getType()) ? "Activation de votre compte" : "Choisir un nouveau mot de passe", message);
                res.put("envoye", envoye);
                if (!envoye) {
                    log.warn("[LIEN D'ACCÈS] E-mail du lien {} NON envoyé (canal d'envoi indisponible)", l.getId());
                }
            }
        }
        if (code != null) {
            res.put("codeOral", code);   // affiché une seule fois à l'administrateur ; jamais dans le message
            res.put("consigne", "Communiquez ce code de vive voix (appel, en personne), jamais dans le même message que le lien.");
        }
        return res;
    }

    private void annulerPrecedents(String userId, LocalDateTime maintenant) {
        for (ReinitialisationMotDePasseJpaEntity ancien : liens.findByUserIdAndUtiliseLeIsNull(userId)) {
            if (ancien.getAnnuleLe() == null) {
                ancien.setAnnuleLe(maintenant);
                liens.save(ancien);
            }
        }
    }

    private String nouveauJeton() {
        byte[] octets = new byte[32];
        aleatoire.nextBytes(octets);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
    }

    private static String encoder(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    static String masquerEmail(String e) {
        if (e == null || !e.contains("@")) return null;
        int at = e.indexOf('@');
        return e.charAt(0) + "•••" + e.substring(at);
    }

    private static String auteur() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        if (auth.getPrincipal() instanceof ecole_221.sn.ecole.modules.auth.domain.model.User u) return u.getId();
        return auth.getName();
    }
}
