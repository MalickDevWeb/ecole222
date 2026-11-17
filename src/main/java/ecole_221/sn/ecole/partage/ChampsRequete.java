package ecole_221.sn.ecole.partage;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Map;

/**
 * Lecture contrôlée des champs d'un corps JSON libre ({@code Map<String, Object>}) — APP-QA-04.
 *
 * <p>Chaque méthode refuse une valeur invalide par une erreur 400 dont le message nomme le champ en français
 * (« Le titre est obligatoire. »).</p>
 */
public final class ChampsRequete {

    private ChampsRequete() {}

    /** Texte obligatoire, espaces retirés, longueur maximale {@code max}. */
    public static String texte(Map<String, ?> corps, String cle, String libelle, int max) {
        String v = texteOptionnel(corps, cle, libelle, max);
        if (v == null) {
            throw refus(libelle + " est obligatoire.");
        }
        return v;
    }

    /** Texte facultatif : null si absent ou vide ; refusé s'il n'est pas un texte ou dépasse {@code max}. */
    public static String texteOptionnel(Map<String, ?> corps, String cle, String libelle, int max) {
        Object v = corps == null ? null : corps.get(cle);
        if (v == null) {
            return null;
        }
        if (!(v instanceof String s)) {
            throw refus(libelle + " doit être un texte.");
        }
        String t = TexteNormalise.normaliser(s);   // invisibles retirés, NFC
        if (t.isEmpty()) {
            return null;
        }
        if (t.length() > max) {
            throw refus(libelle + " ne doit pas dépasser " + max + " caractères.");
        }
        return t;
    }

    /** Nombre entier obligatoire compris entre {@code min} et {@code max} (accepte « 12 » écrit en texte). */
    public static int entier(Map<String, ?> corps, String cle, String libelle, long min, long max) {
        Integer v = entierOptionnel(corps, cle, libelle, min, max);
        if (v == null) {
            throw refus(libelle + " est obligatoire.");
        }
        return v;
    }

    public static Integer entierOptionnel(Map<String, ?> corps, String cle, String libelle, long min, long max) {
        Double d = nombreOptionnel(corps, cle, libelle, min, max);
        if (d == null) {
            return null;
        }
        if (d != Math.rint(d)) {
            throw refus(libelle + " doit être un nombre entier.");
        }
        return d.intValue();
    }

    /** Nombre (décimal accepté) entre {@code min} et {@code max} ; null si absent. */
    public static Double nombreOptionnel(Map<String, ?> corps, String cle, String libelle, double min, double max) {
        Object v = corps == null ? null : corps.get(cle);
        if (v == null || (v instanceof String s && s.isBlank())) {
            return null;
        }
        double d;
        if (v instanceof Number n) {
            d = n.doubleValue();
        } else if (v instanceof String s) {
            try {
                d = Double.parseDouble(s.strip().replace(',', '.'));
            } catch (NumberFormatException e) {
                throw refus(libelle + " doit être un nombre.");
            }
        } else {
            throw refus(libelle + " doit être un nombre.");
        }
        if (Double.isNaN(d) || d < min || d > max) {
            throw refus(libelle + " doit être entre " + format(min) + " et " + format(max) + ".");
        }
        return d;
    }

    /** Booléen facultatif (true / false uniquement). */
    public static Boolean booleenOptionnel(Map<String, ?> corps, String cle, String libelle) {
        Object v = corps == null ? null : corps.get(cle);
        if (v == null) {
            return null;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        throw refus(libelle + " doit valoir oui ou non (true / false).");
    }

    /** Date ou date-heure ISO facultative (2026-11-13, 2026-11-13T10:00, avec ou sans fuseau) ; renvoyée telle qu'écrite. */
    public static String dateOptionnelle(Map<String, ?> corps, String cle, String libelle) {
        String t = texteOptionnel(corps, cle, libelle, 40);
        if (t == null) {
            return null;
        }
        try {
            if (t.length() <= 10) {
                LocalDate.parse(t);
            } else if (t.endsWith("Z") || t.matches(".*[+-]\\d\\d:\\d\\d$")) {
                OffsetDateTime.parse(t);
            } else {
                LocalDateTime.parse(t);
            }
        } catch (DateTimeParseException e) {
            throw refus(libelle + " n'est pas une date valide (format attendu : 2026-11-13 ou 2026-11-13T10:00).");
        }
        return t;
    }

    /** Valeur obligatoirement choisie dans une liste fermée. */
    public static String choix(Map<String, ?> corps, String cle, String libelle, Collection<String> permis, String parDefaut) {
        String t = texteOptionnel(corps, cle, libelle, 100);
        if (t == null) {
            if (parDefaut != null) {
                return parDefaut;
            }
            throw refus(libelle + " est obligatoire.");
        }
        if (!permis.contains(t)) {
            throw refus(libelle + " doit être l'une des valeurs : " + String.join(", ", permis) + ".");
        }
        return t;
    }

    /** Corps absent ou vide : refus explicite plutôt qu'une création vide. */
    public static void corpsRequis(Map<String, ?> corps) {
        if (corps == null || corps.isEmpty()) {
            throw refus("Aucune donnée reçue.");
        }
    }

    public static ResponseStatusException refus(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static String format(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }
}
