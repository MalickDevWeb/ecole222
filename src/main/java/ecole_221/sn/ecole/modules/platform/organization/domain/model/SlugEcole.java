package ecole_221.sn.ecole.modules.platform.organization.domain.model;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Identifiant d'école (slug) : il sert à la fois dans les adresses /t/&lt;slug&gt; et comme sous-domaine
 * &lt;slug&gt;.&lt;domaine-plateforme&gt; (décision du Tech Lead du 15/11 : domaine propre par simple pointage DNS).
 *
 * <p>Règle unique, appliquée à l'inscription publique (défaut F3) et à la création par le super-administrateur (F2) :
 * 3 à 40 caractères, lettres minuscules sans accent, chiffres et tirets simples entre deux mots
 * ({@code ^[a-z0-9]+(-[a-z0-9]+)*$}), jamais un nom réservé par la plateforme.</p>
 */
public final class SlugEcole {

    public static final int LONGUEUR_MIN = 3;
    public static final int LONGUEUR_MAX = 40;
    private static final Pattern FORME = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    /**
     * Noms d'INFRASTRUCTURE : sous-domaines de la plateforme et services techniques (revue DevSecOps du 15/11).
     * Jamais attribués à une école ni résolus comme école ({@code www.<domaine>} n'est pas une école).
     * « par-hote » : la route publique /public/tenants/par-hote masquerait la fiche d'une école portant ce nom.
     */
    public static final Set<String> RESERVES_INFRASTRUCTURE = Set.of(
            "www", "app", "api", "admin", "super-admin", "administration", "mail", "smtp", "static", "cdn", "assets",
            "status", "support", "docs", "blog", "platform", "plateforme", "auth", "login", "connexion", "sso", "compte",
            "webmail", "mx", "ns1", "ns2", "media", "ws", "aide", "billing", "facturation", "paiement", "paiements", "pay",
            "wave", "orange-money", "test", "staging", "dev", "demo", "localhost", "autoconfig", "autodiscover", "mta-sts",
            "par-hote");

    /**
     * Noms interdits à la CRÉATION d'une école : l'infrastructure, plus les noms de l'École 221 (anti-usurpation ;
     * l'école existante « ecole-221 » reste servie normalement).
     */
    public static final Set<String> RESERVES;

    static {
        java.util.Set<String> tous = new java.util.HashSet<>(RESERVES_INFRASTRUCTURE);
        tous.add("ecole221");
        tous.add("ecole-221");
        RESERVES = java.util.Collections.unmodifiableSet(tous);
    }

    private SlugEcole() {
    }

    /** Préfixes interdits à la création (anti-usurpation : « ecole-221-dakar », « ecole221-paiements »…). */
    public static final java.util.List<String> PREFIXES_RESERVES = java.util.List.of("ecole221", "ecole-221");

    /** Interdit à la création d'une école : nom réservé ou préfixe de l'École 221. */
    public static boolean reserve(String slug) {
        return slug != null && (RESERVES.contains(slug) || PREFIXES_RESERVES.stream().anyMatch(slug::startsWith));
    }

    /** Nom d'infrastructure : jamais résolu comme école, même si une école ancienne le portait. */
    public static boolean reserveInfrastructure(String etiquette) {
        return etiquette != null && RESERVES_INFRASTRUCTURE.contains(etiquette);
    }

    /** Forme correcte (sans tenir compte des noms réservés). */
    public static boolean formeValide(String slug) {
        return slug != null && slug.length() >= LONGUEUR_MIN && slug.length() <= LONGUEUR_MAX
                && FORME.matcher(slug).matches();
    }

    /** Message d'erreur en français, ou null si l'identifiant est utilisable. */
    public static String erreur(String slug) {
        if (slug == null || slug.isBlank()) {
            return "L'identifiant de l'école est obligatoire.";
        }
        if (slug.startsWith("xn--")) {
            return "Identifiant d'école invalide : le préfixe « xn-- » est réservé aux noms de domaine internationaux.";
        }
        if (!formeValide(slug)) {
            String suggestion = depuisNom(slug);
            return "Identifiant d'école invalide : " + LONGUEUR_MIN + " à " + LONGUEUR_MAX + " caractères, lettres "
                    + "minuscules sans accent, chiffres et tirets entre deux mots"
                    + (formeValide(suggestion) && !reserve(suggestion) && !suggestion.equals(slug)
                        ? " (par exemple « " + suggestion + " »)." : " (par exemple « lycee-de-thies »).");
        }
        if (reserve(slug)) {
            return "L'identifiant « " + slug + " » est réservé par la plateforme : choisissez-en un autre.";
        }
        return null;
    }

    /**
     * Identifiant fabriqué à partir du nom de l'école : accents retirés (« Lycée de Thiès » → « lycee-de-thies »),
     * tout autre caractère remplacé par un tiret, 40 caractères au plus. Peut être trop court ou réservé :
     * {@link #libre} complète.
     */
    public static String depuisNom(String nom) {
        if (nom == null) {
            return "";
        }
        String sansAccents = Normalizer.normalize(nom, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        String s = sansAccents.toLowerCase(Locale.ROOT)
                .replace("œ", "oe").replace("æ", "ae").replace("ß", "ss")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return couper(s, LONGUEUR_MAX);
    }

    /** Base d'identifiant refusée (nom réservé ou préfixe de l'École 221) : jamais suffixée. */
    public static class BaseReservee extends IllegalArgumentException {
        public BaseReservee(String message) {
            super(message);
        }
    }

    /**
     * Premier identifiant libre à partir d'une base : la base elle-même, puis base-2, base-3… (prévisible ; avant :
     * suffixe aléatoire) — pour une collision ORDINAIRE seulement. Une base vide ou trop courte devient
     * « ecole[-base] ». Revue DevSecOps phase 2 (B4) : une base réservée ou commençant par ecole221 / ecole-221 est
     * REFUSÉE ({@link BaseReservee}) au lieu d'être suffixée (« École 221 » ne donne jamais « ecole-221-2 »).
     */
    public static String libre(String base, Predicate<String> pris) {
        String b = base == null ? "" : base;
        if (b.isEmpty()) {
            b = "ecole";
        } else if (b.length() < LONGUEUR_MIN) {
            b = "ecole-" + b;
        }
        if (reserve(b)) {
            throw new BaseReservee("Le nom de l'établissement donne l'identifiant réservé « " + b + " » : choisissez un "
                    + "autre nom (ou, à l'inscription, un autre identifiant).");
        }
        if (!pris.test(b)) {
            return b;
        }
        for (int i = 2; i < 10_000; i++) {
            String suffixe = "-" + i;
            String candidat = couper(b, LONGUEUR_MAX - suffixe.length()) + suffixe;
            if (!reserve(candidat) && !pris.test(candidat)) {
                return candidat;
            }
        }
        throw new IllegalStateException("Aucun identifiant libre trouvé pour « " + b + " ».");
    }

    private static String couper(String s, int max) {
        String r = s.length() > max ? s.substring(0, max) : s;
        return r.replaceAll("-+$", "");
    }
}
