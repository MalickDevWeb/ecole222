package ecole_221.sn.ecole.modules.platform.organization.application;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.DomaineEcoleJpaEntity;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.DomaineEcoleJpaRepository;
import ecole_221.sn.ecole.shared.infrastructure.tenant.domaine.RegistreDomaines;
import ecole_221.sn.ecole.shared.infrastructure.tenant.domaine.ResolutionHoteEcole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Domaines propres des écoles (décision du Tech Lead du 15/11 : POINTAGE + DÉCLARATION).
 *
 * <p>L'administrateur déclare {@code portail.mon-ecole.sn} ; le serveur vérifie aussitôt que la chaîne CNAME mène à
 * {@code <slug>.<domaine-plateforme>} (ACTIF) ou explique l'enregistrement attendu (EN_ATTENTE_POINTAGE). Les domaines
 * sont revérifiés périodiquement : {@value #ECHECS_AVANT_DESACTIVATION} échecs consécutifs → DESACTIVE (plus servi) ;
 * un domaine qui pointe de nouveau redevient ACTIF à la vérification suivante. Au plus {@value #QUOTA} domaines par
 * école ; un hostname = une seule école.</p>
 */
@Service
public class DomainesEcoleService {

    private static final Logger log = LoggerFactory.getLogger(DomainesEcoleService.class);
    public static final int QUOTA = 3;
    public static final int ECHECS_AVANT_DESACTIVATION = 3;

    private final DomaineEcoleJpaRepository domaines;
    private final OrganizationRepositoryPort ecoles;
    private final ResolutionHoteEcole resolution;
    Clock horloge = Clock.systemDefaultZone();

    public DomainesEcoleService(DomaineEcoleJpaRepository domaines, OrganizationRepositoryPort ecoles, ResolutionHoteEcole resolution) {
        this.domaines = domaines;
        this.ecoles = ecoles;
        this.resolution = resolution;
    }

    // ── Lecture ─────────────────────────────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> lister(Organization ecole) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("adresseEcole", resolution.adresseEcole(ecole.getSlug()));
        r.put("cible", resolution.cible(ecole.getSlug()));
        r.put("domaines", domaines.findByOrganizationIdOrderByCreatedAtAsc(ecole.getId()).stream().map(d -> {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("domaine", d.getHostname());
            e.put("statut", d.getStatut());
            e.put("pointeVersCetteEcole", RegistreDomaines.ACTIF.equals(d.getStatut()));
            e.put("verifieLe", d.getVerifieLe());
            e.put("message", message(d, ecole, RegistreDomaines.ACTIF.equals(d.getStatut()), false));
            return e;
        }).toList());
        return r;
    }

    // ── Déclaration, vérification, suppression ──────────────────────────────────────────────────────────────────

    /** 201 : {domaine, statut, pointeVersCetteEcole, message}. 400 adresse refusée ; 409 déjà déclaré ou quota atteint. */
    @Transactional
    public Map<String, Object> declarer(Organization ecole, String saisi, String creePar) {
        String h = domaineDeclarable(saisi);
        Optional<DomaineEcoleJpaEntity> existant = domaines.findByHostname(h);
        if (existant.isPresent()) {
            if (ecole.getId().equals(existant.get().getOrganizationId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Ce domaine est déjà déclaré pour votre établissement : utilisez « Vérifier » pour contrôler son pointage.");
            }
            return recuperer(existant.get(), ecole, creePar);
        }
        verifierQuota(ecole);
        DomaineEcoleJpaEntity d = new DomaineEcoleJpaEntity();
        d.setId(UUID.randomUUID().toString());
        d.setOrganizationId(ecole.getId());
        d.setHostname(h);
        d.setStatut(RegistreDomaines.EN_ATTENTE_POINTAGE);
        d.setEchecsConsecutifs(0);
        d.setCreePar(creePar);
        d.setCreatedAt(LocalDateTime.now(horloge));
        resolution.oublier(h);
        return verifierEtEnregistrer(d, ecole);
    }

    /**
     * Revue DevSecOps phase 2 (B7, accaparement) : une école B peut déclarer le domaine de l'école A avant elle. Si la
     * déclaration existante n'est pas ACTIVE et que le CNAME, lu à neuf, mène à l'école qui demande, la déclaration
     * lui est TRANSFÉRÉE (journalisé) : le propriétaire du domaine (seul à pouvoir poser le CNAME) l'emporte. Sinon 409,
     * sans nommer l'autre établissement.
     */
    private Map<String, Object> recuperer(DomaineEcoleJpaEntity d, Organization ecole, String creePar) {
        String h = d.getHostname();
        boolean recuperable = false;
        if (!RegistreDomaines.ACTIF.equals(d.getStatut())) {
            try {
                recuperable = ecole.getSlug() != null && ecole.getSlug().equals(canon(resolution.slugDuPointageStrict(h).orElse(null)));
            } catch (RuntimeException e) {
                log.warn("[DOMAINES] Pointage de « {} » illisible pour l'instant : pas de transfert ({})", h, e.toString());
            }
        }
        if (!recuperable) {
            log.warn("[DOMAINES] « {} » déjà déclaré par une autre école (demande de l'école {})", h, ecole.getId());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce domaine est déjà déclaré par un autre établissement.");
        }
        verifierQuota(ecole);
        log.warn("[DOMAINES] « {} » TRANSFÉRÉ de l'école {} (statut {}) à l'école {} : son CNAME mène à « {} »",
                h, d.getOrganizationId(), d.getStatut(), ecole.getId(), resolution.cible(ecole.getSlug()));
        d.setOrganizationId(ecole.getId());
        d.setCreePar(creePar);
        d.setStatut(RegistreDomaines.EN_ATTENTE_POINTAGE);
        d.setEchecsConsecutifs(0);
        d.setVerifieLe(null);
        d.setCreatedAt(LocalDateTime.now(horloge));
        resolution.oublier(h);
        return verifierEtEnregistrer(d, ecole);
    }

    private void verifierQuota(Organization ecole) {
        if (domaines.countByOrganizationId(ecole.getId()) >= QUOTA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Quota atteint : " + QUOTA + " domaines au plus par établissement. Supprimez-en un avant d'en déclarer un autre.");
        }
    }

    private static String canon(String slug) {
        return "ecole221".equals(slug) ? "ecole-221" : slug;
    }

    /** 200 : revérifie maintenant. 404 si le domaine n'est pas déclaré par CETTE école. */
    @Transactional
    public Map<String, Object> verifier(Organization ecole, String saisi) {
        return verifierEtEnregistrer(duneEcole(ecole, saisi), ecole);
    }

    /** 204 : le domaine cesse d'être servi aussitôt (mémoire du résolveur vidée pour ce nom). */
    @Transactional
    public void supprimer(Organization ecole, String saisi) {
        DomaineEcoleJpaEntity d = duneEcole(ecole, saisi);
        domaines.delete(d);
        resolution.oublier(d.getHostname());
        log.info("[DOMAINES] « {} » retiré par l'école {}", d.getHostname(), ecole.getId());
    }

    /**
     * Revérification périodique de tous les domaines déclarés (voir VerificationPeriodiqueDomaines).
     * N4 (revue phase 2) : SANS transaction englobante — les lectures DNS (jusqu'à 10 s par domaine) ne tiennent
     * aucune connexion à la base ; chaque enregistrement est fait par le dépôt dans sa propre transaction.
     */
    public int verifierTout() {
        int n = 0;
        for (DomaineEcoleJpaEntity d : domaines.findAll()) {
            Organization ecole = ecoles.findById(d.getOrganizationId()).orElse(null);
            if (ecole == null) {
                continue;
            }
            try {
                verifierEtEnregistrer(d, ecole);
                n++;
            } catch (RuntimeException e) {
                log.warn("[DOMAINES] Vérification impossible pour « {} » : {}", d.getHostname(), e.toString());
            }
        }
        return n;
    }

    // ── Règles ──────────────────────────────────────────────────────────────────────────────────────────────────

    /** Adresse normalisée (A-label minuscule) ou 400 : invalide, punycode mal formé, adresse IP, domaine de la plateforme. */
    public String domaineDeclarable(String saisi) {
        if (saisi == null || saisi.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saisissez le domaine de votre école, par exemple portail.mon-ecole.sn.");
        }
        String h = ResolutionHoteEcole.normaliser(sansSchema(saisi));
        if (h == null || !h.contains(".")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Adresse invalide : saisissez un nom de domaine, par exemple portail.mon-ecole.sn.");
        }
        if (ResolutionHoteEcole.estAdresseIp(h)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Une adresse IP ne peut pas être déclarée : saisissez un nom de domaine, par exemple portail.mon-ecole.sn.");
        }
        if (resolution.estHotePlateforme(h) || resolution.estSousDomainePlateforme(h) || h.equals(resolution.domainePlateforme())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "« " + h + " » est une adresse de la plateforme : saisissez le "
                    + "domaine de votre école (par exemple portail.mon-ecole.sn).");
        }
        return h;
    }

    private DomaineEcoleJpaEntity duneEcole(Organization ecole, String saisi) {
        String h = ResolutionHoteEcole.normaliser(sansSchema(saisi == null ? "" : saisi));
        return Optional.ofNullable(h).flatMap(domaines::findByHostname)
                .filter(d -> ecole.getId().equals(d.getOrganizationId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Domaine introuvable pour cet établissement."));
    }

    private Map<String, Object> verifierEtEnregistrer(DomaineEcoleJpaEntity d, Organization ecole) {
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        String avant = d.getStatut();
        String trouve;
        try {
            trouve = canon(resolution.slugDuPointageStrict(d.getHostname()).orElse(null));
        } catch (RuntimeException e) {
            // critère 5 : panne DNS → ni échec compté, ni changement de statut ; l'administrateur réessaie
            log.warn("[DOMAINES] Pointage de « {} » illisible pour l'instant : {}", d.getHostname(), e.toString());
            if (d.getCreatedAt() != null && domaines.findByHostname(d.getHostname()).isEmpty()) {
                domaines.save(d);   // nouvelle déclaration : enregistrée EN_ATTENTE_POINTAGE
            }
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("domaine", d.getHostname());
            r.put("statut", d.getStatut());
            r.put("pointeVersCetteEcole", false);
            r.put("message", "Le pointage de « " + d.getHostname() + " » n'a pas pu être lu pour l'instant (DNS injoignable). "
                    + "Réessayez dans quelques minutes avec « Vérifier ».");
            return r;
        }
        boolean pointe = ecole.getSlug() != null && ecole.getSlug().equals(trouve);
        d.setDerniereVerification(maintenant);
        if (pointe) {
            d.setStatut(RegistreDomaines.ACTIF);
            d.setEchecsConsecutifs(0);
            d.setVerifieLe(maintenant);
        } else {
            d.setEchecsConsecutifs(d.getEchecsConsecutifs() + 1);
            if (d.getEchecsConsecutifs() >= ECHECS_AVANT_DESACTIVATION) {
                d.setStatut(RegistreDomaines.DESACTIVE);
            }
        }
        domaines.save(d);
        resolution.oublier(d.getHostname());
        if (!d.getStatut().equals(avant)) {
            log.info("[DOMAINES] « {} » (école {}) : {} → {}", d.getHostname(), ecole.getId(), avant, d.getStatut());
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("domaine", d.getHostname());
        r.put("statut", d.getStatut());
        r.put("pointeVersCetteEcole", pointe);
        r.put("message", message(d, ecole, pointe, trouve != null && !pointe));
        return r;
    }

    private String message(DomaineEcoleJpaEntity d, Organization ecole, boolean pointe, boolean autreEcole) {
        String h = d.getHostname();
        String cible = resolution.cible(ecole.getSlug());
        if (pointe) {
            return "Le domaine « " + h + " » pointe bien vers votre établissement : il est servi en https://" + h
                    + " (certificat HTTPS créé automatiquement à la première visite)."
                    + (ecole.isActive() ? "" : " Il ne sera servi qu'une fois votre établissement validé par la plateforme.");
        }
        if (RegistreDomaines.DESACTIVE.equals(d.getStatut())) {
            return "Le domaine « " + h + " » n'est plus servi : " + ECHECS_AVANT_DESACTIVATION + " vérifications de suite n'ont pas "
                    + "trouvé l'enregistrement « " + h + " CNAME " + cible + " ». Corrigez-le chez votre registraire puis cliquez sur « Vérifier ».";
        }
        if (autreEcole) {
            return "Le domaine « " + h + " » pointe vers un autre établissement. Chez votre registraire, remplacez la cible de "
                    + "l'enregistrement CNAME par « " + cible + " ».";
        }
        return "Aucun enregistrement CNAME menant à « " + cible + " » n'a été trouvé pour « " + h + " ». Chez votre registraire, "
                + "ajoutez : " + h + " CNAME " + cible + ". Un changement DNS peut mettre de quelques minutes à quelques heures "
                + "à se propager ; cliquez ensuite sur « Vérifier »." + conseilApex(h);
    }

    /** Domaine nu (deux étiquettes, ex. mon-ecole.sn) : pas de CNAME possible (norme DNS). */
    public static String conseilApex(String h) {
        return h.split("\\.").length == 2
                ? " « " + h + " » est un domaine nu : la norme DNS n'y permet pas de CNAME. Utilisez un sous-domaine (www." + h
                  + " ou portail." + h + ") et, chez votre registraire, redirigez " + h + " vers ce sous-domaine."
                : "";
    }

    /** « https://portail.x.sn/ » collé depuis le navigateur → « portail.x.sn ». */
    public static String sansSchema(String saisi) {
        String s = saisi.trim().replaceFirst("(?i)^https?://", "");
        int fin = s.indexOf('/');
        return fin >= 0 ? s.substring(0, fin) : s;
    }
}
