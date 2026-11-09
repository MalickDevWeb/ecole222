package ecole_221.sn.ecole.infrastructure.logging;

import java.util.regex.Pattern;

/**
 * Masquage des données personnelles et secrets avant écriture dans les journaux (APP-SEC-13).
 * Utilisé par les aspects de journalisation : emails, téléphones, jetons JWT/Bearer et
 * champs de type mot de passe ne doivent jamais apparaître en clair.
 */
public final class LogMasker {

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern JWT = Pattern.compile("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");
    private static final Pattern BEARER = Pattern.compile("(?i)bearer\\s+[A-Za-z0-9._~+/=-]+");
    /** +221 ou numéro national à 9 chiffres (7x / 3x), espaces et tirets tolérés. */
    private static final Pattern TELEPHONE =
            Pattern.compile("(?<![\\w-])(?:\\+?221[\\s.-]?)?[37]\\d(?:[\\s.-]?\\d){7}(?![\\w-])");
    /** Valeur entre guillemets (JSON) : masquée jusqu'au guillemet fermant, espaces compris. */
    private static final Pattern CHAMP_SECRET_JSON = Pattern.compile(
            "(?i)((?:password|motDePasse|mot_de_passe|mdp|secret|token|apiKey|credential)\\w*\"\\s*:\\s*\")(?:[^\"\\\\]|\\\\.)*\"");
    private static final Pattern CHAMP_SECRET = Pattern.compile(
            "(?i)(password|motDePasse|mot_de_passe|mdp|secret|token|apiKey|credential)(\"?\\s*[=:]\\s*\"?)[^,\\]}\"\\s]+");

    private static final int LONGUEUR_MAX = 300;

    private LogMasker() {
    }

    /** Texte masqué et tronqué, prêt pour un journal. */
    public static String masquer(Object valeur) {
        if (valeur == null) {
            return "null";
        }
        String s = String.valueOf(valeur);
        s = JWT.matcher(s).replaceAll("[jwt]");
        s = BEARER.matcher(s).replaceAll("Bearer [jeton]");
        s = CHAMP_SECRET_JSON.matcher(s).replaceAll("$1***\"");
        s = CHAMP_SECRET.matcher(s).replaceAll("$1$2***");
        s = EMAIL.matcher(s).replaceAll("[email]");
        s = TELEPHONE.matcher(s).replaceAll("[tel]");
        return s.length() > LONGUEUR_MAX ? s.substring(0, LONGUEUR_MAX) + "… [tronqué]" : s;
    }
}
