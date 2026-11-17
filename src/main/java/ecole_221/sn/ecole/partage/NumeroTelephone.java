package ecole_221.sn.ecole.partage;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Numéro de téléphone au format international E.164 (15/11 : connexion des parents par téléphone, liens d'accès
 * envoyés par WhatsApp ou SMS).
 *
 * <ul>
 *   <li>Sénégal : mobiles seulement, {@code ^\+221(7[05678]\d{7})$} (« 77 123 45 67 », « 00221771234567 » →
 *       « +221771234567 ») ;</li>
 *   <li>diaspora : {@code ^\+[1-9]\d{7,14}$} (indicatif du pays obligatoire, « +33… », « 0033… »).</li>
 * </ul>
 */
public final class NumeroTelephone {

    private static final Pattern SENEGAL = Pattern.compile("^\\+221(7[05678]\\d{7})$");
    private static final Pattern INTERNATIONAL = Pattern.compile("^\\+[1-9]\\d{7,14}$");

    private NumeroTelephone() {
    }

    /** Forme E.164, ou vide si la saisie n'est pas un numéro de mobile accepté. */
    public static Optional<String> e164(String saisi) {
        if (saisi == null) {
            return Optional.empty();
        }
        String s = saisi.trim().replaceAll("[\\s.\\-()/]", "");
        if (s.isEmpty() || s.length() > 20) {
            return Optional.empty();
        }
        if (s.startsWith("00")) {
            s = "+" + s.substring(2);
        } else if (!s.startsWith("+")) {
            if (s.matches("7[05678]\\d{7}")) {
                s = "+221" + s;   // numéro sénégalais saisi sans indicatif
            } else if (s.matches("221\\d{9}")) {
                s = "+" + s;
            } else {
                return Optional.empty();
            }
        }
        if (s.startsWith("+221")) {
            return SENEGAL.matcher(s).matches() ? Optional.of(s) : Optional.empty();
        }
        return INTERNATIONAL.matcher(s).matches() ? Optional.of(s) : Optional.empty();
    }

    /** Vrai si la saisie ressemble à un numéro de téléphone (chiffres, espaces, +, sans « @ »). */
    public static boolean ressembleAUnNumero(String saisi) {
        return saisi != null && !saisi.contains("@") && saisi.trim().matches("^\\+?[0-9 .\\-()]{8,20}$");
    }

    /** « +221771234567 » → « +221 77 ••• •• 67 » (jamais le numéro complet dans un journal ni une réponse). */
    public static String masquer(String e164) {
        if (e164 == null || e164.length() < 8) {
            return null;
        }
        if (e164.startsWith("+221") && e164.length() == 13) {
            return "+221 " + e164.substring(4, 6) + " ••• •• " + e164.substring(11);
        }
        return e164.substring(0, Math.min(4, e164.length() - 2)) + " ••• " + e164.substring(e164.length() - 2);
    }

    /** Chiffres seuls (« +221771234567 » → « 221771234567 »), pour wa.me. */
    public static String chiffres(String e164) {
        return e164 == null ? null : e164.replaceAll("\\D", "");
    }
}
