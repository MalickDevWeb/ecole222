package ecole_221.sn.ecole.partage;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Normalisation des textes saisis (2026-11-13, campagne « erreurs humaines et paranormales »).
 *
 * <p>Avant : « Entree Sud » et « Entree​Sud » (espace de largeur nulle, invisible), ou « Préau » écrit avec un
 * accent composé ou décomposé (Unicode NFC / NFD), étaient acceptés comme deux noms DIFFÉRENTS : la règle d'unicité
 * (terminaux, groupes, classes) était contournée par un simple copier-coller.</p>
 *
 * <p>Règles : forme composée NFC ; caractères de mise en forme invisibles retirés (U+200B…U+200F, U+2060, U+FEFF,
 * marques de direction U+202A…U+202E, trait d'union conditionnel) ; espaces spéciales (insécable, fine insécable…)
 * remplacées par une espace normale ; espaces de début et de fin retirées. Les retours à la ligne sont conservés.</p>
 */
public final class TexteNormalise {

    private TexteNormalise() {}

    public static String normaliser(String s) {
        if (s == null) {
            return null;
        }
        String n = Normalizer.normalize(s, Normalizer.Form.NFC);
        StringBuilder b = new StringBuilder(n.length());
        n.codePoints().forEach(cp -> {
            int type = Character.getType(cp);
            if (type == Character.FORMAT) {
                return;                                   // invisibles : U+200B, U+200D, U+FEFF, U+202E…
            }
            if (type == Character.SPACE_SEPARATOR) {
                b.append(' ');                            // U+00A0, U+202F, U+2009… → espace normale
                return;
            }
            b.appendCodePoint(cp);
        });
        return b.toString().strip();
    }

    /** Clé de comparaison pour l'unicité : normalisé, minuscules, espaces multiples réduites à une. */
    public static String cle(String s) {
        String n = normaliser(s);
        return n == null ? null : n.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
