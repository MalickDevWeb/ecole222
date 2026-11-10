package ecole_221.sn.ecole.modules.professor.application.service;

import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.PresenceSeanceJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaRepository;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Feuille d'appel rattachée à une SÉANCE (ou, à défaut, au jour) d'un cours.
 *
 * <p>Règles :</p>
 * <ul>
 *   <li>la date est obligatoire (AAAA-MM-JJ) et ne peut pas être dans le futur ;</li>
 *   <li>la séance, si elle est indiquée, doit exister dans l'école et appartenir au cours ;</li>
 *   <li>chaque élève doit être affecté à la classe du cours, une seule fois par envoi ;</li>
 *   <li>statuts acceptés : PRESENT, ABSENT, RETARD, EXCUSE (stockés PRESENT / ABSENT / LATE / EXCUSED,
 *       valeurs du modèle de présence de l'espace élève) ;</li>
 *   <li>l'envoi est tout-ou-rien (transaction, validation complète avant écriture) ;</li>
 *   <li>une nouvelle saisie REMPLACE l'ancienne pour (cours, séance, date, élève) : pas de doublon.</li>
 * </ul>
 * <p>Le cours passé en paramètre a déjà été contrôlé (école + professeur) par AccesCoursInterceptor.</p>
 */
@Service
public class FeuilleAppelService {

    /** Statut affiché → statut stocké. */
    static final Map<String, String> STATUTS = Map.of(
            "PRESENT", "PRESENT",
            "ABSENT", "ABSENT",
            "RETARD", "LATE",
            "EXCUSE", "EXCUSED");
    static final int LONGUEUR_MAX_NOTE = 500;
    public static final String REGLE_TAUX = "taux = (présences + retards) / séances notées × 100 ; null si aucune séance notée";

    private final ScheduleSessionJpaRepository seances;
    private final InscriptionClasseJpaRepository inscriptions;
    private final PresenceSeanceJpaRepository presences;
    private final ComptesEcoleService comptesEcole;
    private final Clock horloge;

    @org.springframework.beans.factory.annotation.Autowired
    public FeuilleAppelService(ScheduleSessionJpaRepository seances, InscriptionClasseJpaRepository inscriptions,
                               PresenceSeanceJpaRepository presences, ComptesEcoleService comptesEcole) {
        this(seances, inscriptions, presences, comptesEcole, Clock.systemDefaultZone());
    }

    FeuilleAppelService(ScheduleSessionJpaRepository seances, InscriptionClasseJpaRepository inscriptions,
                        PresenceSeanceJpaRepository presences, ComptesEcoleService comptesEcole, Clock horloge) {
        this.seances = seances;
        this.inscriptions = inscriptions;
        this.presences = presences;
        this.comptesEcole = comptesEcole;
        this.horloge = horloge;
    }

    // ==================== Séances ====================

    /** Séances réelles du cours, avec leur date si elle est connue et les dates d'appel déjà enregistrées. */
    public List<Map<String, Object>> seancesDuCours(CourseJpaEntity cours) {
        Map<String, TreeSet<String>> appels = new HashMap<>();
        for (AttendanceJpaEntity a : presences.findByCourseId(cours.getId())) {
            if (a.getSessionId() != null && a.getDate() != null) {
                appels.computeIfAbsent(a.getSessionId(), k -> new TreeSet<>(Comparator.reverseOrder())).add(a.getDate());
            }
        }
        List<Map<String, Object>> res = new ArrayList<>();
        for (ScheduleSessionJpaEntity s : seances.findByCourseId(cours.getId())) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("courseId", s.getCourseId());
            m.put("day", s.getDay());
            m.put("time", s.getTime());
            m.put("room", s.getRoom());
            m.put("type", s.getType());
            m.put("status", s.getStatus());
            m.put("date", dateIso(s.getDateStr()));
            m.put("appelsEnregistres", new ArrayList<>(appels.getOrDefault(s.getId(), new TreeSet<>())));
            res.add(m);
        }
        return res;
    }

    // ==================== Feuille d'appel ====================

    /** Élèves de la classe du cours avec le statut enregistré pour (séance, date) — null = non noté. */
    public Map<String, Object> feuille(CourseJpaEntity cours, String sessionId, String dateBrute) {
        LocalDate date = dateValide(dateBrute, "");
        String seance = seanceDuCours(cours, sessionId);
        Map<String, AttendanceJpaEntity> saisies = saisiesDe(cours.getId(), seance, date.toString());

        List<Map<String, Object>> lignes = new ArrayList<>();
        LocalDateTime derniere = null;
        int notes = 0;
        for (UserJpaEntity u : elevesDeLaClasse(cours).values()) {
            AttendanceJpaEntity a = saisies.get(u.getId());
            Map<String, Object> l = new LinkedHashMap<>();
            l.put("studentId", u.getId());
            l.put("nom", nomComplet(u));
            l.put("matricule", u.getMatricule());
            l.put("status", a == null ? null : statutAffiche(a.getStatus()));
            l.put("note", a == null ? null : a.getNote());
            l.put("enregistreA", a == null || a.getRecordedAt() == null ? null : a.getRecordedAt().toString());
            if (a != null) {
                notes++;
                if (a.getRecordedAt() != null && (derniere == null || a.getRecordedAt().isAfter(derniere))) derniere = a.getRecordedAt();
            }
            lignes.add(l);
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("courseId", cours.getId());
        res.put("sessionId", seance);
        res.put("date", date.toString());
        res.put("enregistreA", derniere == null ? null : derniere.toString());
        res.put("totalEleves", lignes.size());
        res.put("elevesNotes", notes);
        res.put("eleves", lignes);
        return res;
    }

    /** Enregistre l'appel complet en un seul envoi ; tout est refusé si une ligne est invalide. */
    @Transactional
    public Map<String, Object> enregistrer(CourseJpaEntity cours, Map<String, Object> corps, String auteur) {
        if (corps == null) throw erreur("Le corps de la requête est vide.");
        LocalDate date = dateValide(texte(corps.get("date")), "");
        String seance = seanceDuCours(cours, texte(corps.get("sessionId")));
        if (!(corps.get("entries") instanceof List<?> lignes) || lignes.isEmpty()) {
            throw erreur("La feuille d'appel ne contient aucun élève (entries).");
        }
        Map<String, UserJpaEntity> classe = elevesDeLaClasse(cours);

        // 1. validation de TOUTE la feuille
        Map<String, AttendanceJpaEntity> nouvelles = new LinkedHashMap<>();
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        for (int i = 0; i < lignes.size(); i++) {
            String ligne = " (ligne " + (i + 1) + ")";
            if (!(lignes.get(i) instanceof Map<?, ?> e)) throw erreur("Ligne d'appel vide ou illisible" + ligne + ".");
            String eleve = texte(e.get("studentId"));
            if (eleve == null) throw erreur("Élève manquant" + ligne + ".");
            UserJpaEntity u = classe.get(eleve);
            if (u == null) throw erreur("Cet élève n'est pas affecté à la classe de ce cours" + ligne + ".");
            if (nouvelles.containsKey(eleve)) throw erreur("Le même élève apparaît deux fois dans l'appel" + ligne + ".");
            String statutBrut = texte(e.get("status"));
            if (statutBrut == null) throw erreur("Statut de présence manquant" + ligne + ".");
            String stocke = STATUTS.get(statutBrut.toUpperCase(Locale.ROOT));
            if (stocke == null) {
                throw erreur("Statut de présence inconnu" + ligne + " : « " + statutBrut + " » (attendu : PRESENT, ABSENT, RETARD ou EXCUSE).");
            }
            String note = texte(e.get("note"));
            if (note != null && note.length() > LONGUEUR_MAX_NOTE) {
                throw erreur("Observation trop longue" + ligne + " (" + LONGUEUR_MAX_NOTE + " caractères au plus).");
            }
            AttendanceJpaEntity a = new AttendanceJpaEntity();
            a.setId("att-" + UUID.randomUUID());
            a.setCourseId(cours.getId());
            a.setSessionId(seance);
            a.setDate(date.toString());
            a.setStudentId(eleve);
            a.setStudentName(nomComplet(u));
            a.setStatus(stocke);
            a.setNote(note);
            a.setRecordedAt(maintenant);
            a.setRecordedBy(auteur);
            nouvelles.put(eleve, a);
        }

        // 2. remplacement des saisies précédentes de ces élèves pour (cours, séance, date)
        List<AttendanceJpaEntity> anciennes = presences.findByCourseIdAndDate(cours.getId(), date.toString()).stream()
                .filter(a -> Objects.equals(a.getSessionId(), seance) && nouvelles.containsKey(a.getStudentId()))
                .toList();
        if (!anciennes.isEmpty()) {
            presences.deleteAll(anciennes);
            presences.flush();
        }
        presences.saveAll(nouvelles.values());

        Map<String, Object> res = feuille(cours, seance, date.toString());
        res.put("enregistreA", maintenant.toString());
        res.put("lignesEnregistrees", nouvelles.size());
        res.put("lignesRemplacees", anciennes.size());
        return res;
    }

    // ==================== Assiduité ====================

    /** Par élève de la classe : nombre de présences / retards / absences / excuses et taux d'assiduité. */
    public Map<String, Object> statistiques(CourseJpaEntity cours) {
        // une seule saisie par (séance, date, élève) : la plus récente (les anciens envois pouvaient être en double)
        Map<String, AttendanceJpaEntity> uniques = new HashMap<>();
        for (AttendanceJpaEntity a : presences.findByCourseId(cours.getId())) {
            if (a.getStudentId() == null || statutAffiche(a.getStatus()) == null) continue;
            String cle = a.getSessionId() + "|" + a.getDate() + "|" + a.getStudentId();
            uniques.merge(cle, a, (x, y) -> plusRecente(x, y));
        }
        Map<String, int[]> compteurs = new HashMap<>();   // présent, retard, absent, excusé
        for (AttendanceJpaEntity a : uniques.values()) {
            int[] c = compteurs.computeIfAbsent(a.getStudentId(), k -> new int[4]);
            switch (statutAffiche(a.getStatus())) {
                case "PRESENT" -> c[0]++;
                case "RETARD" -> c[1]++;
                case "ABSENT" -> c[2]++;
                default -> c[3]++;
            }
        }
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (UserJpaEntity u : elevesDeLaClasse(cours).values()) {
            int[] c = compteurs.getOrDefault(u.getId(), new int[4]);
            int total = c[0] + c[1] + c[2] + c[3];
            Map<String, Object> l = new LinkedHashMap<>();
            l.put("studentId", u.getId());
            l.put("nom", nomComplet(u));
            l.put("present", c[0]);
            l.put("retard", c[1]);
            l.put("absent", c[2]);
            l.put("excuse", c[3]);
            l.put("seancesNotees", total);
            l.put("taux", taux(c[0] + c[1], total));
            lignes.add(l);
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("courseId", cours.getId());
        res.put("regle", REGLE_TAUX);
        res.put("eleves", lignes);
        return res;
    }

    // ==================== Règles (visibles pour les tests) ====================

    /** Taux en pourcentage arrondi à 0,1 ; null si aucune séance notée (jamais 0 % ni 100 % inventé). */
    public static Double taux(int assidu, int total) {
        if (total <= 0) return null;
        return Math.round(assidu * 1000.0 / total) / 10.0;
    }

    /** Statut stocké (nouveau ou ancien chemin) → statut affiché ; null si non reconnu. */
    public static String statutAffiche(String stocke) {
        if (stocke == null) return null;
        return switch (stocke.trim().toUpperCase(Locale.ROOT)) {
            case "PRESENT", "COURS", "COURSE" -> "PRESENT";
            case "ABSENT" -> "ABSENT";
            case "LATE", "RETARD" -> "RETARD";
            case "EXCUSED", "EXCUSE" -> "EXCUSE";
            default -> null;
        };
    }

    LocalDate dateValide(String brute, String ligne) {
        if (brute == null || brute.isBlank()) throw erreur("La date de l'appel est obligatoire (AAAA-MM-JJ)" + ligne + ".");
        LocalDate d;
        try {
            d = LocalDate.parse(brute.trim());
        } catch (DateTimeParseException ex) {
            throw erreur("Date invalide (format attendu AAAA-MM-JJ)" + ligne + " : " + brute);
        }
        if (d.isAfter(LocalDate.now(horloge))) throw erreur("On ne peut pas faire l'appel pour une date future" + ligne + ".");
        return d;
    }

    /** null si aucune séance n'est indiquée ; 404 si inconnue dans l'école ; 400 si elle relève d'un autre cours. */
    String seanceDuCours(CourseJpaEntity cours, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return null;
        ScheduleSessionJpaEntity s = seances.findById(sessionId.trim())   // @TenantId : séances de l'école
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Séance introuvable dans cet établissement."));
        if (!cours.getId().equals(s.getCourseId())) throw erreur("Cette séance n'appartient pas à ce cours.");
        return s.getId();
    }

    // ==================== Outils ====================

    /** Élèves ACTIFS de l'école affectés à la classe du cours, triés par nom. */
    private Map<String, UserJpaEntity> elevesDeLaClasse(CourseJpaEntity cours) {
        if (cours.getClasse() == null) return new LinkedHashMap<>();
        Set<String> ids = new HashSet<>();
        for (InscriptionClasseJpaEntity i : inscriptions.findByClasseId(cours.getClasse())) ids.add(i.getUserId());
        Map<String, UserJpaEntity> res = new LinkedHashMap<>();
        comptesEcole.membres("ETUDIANT").stream()
                .filter(e -> e.getValue() != null && "ACTIVE".equalsIgnoreCase(e.getValue().getStatus()))
                .map(Map.Entry::getKey)
                .filter(u -> ids.contains(u.getId()))
                .sorted(Comparator.comparing(FeuilleAppelService::nomComplet, String.CASE_INSENSITIVE_ORDER))
                .forEach(u -> res.put(u.getId(), u));
        return res;
    }

    private Map<String, AttendanceJpaEntity> saisiesDe(String courseId, String sessionId, String date) {
        Map<String, AttendanceJpaEntity> res = new HashMap<>();
        for (AttendanceJpaEntity a : presences.findByCourseIdAndDate(courseId, date)) {
            if (a.getStudentId() == null || !Objects.equals(a.getSessionId(), sessionId)) continue;
            res.merge(a.getStudentId(), a, FeuilleAppelService::plusRecente);
        }
        return res;
    }

    public static AttendanceJpaEntity plusRecente(AttendanceJpaEntity x, AttendanceJpaEntity y) {
        if (x.getRecordedAt() == null) return y;
        if (y.getRecordedAt() == null) return x;
        return y.getRecordedAt().isAfter(x.getRecordedAt()) ? y : x;
    }

    static String nomComplet(UserJpaEntity u) {
        String n = ((u.getPrenom() == null ? "" : u.getPrenom()) + " " + (u.getNom() == null ? "" : u.getNom())).trim();
        return n.isEmpty() ? (u.getEmail() == null ? u.getId() : u.getEmail()) : n;
    }

    private static String dateIso(String brute) {
        if (brute == null) return null;
        try {
            return LocalDate.parse(brute.trim()).toString();
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static String texte(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static ResponseStatusException erreur(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
