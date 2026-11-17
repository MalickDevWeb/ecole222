package ecole_221.sn.ecole.shared.infrastructure.tenant.domaine;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.SlugEcole;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.IDN;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Hôte → école (décisions du Tech Lead du 15/11 : domaine propre par pointage DNS, puis « pointage + déclaration »).
 *
 * <ul>
 *   <li>{@code <slug>.<domaine-plateforme>} → slug (noms d'infrastructure exclus : www, app, api, admin…) ;</li>
 *   <li>domaine propre : servi seulement s'il est DÉCLARÉ par un administrateur de l'école (table domaines_ecoles,
 *       statut autre que DESACTIVE) ET que sa chaîne CNAME (au plus {@value #SAUTS_MAX} sauts) mène exactement à
 *       {@code <slug>.<domaine-plateforme>} de CETTE école. Un nom non déclaré ne déclenche aucune lecture DNS ;</li>
 *   <li>la table {@code ecole.dns.statique} (tests et QA) remplace le vrai DNS pour les noms qu'elle contient ;</li>
 *   <li>mémoire des lectures DNS (revue DevSecOps) : LRU bornée, 10 minutes si trouvé, 60 secondes sinon ; une seule
 *       lecture en cours par nom ; boucle de CNAME détectée ; un échec DNS n'associe jamais un nom à une école.</li>
 * </ul>
 *
 * <p>Le résolveur dit seulement quelle école une adresse désigne ; l'appartenance du compte à l'école reste
 * contrôlée par TenantMembershipInterceptor, et le statut de l'école par StatutEcoleFilter.</p>
 */
@Component
public class ResolutionHoteEcole {

    private static final Logger log = LoggerFactory.getLogger(ResolutionHoteEcole.class);

    public static final int SAUTS_MAX = 5;
    static final Duration DUREE_TROUVE = Duration.ofMinutes(10);
    static final Duration DUREE_ABSENT = Duration.ofSeconds(60);
    static final int TAILLE_MAX_CACHE = 10_000;
    /** Attente maximale d'une lecture déjà en cours pour le même nom (5 sauts × délai DNS de 2 s, plus une marge). */
    static final long ATTENTE_MAX_S = 12;

    private static final Pattern ETIQUETTE = Pattern.compile("^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$");
    private static final Pattern SLUG_ETIQUETTE = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    private static final Pattern IPV4 = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");

    private final OrganizationRepositoryPort ecoles;
    private final ResolveurDns dns;
    private final RegistreDomaines registre;
    private final String domaine;
    private final Set<String> hotesPlateforme;
    private final Map<String, String> statique;
    /** LRU bornée (ordre d'accès) : la plus ancienne entrée sort quand la mémoire est pleine, jamais de vidage total. */
    private final LinkedHashMap<String, Entree> cache = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Entree> plusAncienne) {
            return size() > TAILLE_MAX_CACHE;
        }
    };
    private final Map<String, CompletableFuture<Optional<String>>> enCours = new ConcurrentHashMap<>();
    Clock horloge = Clock.systemUTC();

    private record Entree(String slug, Instant expire) {
    }

    public ResolutionHoteEcole(OrganizationRepositoryPort ecoles, ResolveurDns dns, RegistreDomaines registre,
                               @Value("${ecole.plateforme.domaine:${PLATFORM_DOMAIN:myschool.sn}}") String domaine,
                               @Value("${ecole.dns.statique:}") String tableStatique,
                               @Value("${app.url-frontend:http://localhost:5173}") String urlFrontend) {
        this.ecoles = ecoles;
        this.dns = dns;
        this.registre = registre;
        String d = normaliser(domaine);
        if (d == null || !d.contains(".")) {
            throw new IllegalStateException("ecole.plateforme.domaine (PLATFORM_DOMAIN) invalide : « " + domaine
                    + " ». Attendu un nom de domaine, par exemple myschool.sn.");
        }
        this.domaine = d;
        Set<String> hotes = new HashSet<>(Set.of(d, "www." + d, "app." + d, "api." + d, "localhost", "127.0.0.1"));
        String hoteFrontend = hoteDeLUrl(urlFrontend);
        if (hoteFrontend != null) {
            hotes.add(hoteFrontend);
        }
        this.hotesPlateforme = Collections.unmodifiableSet(hotes);
        this.statique = Collections.unmodifiableMap(lireTableStatique(tableStatique));
        log.info("Domaine de la plateforme : {} ; hôtes de la plateforme : {} ; table DNS statique : {} entrée(s)",
                this.domaine, this.hotesPlateforme, this.statique.size());
    }

    // ── Normalisation ───────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Nom d'hôte normalisé : minuscules, port et point final retirés, forme punycode (java.net.IDN) ; null si ce
     * n'est pas un nom d'hôte valable (adresse IPv6, caractères interdits, étiquette « xn-- » mal formée…).
     */
    public static String normaliser(String hoteBrut) {
        if (hoteBrut == null) {
            return null;
        }
        String h = hoteBrut.trim();
        if (h.isEmpty() || h.startsWith("[") || h.length() > 300) {
            return null;
        }
        int deuxPoints = h.lastIndexOf(':');
        if (deuxPoints >= 0) {
            String port = h.substring(deuxPoints + 1);
            if (h.indexOf(':') != deuxPoints || !port.matches("\\d{0,5}")) {
                return null;   // IPv6 sans crochets ou valeur étrangère
            }
            h = h.substring(0, deuxPoints);
        }
        while (h.endsWith(".")) {
            h = h.substring(0, h.length() - 1);
        }
        if (h.isEmpty()) {
            return null;
        }
        try {
            h = IDN.toASCII(h);   // sans ALLOW_UNASSIGNED : points de code non attribués refusés
        } catch (IllegalArgumentException e) {
            return null;
        }
        h = h.toLowerCase(Locale.ROOT);
        if (h.length() > 253) {
            return null;
        }
        for (String etiquette : h.split("\\.", -1)) {
            if (!ETIQUETTE.matcher(etiquette).matches() || !punycodeValide(etiquette)) {
                return null;
            }
        }
        return h;
    }

    /** Une étiquette « xn--… » doit se décoder puis se réencoder à l'identique (sinon : punycode mal formé). */
    private static boolean punycodeValide(String etiquette) {
        if (!etiquette.startsWith("xn--")) {
            return true;
        }
        try {
            String unicode = IDN.toUnicode(etiquette);
            return !unicode.equals(etiquette) && IDN.toASCII(unicode).toLowerCase(Locale.ROOT).equals(etiquette);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    static String hoteDeLUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            return normaliser(URI.create(url.trim()).getHost());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Map<String, String> lireTableStatique(String table) {
        Map<String, String> m = new LinkedHashMap<>();
        if (table == null || table.isBlank()) {
            return m;
        }
        for (String paire : table.split(",")) {
            String p = paire.trim();
            if (p.isEmpty()) {
                continue;
            }
            int egal = p.indexOf('=');
            String nom = egal > 0 ? normaliser(p.substring(0, egal)) : null;
            String cible = egal > 0 ? normaliser(p.substring(egal + 1)) : null;
            if (nom == null || cible == null) {
                log.warn("ecole.dns.statique : entrée ignorée « {} » (attendu nom=cible)", p);
                continue;
            }
            m.put(nom, cible);
        }
        return m;
    }

    // ── Hôtes de la plateforme et sous-domaines ─────────────────────────────────────────────────────────────────

    public String domainePlateforme() {
        return domaine;
    }

    /** Adresse à viser par le CNAME d'une école : {@code <slug>.<domaine-plateforme>}. */
    public String cible(String slug) {
        return slug + "." + domaine;
    }

    /** Adresse de l'école sur la plateforme : {@code https://<slug>.<domaine-plateforme>}. */
    public String adresseEcole(String slug) {
        return "https://" + cible(slug);
    }

    /** Hôte (normalisé) de la plateforme elle-même : domaine nu, www, app, api, localhost, hôte de FRONTEND_URL. */
    public boolean estHotePlateforme(String hoteNormalise) {
        return hoteNormalise != null && hotesPlateforme.contains(hoteNormalise);
    }

    public static boolean estAdresseIp(String hoteNormalise) {
        return hoteNormalise != null && IPV4.matcher(hoteNormalise).matches();
    }

    /** Hôte normalisé situé SOUS le domaine de la plateforme (quel qu'il soit). */
    public boolean estSousDomainePlateforme(String hoteNormalise) {
        return hoteNormalise != null && hoteNormalise.endsWith("." + domaine);
    }

    /** {@code <slug>.<domaine>} → slug ; vide pour un nom d'infrastructure, plusieurs niveaux ou un autre domaine. */
    public Optional<String> slugDuSousDomaine(String hoteNormalise) {
        if (!estSousDomainePlateforme(hoteNormalise) || estHotePlateforme(hoteNormalise)) {
            return Optional.empty();
        }
        String etiquette = hoteNormalise.substring(0, hoteNormalise.length() - domaine.length() - 1);
        if (etiquette.contains(".") || SlugEcole.reserveInfrastructure(etiquette) || !SLUG_ETIQUETTE.matcher(etiquette).matches()) {
            return Optional.empty();
        }
        return Optional.of(etiquette);
    }

    /**
     * Hôte d'ÉCOLE : sous-domaine de la plateforme (hors infrastructure) ou domaine propre déclaré. Sur un tel hôte,
     * l'école est celle de l'hôte ; un en-tête X-Tenant-Slug différent est refusé (TenantResolutionFilter).
     */
    public boolean estHoteDEcole(String hoteBrut) {
        String h = normaliser(hoteBrut);
        if (h == null || estHotePlateforme(h) || estAdresseIp(h) || !h.contains(".")) {
            return false;
        }
        if (estSousDomainePlateforme(h)) {
            return slugDuSousDomaine(h).isPresent();
        }
        return registre.parHote(h).isPresent();
    }

    /**
     * POINT DE BRANCHEMENT de la déclaration (décision du Tech Lead du 15/11) : l'hôte peut-il servir CETTE école ?
     * Sous-domaine : seulement le sien. Domaine propre : déclaré par un administrateur de cette école et non désactivé.
     * Appelé par la résolution, par-hote et l'autorisation de certificat.
     */
    public boolean domaineAutorise(String hoteBrut, Organization ecole) {
        String h = normaliser(hoteBrut);
        if (h == null || ecole == null || ecole.getSlug() == null) {
            return false;
        }
        Optional<String> sousDomaine = slugDuSousDomaine(h);
        if (sousDomaine.isPresent()) {
            return canonique(sousDomaine.get()).equals(ecole.getSlug());
        }
        if (estSousDomainePlateforme(h) || estHotePlateforme(h) || estAdresseIp(h)) {
            return false;
        }
        return registre.parHote(h)
                .filter(d -> ecole.getId().equals(d.organizationId()) && !RegistreDomaines.DESACTIVE.equals(d.statut()))
                .isPresent();
    }

    // ── Résolution ──────────────────────────────────────────────────────────────────────────────────────────────

    /** Slug désigné par l'hôte (sous-domaine, ou domaine déclaré ET pointé vers son école), quel que soit le statut. */
    public Optional<String> slugDepuisHote(String hoteBrut) {
        String h = normaliser(hoteBrut);
        if (h == null || estHotePlateforme(h) || estAdresseIp(h) || !h.contains(".")) {
            return Optional.empty();
        }
        if (estSousDomainePlateforme(h)) {
            return slugDuSousDomaine(h);   // jamais de lecture DNS sous le domaine de la plateforme
        }
        Optional<RegistreDomaines.DomaineDeclare> declare = registre.parHote(h);
        if (declare.isEmpty() || RegistreDomaines.DESACTIVE.equals(declare.get().statut())) {
            return Optional.empty();       // non déclaré : aucune lecture DNS
        }
        Organization ecole = slugDuPointage(h, true).flatMap(this::ecoleParSlug).orElse(null);
        if (ecole == null || !domaineAutorise(h, ecole)) {
            return Optional.empty();       // CNAME absent, ou menant à une autre école que celle qui l'a déclaré
        }
        return Optional.of(ecole.getSlug());
    }

    /**
     * Slug au bout de la chaîne CNAME de l'hôte (sans regarder les déclarations).
     *
     * @param avecCache false : lecture DNS fraîche (vérification d'un domaine), le résultat remplace celui en mémoire.
     */
    public Optional<String> slugDuPointage(String hoteBrut, boolean avecCache) {
        String h = normaliser(hoteBrut);
        if (h == null || estHotePlateforme(h) || estAdresseIp(h) || !h.contains(".")) {
            return Optional.empty();
        }
        if (estSousDomainePlateforme(h)) {
            return slugDuSousDomaine(h);
        }
        if (avecCache) {
            synchronized (cache) {
                Entree e = cache.get(h);
                if (e != null && e.expire().isAfter(horloge.instant())) {
                    return Optional.ofNullable(e.slug());
                }
            }
        }
        CompletableFuture<Optional<String>> nouvelle = new CompletableFuture<>();
        CompletableFuture<Optional<String>> deja = enCours.putIfAbsent(h, nouvelle);
        if (deja != null) {
            try {
                return deja.get(ATTENTE_MAX_S, TimeUnit.SECONDS);   // une seule lecture en cours par nom
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                return Optional.empty();
            }
        }
        Optional<String> slug = Optional.empty();
        try {
            slug = suivreChaine(h);
            ranger(h, slug.orElse(null));
        } catch (RuntimeException e) {
            // lecture impossible (ResolveurDns.Indisponible ou autre) : rien n'est gardé en mémoire, et le nom n'est
            // jamais associé à une école
            log.warn("Lecture de la chaîne CNAME impossible pour {} : {}", h, e.toString());
            slug = Optional.empty();
        } finally {
            nouvelle.complete(slug);
            enCours.remove(h, nouvelle);
        }
        return slug;
    }

    /**
     * Pointage lu à neuf pour la VÉRIFICATION d'un domaine déclaré : comme {@link #slugDuPointage} sans mémoire, mais
     * une lecture impossible lève {@link ResolveurDns.Indisponible} au lieu de répondre « absent » (une panne DNS ne
     * doit pas compter comme un échec de pointage). Le résultat obtenu remplace celui gardé en mémoire.
     */
    public Optional<String> slugDuPointageStrict(String hoteBrut) {
        String h = normaliser(hoteBrut);
        if (h == null || estHotePlateforme(h) || estAdresseIp(h) || !h.contains(".")) {
            return Optional.empty();
        }
        if (estSousDomainePlateforme(h)) {
            return slugDuSousDomaine(h);
        }
        Optional<String> slug = suivreChaine(h);   // ResolveurDns.Indisponible remonte à l'appelant
        ranger(h, slug.orElse(null));
        return slug;
    }

    /** Oublie la lecture gardée en mémoire (domaine supprimé ou revérifié). */
    public void oublier(String hoteBrut) {
        String h = normaliser(hoteBrut);
        if (h != null) {
            synchronized (cache) {
                cache.remove(h);
            }
        }
    }

    /** École désignée par l'hôte, quel que soit son statut. */
    public Optional<Organization> ecoleDepuisHote(String hoteBrut) {
        return slugDepuisHote(hoteBrut).flatMap(this::ecoleParSlug);
    }

    /** École SERVIE par l'hôte : ACTIVE et hôte autorisé pour elle (par-hote, certificat). */
    public Optional<Organization> ecoleServieParHote(String hoteBrut) {
        return ecoleDepuisHote(hoteBrut).filter(Organization::isActive).filter(e -> domaineAutorise(hoteBrut, e));
    }

    /** École par slug (alias historique ecole221 → ecole-221). */
    public Optional<Organization> ecoleParSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }
        return ecoles.findBySlug(canonique(slug));
    }

    static String canonique(String slug) {
        return "ecole221".equals(slug) ? "ecole-221" : slug;
    }

    private Optional<String> suivreChaine(String hote) {
        String courant = hote;
        Set<String> vus = new HashSet<>();
        vus.add(courant);
        for (int saut = 0; saut < SAUTS_MAX; saut++) {
            String cible = statique.containsKey(courant) ? statique.get(courant)
                    : dns.cname(courant).map(ResolutionHoteEcole::normaliser).orElse(null);
            if (cible == null) {
                return Optional.empty();
            }
            Optional<String> slug = slugDuSousDomaine(cible);
            if (slug.isPresent()) {
                return slug;
            }
            if (estSousDomainePlateforme(cible) || estHotePlateforme(cible) || !vus.add(cible)) {
                return Optional.empty();   // nom d'infrastructure de la plateforme, ou boucle de CNAME
            }
            courant = cible;
        }
        return Optional.empty();
    }

    private void ranger(String hote, String slug) {
        Instant maintenant = horloge.instant();
        synchronized (cache) {
            cache.put(hote, new Entree(slug, maintenant.plus(slug != null ? DUREE_TROUVE : DUREE_ABSENT)));
        }
    }

    int tailleCache() {
        synchronized (cache) {
            return cache.size();
        }
    }
}
