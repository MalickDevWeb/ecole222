package ecole_221.sn.ecole.modules.student.application.service;

import ecole_221.sn.ecole.modules.professor.application.service.RendusDevoirsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Synthèse du suivi d'un élève pour la fiche 360° (APP-TL-24) : calculée par des règles explicites à partir des
 * données enregistrées — bulletin ({@link BulletinEleveService}), appel des professeurs ({@link AssiduiteEleveService})
 * et rendus de devoirs ({@link RendusDevoirsService}). Aucune IA, aucun score, aucune probabilité : chaque alerte cite
 * la règle qui la déclenche et la valeur mesurée. Une donnée absente reste {@code null} (affichée « — »).
 */
@Service
public class SyntheseSuiviEleveService {

    public static final double SEUIL_MOYENNE = 10.0;
    public static final double SEUIL_ASSIDUITE = 80.0;
    public static final int SEANCES_MINIMUM = 5;
    public static final int SEUIL_ABSENCES = 3;

    public static final List<String> REGLES = List.of(
            "R1 — moyenne générale du bulletin inférieure à 10/20.",
            "R2 — matière dont la moyenne est inférieure à 10/20.",
            "R3 — taux d'assiduité inférieur à 80 % (à partir de 5 séances notées), ou 3 absences non excusées ou plus.",
            "R4 — devoir non rendu alors que sa date limite est passée.",
            "Niveau : « à suivre » dès qu'une règle est déclenchée ; « données insuffisantes » sans aucune note ni séance notée.");

    private final BulletinEleveService bulletins;
    private final AssiduiteEleveService assiduites;
    private final RendusDevoirsService devoirs;

    public SyntheseSuiviEleveService(BulletinEleveService bulletins, AssiduiteEleveService assiduites,
                                     RendusDevoirsService devoirs) {
        this.bulletins = bulletins;
        this.assiduites = assiduites;
        this.devoirs = devoirs;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> synthese(String eleveId) {
        return synthese(eleveId, LocalDate.now());
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> synthese(String eleveId, LocalDate aujourdhui) {
        List<String> alertes = new ArrayList<>();

        // Notes : moyennes du bulletin officiel (mêmes règles que l'écran du professeur)
        Map<String, Object> bulletin = bulletins.bulletin(eleveId);
        Double moyenneGenerale = nombre(bulletin.get("moyenneGenerale"));
        int matieresNotees = bulletin.get("matieresNotees") instanceof Number n ? n.intValue() : 0;
        List<Map<String, Object>> sous10 = new ArrayList<>();
        for (Map<String, Object> m : (List<Map<String, Object>>) bulletin.getOrDefault("matieres", List.of())) {
            Double moyenne = nombre(m.get("moyenne"));
            if (moyenne != null && moyenne < SEUIL_MOYENNE) {
                Map<String, Object> l = new LinkedHashMap<>();
                l.put("cours", m.get("cours"));
                l.put("moyenne", moyenne);
                sous10.add(l);
            }
        }
        if (moyenneGenerale != null && moyenneGenerale < SEUIL_MOYENNE) {
            alertes.add("Moyenne générale de " + texte(moyenneGenerale) + "/20, inférieure à 10/20 (R1).");
        }
        for (Map<String, Object> l : sous10) {
            alertes.add("Moyenne de " + texte((Double) l.get("moyenne")) + "/20 en " + l.get("cours") + " (R2).");
        }

        // Assiduité : appel des professeurs
        Map<String, Object> global = (Map<String, Object>) assiduites.assiduite(eleveId).getOrDefault("global", Map.of());
        Map<String, Object> assiduite = new LinkedHashMap<>();
        for (String cle : List.of("taux", "present", "retard", "absent", "excuse", "seancesNotees")) {
            assiduite.put(cle, global.get(cle));
        }
        Double taux = nombre(global.get("taux"));
        int seancesNotees = global.get("seancesNotees") instanceof Number n ? n.intValue() : 0;
        int absences = global.get("absent") instanceof Number n ? n.intValue() : 0;
        if (taux != null && seancesNotees >= SEANCES_MINIMUM && taux < SEUIL_ASSIDUITE) {
            alertes.add("Taux d'assiduité de " + texte(taux) + " % sur " + seancesNotees + " séances notées, inférieur à 80 % (R3).");
        }
        if (absences >= SEUIL_ABSENCES) {
            alertes.add(absences + " absences non excusées (R3).");
        }

        // Devoirs : date limite passée et aucun fichier déposé
        int echus = 0;
        List<Map<String, Object>> nonRendus = new ArrayList<>();
        for (Map<String, Object> d : devoirs.devoirsEleve(eleveId)) {
            LocalDate limite = date(d.get("deadline"));
            if (limite == null || !limite.isBefore(aujourdhui)) continue;
            echus++;
            if ("NON_RENDU".equals(d.get("statut"))) {
                Map<String, Object> l = new LinkedHashMap<>();
                l.put("titre", d.get("titre"));
                l.put("cours", d.get("coursTitre"));
                l.put("echeance", limite.toString());
                nonRendus.add(l);
            }
        }
        if (!nonRendus.isEmpty()) {
            alertes.add(nonRendus.size() + (nonRendus.size() > 1 ? " devoirs non rendus" : " devoir non rendu")
                    + " après la date limite (R4).");
        }

        String niveau = !alertes.isEmpty() ? "a_suivre"
                : (matieresNotees == 0 && seancesNotees == 0) ? "donnees_insuffisantes" : "sans_alerte";

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("moyenneGenerale", moyenneGenerale);
        res.put("moyenneProvisoire", Boolean.TRUE.equals(bulletin.get("provisoire")));
        res.put("matieresNotees", matieresNotees);
        res.put("matieresSous10", sous10);
        res.put("assiduite", assiduite);
        res.put("devoirsEchus", echus);
        res.put("devoirsNonRendus", nonRendus);
        res.put("alertes", alertes);
        res.put("niveau", niveau);
        res.put("regles", REGLES);
        return res;
    }

    private static Double nombre(Object o) {
        return o instanceof Number n ? n.doubleValue() : null;
    }

    /** 8.5 → « 8,5 » ; 12.0 → « 12 » (écriture française, sans zéros inutiles). */
    static String texte(double v) {
        return new BigDecimal(String.valueOf(v)).stripTrailingZeros().toPlainString().replace('.', ',');
    }

    /** Date limite enregistrée en texte (« 2026-11-20 » ou « 2026-11-20T23:59 ») ; null si illisible. */
    private static LocalDate date(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        if (s.isEmpty()) return null;
        try {
            return LocalDate.parse(s.length() > 10 ? s.substring(0, 10) : s);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
