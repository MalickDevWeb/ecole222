package ecole_221.sn.ecole.modules.student.application.service;

import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Emploi du temps RÉEL d'un élève : séances des classes où il est inscrit, telles que planifiées par
 * l'administration (Gestion École › Planning) et éventuellement annulées ou reportées par le professeur.
 *
 * <p>Lecture cloisonnée par école (@TenantId sur les séances et les inscriptions). Aucune séance inventée : sans
 * classe ou sans séance, la liste est vide.</p>
 */
@Service
public class EmploiDuTempsEleveService {

    /** Une séance de l'emploi du temps ; {@code jourCode} : LUN, MAR, MER, JEU, VEN, SAM, DIM. */
    public record Seance(String id, String cours, String jourCode, String jour, String heureDebut, String heureFin,
                         String horaire, String type, String salle, String professeur, String statut, String motifAnnulation,
                         String classe) {
    }

    private static final List<String> CODES = List.of("LUN", "MAR", "MER", "JEU", "VEN", "SAM", "DIM");
    private static final List<String> NOMS = List.of("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche");
    private static final Map<String, Integer> JOURS = new HashMap<>();
    static {
        String[][] alias = {{"lundi", "lun", "monday", "mon"}, {"mardi", "mar", "tuesday", "tue"},
                {"mercredi", "mer", "wednesday", "wed"}, {"jeudi", "jeu", "thursday", "thu"},
                {"vendredi", "ven", "friday", "fri"}, {"samedi", "sam", "saturday", "sat"}, {"dimanche", "dim", "sunday", "sun"}};
        for (int i = 0; i < alias.length; i++) for (String a : alias[i]) JOURS.put(a, i);
    }
    private static final Pattern HORAIRE = Pattern.compile("^\\s*(\\d{1,2})\\s*[:hH]\\s*(\\d{2})?\\s*[-–à]+\\s*(\\d{1,2})\\s*[:hH]\\s*(\\d{2})?\\s*$");

    private final InscriptionClasseJpaRepository inscriptions;
    private final ScheduleSessionJpaRepository seances;
    private final UserJpaRepository comptes;
    private final ClasseJpaRepository classes;

    public EmploiDuTempsEleveService(InscriptionClasseJpaRepository inscriptions, ScheduleSessionJpaRepository seances,
                                     UserJpaRepository comptes, ClasseJpaRepository classes) {
        this.inscriptions = inscriptions;
        this.seances = seances;
        this.comptes = comptes;
        this.classes = classes;
    }

    /** Indice du jour (0 = lundi) ; null si le jour n'est pas reconnu. */
    static Integer jour(String brut) {
        if (brut == null) return null;
        String k = Normalizer.normalize(brut.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return JOURS.get(k);
    }

    /** « 8h-10h », « 08:00 - 10:00 »… → {"08:00", "10:00"} ; null si illisible. */
    static String[] horaire(String brut) {
        if (brut == null) return null;
        Matcher m = HORAIRE.matcher(brut);
        if (!m.matches()) return null;
        return new String[]{String.format("%02d:%s", Integer.parseInt(m.group(1)), m.group(2) == null ? "00" : m.group(2)),
                String.format("%02d:%s", Integer.parseInt(m.group(3)), m.group(4) == null ? "00" : m.group(4))};
    }

    static String statut(String brut) {
        if (brut == null) return "a_venir";
        String s = brut.toLowerCase(Locale.ROOT);
        if (s.startsWith("annul")) return "annule";
        if (s.startsWith("report")) return "reporte";
        if (s.startsWith("termin")) return "termine";
        return "a_venir";
    }

    @Transactional(readOnly = true)
    public List<Seance> seances(String eleveId) {
        if (eleveId == null || eleveId.isBlank()) return List.of();
        Set<String> classesIds = inscriptions.findByUserId(eleveId).stream()
                .map(InscriptionClasseJpaEntity::getClasseId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (classesIds.isEmpty()) return List.of();
        List<ScheduleSessionJpaEntity> liste = seances.findByClasseIn(classesIds).stream()
                .filter(s -> !"retire".equals(s.getStatus()))
                .filter(s -> jour(s.getDay()) != null)          // un jour illisible ne peut pas être placé
                .toList();
        Map<String, String> profs = new HashMap<>();
        comptes.findAllById(liste.stream().map(ScheduleSessionJpaEntity::getProfessorId).filter(Objects::nonNull).distinct().toList())
                .forEach(u -> profs.put(u.getId(), ComptesEcoleService.nomComplet(u)));
        Map<String, String> nomsClasses = new HashMap<>();   // nom affiché de la classe
        classes.findAllById(classesIds).forEach(c -> nomsClasses.put(c.getId(), c.getName() != null ? c.getName() : c.getNom()));
        return liste.stream().map(s -> {
                    int j = jour(s.getDay());
                    String[] h = horaire(s.getTime());
                    String type = s.getType() != null && List.of("CM", "TD", "TP").contains(s.getType().toUpperCase(Locale.ROOT))
                            ? s.getType().toUpperCase(Locale.ROOT) : "CM";
                    return new Seance(s.getId(), s.getCourseTitle() == null ? "" : s.getCourseTitle(), CODES.get(j), NOMS.get(j),
                            h == null ? "" : h[0], h == null ? "" : h[1], s.getTime() == null ? "" : s.getTime(), type,
                            s.getRoom() == null ? "" : s.getRoom(), profs.getOrDefault(s.getProfessorId(), ""),
                            statut(s.getStatus()), s.getCancellationReason(), nomsClasses.getOrDefault(s.getClasse(), ""));
                })
                .sorted(Comparator.comparing((Seance s) -> CODES.indexOf(s.jourCode())).thenComparing(Seance::heureDebut))
                .toList();
    }
}
