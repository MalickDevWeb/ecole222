package ecole_221.sn.ecole.modules.admin.domain.model;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Contrôle (pur) des doubles réservations dans un emploi du temps, appliqué par le SERVEUR à l'enregistrement.
 * Avant : seul le navigateur signalait les conflits ; un enregistrement direct ou concurrent les acceptait.
 *
 * <p>Deux créneaux du même jour se chevauchent si leurs plages [début, fin) se recouvrent (sans heure de fin :
 * même heure de début). Sont des conflits :</p>
 * <ul>
 *   <li><b>salle</b> occupée au même moment par deux séances différentes ;</li>
 *   <li><b>professeur</b> attendu au même moment dans deux séances différentes ;</li>
 *   <li><b>classe</b> ayant deux créneaux qui se chevauchent.</li>
 * </ul>
 * Une même séance (même matière, même professeur, même salle, même horaire) donnée à plusieurs classes — cours
 * magistral commun — n'est PAS un conflit.
 */
public final class ConflitsPlanning {

    static final Set<String> SALLES_NEUTRES = Set.of("", "salle non assignée", "non assignée", "à définir");

    private ConflitsPlanning() {}

    /** @param nomsClasses identifiant → nom de classe (pour des messages lisibles) ; peut être vide */
    public static List<String> detecter(List<TimeSlot> creneaux, Map<String, String> nomsClasses) {
        List<String> conflits = new ArrayList<>();
        Set<String> dejaSignales = new HashSet<>();
        for (int i = 0; i < creneaux.size(); i++) {
            for (int j = i + 1; j < creneaux.size(); j++) {
                TimeSlot a = creneaux.get(i), b = creneaux.get(j);
                if (!cle(a.getDay()).equals(cle(b.getDay())) || !chevauchent(a, b)) continue;
                boolean memeSeance = cle(a.getNom()).equals(cle(b.getNom())) && cle(a.getProfesseur()).equals(cle(b.getProfesseur()))
                        && cle(a.getSalle()).equals(cle(b.getSalle())) && cle(a.getHeureDebut()).equals(cle(b.getHeureDebut()));
                String quand = a.getDay() + " " + plage(a) + " / " + plage(b);
                String qui = "(" + decrire(a, nomsClasses) + " ; " + decrire(b, nomsClasses) + ")";
                if (cle(a.getClasseId()).equals(cle(b.getClasseId()))) {
                    ajouter(conflits, dejaSignales, "Classe « " + nomClasse(a.getClasseId(), nomsClasses) + " » : deux cours le " + quand + " " + qui);
                } else if (!memeSeance) {
                    String salle = cle(a.getSalle());
                    if (!SALLES_NEUTRES.contains(salle) && salle.equals(cle(b.getSalle()))) {
                        ajouter(conflits, dejaSignales, "Salle « " + a.getSalle().trim() + " » occupée deux fois le " + quand + " " + qui);
                    }
                    String prof = cle(a.getProfesseur());
                    if (!prof.isEmpty() && prof.equals(cle(b.getProfesseur()))) {
                        ajouter(conflits, dejaSignales, "Professeur « " + a.getProfesseur().trim() + " » attendu dans deux cours le " + quand + " " + qui);
                    }
                }
            }
        }
        return conflits;
    }

    static boolean chevauchent(TimeSlot a, TimeSlot b) {
        LocalTime da = heure(a.getHeureDebut()), db = heure(b.getHeureDebut());
        LocalTime fa = heure(a.getHeureFin()), fb = heure(b.getHeureFin());
        if (da == null || db == null) return cle(a.getHeureDebut()).equals(cle(b.getHeureDebut()));
        if (fa == null || fb == null || !fa.isAfter(da) || !fb.isAfter(db)) return da.equals(db);
        return da.isBefore(fb) && db.isBefore(fa);
    }

    private static void ajouter(List<String> conflits, Set<String> vus, String message) {
        if (vus.add(message)) conflits.add(message);
    }

    private static String decrire(TimeSlot s, Map<String, String> noms) {
        String matiere = s.getNom() == null || s.getNom().isBlank() ? "cours sans intitulé" : s.getNom().trim();
        return matiere + " – " + nomClasse(s.getClasseId(), noms);
    }

    private static String nomClasse(String id, Map<String, String> noms) {
        return noms.getOrDefault(id, id);
    }

    private static String plage(TimeSlot s) {
        return s.getHeureFin() == null || s.getHeureFin().isBlank() ? s.getHeureDebut() : s.getHeureDebut() + "–" + s.getHeureFin();
    }

    private static LocalTime heure(String h) {
        if (h == null || h.isBlank()) return null;
        String t = h.trim().replace('h', ':').replace('H', ':');
        if (t.endsWith(":")) t = t + "00";
        if (t.length() == 4 && t.charAt(1) == ':') t = "0" + t;
        try {
            return LocalTime.parse(t);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String cle(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
