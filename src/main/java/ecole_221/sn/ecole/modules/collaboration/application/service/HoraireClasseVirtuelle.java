package ecole_221.sn.ecole.modules.collaboration.application.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;

import static ecole_221.sn.ecole.partage.ChampsRequete.refus;

/**
 * Horaire d'une classe virtuelle (16/11, demande du Tech Lead) : le professeur fixe une heure de DÉBUT, à partir de
 * laquelle le lien apparaît aux élèves et aux parents, et une heure de FIN, après laquelle la classe virtuelle disparaît
 * de leur écran. Avant : date et heure en texte libre, facultatives, et lien visible en permanence.
 * Heure de l'école : Dakar (UTC, sans heure d'été).
 */
public final class HoraireClasseVirtuelle {

    public static final ZoneId FUSEAU = ZoneId.of("Africa/Dakar");
    public static final String A_VENIR = "a_venir";
    public static final String EN_COURS = "en_cours";
    public static final String TERMINEE = "terminee";
    public static final String SANS_HORAIRE = "sans_horaire";
    static final Duration DUREE_MAX = Duration.ofHours(12);
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH);

    private HoraireClasseVirtuelle() {
    }

    public record Horaire(LocalDateTime debut, LocalDateTime fin) {
    }

    public static LocalDateTime maintenant() {
        return LocalDateTime.now(FUSEAU);
    }

    /** Lit et contrôle {debut, fin} (AAAA-MM-JJTHH:MM) : obligatoires, fin après le début et à venir, 12 h au plus. */
    public static Horaire lire(Map<String, ?> corps, LocalDateTime maintenant) {
        LocalDateTime debut = dateHeure(corps.get("debut"), "L'heure de début");
        LocalDateTime fin = dateHeure(corps.get("fin"), "L'heure de fin");
        if (!fin.isAfter(debut)) {
            throw refus("L'heure de fin doit être après l'heure de début.");
        }
        if (!fin.isAfter(maintenant)) {
            throw refus("L'heure de fin est déjà passée : la classe virtuelle ne serait jamais visible.");
        }
        if (Duration.between(debut, fin).compareTo(DUREE_MAX) > 0) {
            throw refus("Une classe virtuelle dure 12 heures au plus.");
        }
        if (debut.isAfter(maintenant.plusYears(1))) {
            throw refus("L'heure de début doit être dans l'année qui vient.");
        }
        return new Horaire(debut.withSecond(0).withNano(0), fin.withSecond(0).withNano(0));
    }

    private static LocalDateTime dateHeure(Object valeur, String libelle) {
        if (!(valeur instanceof String texte) || texte.isBlank()) {
            throw refus(libelle + " est obligatoire.");
        }
        try {
            return LocalDateTime.parse(texte.trim().length() > 19 ? texte.trim().substring(0, 19) : texte.trim());
        } catch (DateTimeParseException e) {
            throw refus(libelle + " est invalide (format attendu : 2026-11-16T14:00).");
        }
    }

    /** à venir, en cours, terminée ; « sans horaire » pour les séances créées avant V26. */
    public static String statut(LocalDateTime debut, LocalDateTime fin, LocalDateTime maintenant) {
        if (debut == null || fin == null) return SANS_HORAIRE;
        if (maintenant.isBefore(debut)) return A_VENIR;
        return maintenant.isAfter(fin) ? TERMINEE : EN_COURS;
    }

    /** « le 16/11/2026 de 14:00 à 15:30 » (ou « du … au … » sur deux jours). */
    public static String libelle(LocalDateTime debut, LocalDateTime fin) {
        if (debut.toLocalDate().equals(fin.toLocalDate())) {
            return "le " + JOUR.format(debut) + " de " + HEURE.format(debut) + " à " + HEURE.format(fin);
        }
        return "du " + JOUR.format(debut) + " " + HEURE.format(debut) + " au " + JOUR.format(fin) + " " + HEURE.format(fin);
    }

    public static String jour(LocalDateTime t) {
        return JOUR.format(t);
    }

    public static String plageHoraire(LocalDateTime debut, LocalDateTime fin) {
        return HEURE.format(debut) + " – " + HEURE.format(fin);
    }
}
