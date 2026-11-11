package ecole_221.sn.ecole.modules.professor.application.service;

import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CahierDeTextesJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CahierDeTextesJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.GradeJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.GradeJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaRepository;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaEntity;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaRepository;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.StudentHomeworkJpaEntity;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.StudentHomeworkJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Suivi pédagogique d'un cours (lot E, 2026-11-10) : cahier de textes, tableau de bord de la classe et fiche élève.
 *
 * <p>Tout est calculé à partir des données réellement saisies (notes, présences, séances, cahier de textes) ;
 * un indicateur sans source est renvoyé à {@code null} (affiché « — »). Le contrôle « cours du professeur / école »
 * est fait en amont par AccesCoursInterceptor pour les routes /professor/** ; les lectures sont filtrées par école
 * (@TenantId sur les entités).</p>
 */
@Service
public class SuiviPedagogiqueService {

    /** Seuils « élève en difficulté ». */
    public static final double SEUIL_MOYENNE = 10.0;
    public static final int SEUIL_ABSENCES = 3;

    private static final Set<String> STATUTS_SEANCE_FAITE = Set.of("termine", "terminee", "fait", "faite", "effectue", "effectuee", "done", "completed");
    private static final Set<String> STATUTS_PRESENT = Set.of("PRESENT", "RETARD", "LATE");
    private static final Set<String> STATUTS_ABSENT = Set.of("ABSENT");
    private static final Pattern CRENEAU = Pattern.compile("^\\s*(\\d{1,2})\\s*[:hH]\\s*(\\d{2})?\\s*[-–à]+\\s*(\\d{1,2})\\s*[:hH]\\s*(\\d{2})?\\s*$");

    private final CourseJpaRepository cours;
    private final GradeJpaRepository notes;
    private final AttendanceJpaRepository presences;
    private final InscriptionClasseJpaRepository inscriptions;
    private final ScheduleSessionJpaRepository seances;
    private final CahierDeTextesJpaRepository cahier;
    private final ComptesEcoleService comptesEcole;

    /** Rendus de devoirs par élève (facultatif : absent de la fiche si le dépôt n'est pas disponible). */
    @Autowired(required = false)
    private StudentHomeworkJpaRepository rendus;

    public SuiviPedagogiqueService(CourseJpaRepository cours, GradeJpaRepository notes, AttendanceJpaRepository presences,
                                   InscriptionClasseJpaRepository inscriptions, ScheduleSessionJpaRepository seances,
                                   CahierDeTextesJpaRepository cahier, ComptesEcoleService comptesEcole) {
        this.cours = cours;
        this.notes = notes;
        this.presences = presences;
        this.inscriptions = inscriptions;
        this.seances = seances;
        this.cahier = cahier;
        this.comptesEcole = comptesEcole;
    }

    void setRendus(StudentHomeworkJpaRepository rendus) { this.rendus = rendus; }

    // ==================== CAHIER DE TEXTES ====================

    public List<Map<String, Object>> cahierDeTextes(String courseId) {
        coursDeLEcole(courseId);
        return cahier.findByCourseIdOrderByDateDescCreatedAtDesc(courseId).stream().map(this::vueEntree).toList();
    }

    @Transactional
    public Map<String, Object> ajouterEntree(String courseId, String auteurId, Map<String, ?> corps) {
        coursDeLEcole(courseId);
        String sessionId = texte(corps, "sessionId");
        String date = texte(corps, "date");
        String contenu = texte(corps, "contenu");
        String travail = texte(corps, "travailAFaire");
        if (sessionId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La séance est obligatoire.");
        if (date == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de la séance est obligatoire.");
        if (contenu == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le contenu de la séance est obligatoire.");
        LocalDate jour;
        try {
            jour = LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date invalide (format attendu AAAA-MM-JJ) : " + date);
        }
        if (jour.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "On ne remplit pas le cahier de textes d'une séance future.");
        }
        ScheduleSessionJpaEntity s = seances.findById(sessionId)
                .filter(x -> courseId.equals(x.getCourseId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Séance introuvable pour ce cours."));
        if (cahier.existsBySessionIdAndDate(s.getId(), jour.toString())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le cahier de textes de cette séance est déjà rempli pour cette date.");
        }
        CahierDeTextesJpaEntity e = new CahierDeTextesJpaEntity();
        e.setId("cdt-" + UUID.randomUUID());
        e.setCourseId(courseId);
        e.setSessionId(s.getId());
        e.setDate(jour.toString());
        e.setContenu(contenu);
        e.setTravailAFaire(travail);
        e.setAuteurId(auteurId);
        e.setCreatedAt(LocalDateTime.now());
        return vueEntree(cahier.save(e));
    }

    /** Lecture par un élève : uniquement s'il est inscrit dans la classe du cours (sinon 404, comme un cours inexistant). */
    public List<Map<String, Object>> cahierDeTextesPourEleve(String courseId, String eleveId) {
        CourseJpaEntity c = coursDeLEcole(courseId);
        boolean inscrit = eleveId != null && c.getClasse() != null && inscriptions.findByUserId(eleveId).stream()
                .anyMatch(i -> c.getClasse().equals(i.getClasseId()));
        if (!inscrit) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cours introuvable dans votre classe.");
        return cahierDeTextes(courseId);
    }

    // ==================== TABLEAU DE BORD ====================

    public Map<String, Object> tableauDeBord(String courseId) {
        CourseJpaEntity c = coursDeLEcole(courseId);
        List<UserJpaEntity> eleves = elevesDuCours(c);
        Map<String, Double> moyennes = moyennesParEleve(courseId);
        Map<String, Integer> absences = new HashMap<>();
        for (AttendanceJpaEntity a : presences.findByCourseId(courseId)) {
            if (a.getStudentId() != null && STATUTS_ABSENT.contains(statut(a))) absences.merge(a.getStudentId(), 1, Integer::sum);
        }

        List<Double> moyennesClasse = new ArrayList<>();
        List<Map<String, Object>> difficulte = new ArrayList<>();
        for (UserJpaEntity u : eleves) {
            Double moy = moyennes.get(u.getId());
            int abs = absences.getOrDefault(u.getId(), 0);
            if (moy != null) moyennesClasse.add(moy);
            List<String> motifs = new ArrayList<>();
            if (moy != null && moy < SEUIL_MOYENNE) motifs.add("Moyenne inférieure à " + (int) SEUIL_MOYENNE);
            if (abs >= SEUIL_ABSENCES) motifs.add(abs + " absences");
            if (!motifs.isEmpty()) {
                Map<String, Object> m = vueEleve(u);
                m.put("moyenne", moy);
                m.put("absences", abs);
                m.put("motifs", motifs);
                difficulte.add(m);
            }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("courseId", c.getId());
        res.put("cours", c.getName());
        res.put("classe", c.getClasse());
        res.put("effectif", eleves.size());
        res.put("nombreNotes", moyennesClasse.size());
        res.put("moyenneClasse", moyennesClasse.isEmpty() ? null
                : arrondi(moyennesClasse.stream().mapToDouble(Double::doubleValue).average().orElse(0)));
        res.put("seuils", Map.of("moyenne", SEUIL_MOYENNE, "absences", SEUIL_ABSENCES));
        res.put("elevesEnDifficulte", difficulte);
        res.putAll(progression(c));
        return res;
    }

    /**
     * Heures des séances faites / volume horaire du cours. Une séance faite = une entrée du cahier de textes
     * (une occurrence datée) ou une séance marquée terminée sans entrée. null si le volume horaire n'est pas
     * renseigné ou si la durée d'une séance faite ne peut pas être lue (« HH:mm-HH:mm »).
     */
    Map<String, Object> progression(CourseJpaEntity c) {
        Map<String, Object> res = new LinkedHashMap<>();
        Map<String, ScheduleSessionJpaEntity> parId = new HashMap<>();
        for (ScheduleSessionJpaEntity s : seances.findByCourseId(c.getId())) parId.put(s.getId(), s);
        List<CahierDeTextesJpaEntity> entrees = cahier.findByCourseIdOrderByDateDescCreatedAtDesc(c.getId());
        Set<String> avecEntree = new HashSet<>();
        List<ScheduleSessionJpaEntity> faites = new ArrayList<>();
        for (CahierDeTextesJpaEntity e : entrees) {
            ScheduleSessionJpaEntity s = parId.get(e.getSessionId());
            if (s != null) { faites.add(s); avecEntree.add(s.getId()); }
        }
        for (ScheduleSessionJpaEntity s : parId.values()) {
            if (!avecEntree.contains(s.getId()) && s.getStatus() != null
                    && STATUTS_SEANCE_FAITE.contains(s.getStatus().trim().toLowerCase(Locale.ROOT))) {
                faites.add(s);
            }
        }
        Double heures = 0.0;
        for (ScheduleSessionJpaEntity s : faites) {
            Double d = dureeHeures(s.getTime());
            if (d == null) { heures = null; break; }
            heures += d;
        }
        Integer volume = c.getTotalHours() > 0 ? c.getTotalHours() : null;
        res.put("seancesFaites", faites.size());
        res.put("heuresFaites", heures == null ? null : arrondi(heures));
        res.put("volumeHoraire", volume);
        res.put("progressionProgramme", heures == null || volume == null ? null
                : Math.round(Math.min(100.0, heures * 100.0 / volume) * 10.0) / 10.0);
        return res;
    }

    /** Durée d'un créneau « 08:00-10:00 » / « 8h-10h30 », en heures ; null si illisible ou incohérent. */
    static Double dureeHeures(String creneau) {
        if (creneau == null) return null;
        Matcher m = CRENEAU.matcher(creneau);
        if (!m.matches()) return null;
        int debut = Integer.parseInt(m.group(1)) * 60 + (m.group(2) == null ? 0 : Integer.parseInt(m.group(2)));
        int fin = Integer.parseInt(m.group(3)) * 60 + (m.group(4) == null ? 0 : Integer.parseInt(m.group(4)));
        if (fin <= debut || fin > 24 * 60) return null;
        return (fin - debut) / 60.0;
    }

    // ==================== FICHE ÉLÈVE ====================

    public Map<String, Object> ficheEleve(String courseId, String eleveId) {
        CourseJpaEntity c = coursDeLEcole(courseId);
        UserJpaEntity eleve = elevesDuCours(c).stream().filter(u -> u.getId().equals(eleveId)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Élève introuvable dans la classe de ce cours."));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("eleve", vueEleve(eleve));
        res.put("courseId", c.getId());
        res.put("cours", c.getName());

        Map<String, Object> note = notes.findByStudentIdAndCourseId(eleveId, courseId).map(g -> {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("cc", g.getCc());
            n.put("examen", g.getExamen());
            n.put("moyenne", g.getCc() == null && g.getExamen() == null ? null : g.getAverage());
            return n;
        }).orElse(null);
        res.put("notes", note);

        List<AttendanceJpaEntity> liste = new ArrayList<>(presences.findByStudentIdAndCourseId(eleveId, courseId));
        liste.sort(Comparator.comparing((AttendanceJpaEntity a) -> String.valueOf(a.getDate())).reversed());
        int presents = 0, absents = 0;
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (AttendanceJpaEntity a : liste) {
            String st = statut(a);
            if (STATUTS_PRESENT.contains(st)) presents++;
            if (STATUTS_ABSENT.contains(st)) absents++;
            Map<String, Object> l = new LinkedHashMap<>();
            l.put("date", a.getDate());
            l.put("statut", a.getStatus());
            lignes.add(l);
        }
        Map<String, Object> pres = new LinkedHashMap<>();
        pres.put("liste", lignes);
        pres.put("total", liste.size());
        pres.put("presents", presents);
        pres.put("absences", absents);
        pres.put("tauxPresence", liste.isEmpty() ? null : Math.round(presents * 1000.0 / liste.size()) / 10.0);
        res.put("presences", pres);

        if (rendus != null) {
            List<Map<String, Object>> r = rendus.findByStudentId(eleveId).stream()
                    .filter(h -> courseId.equals(h.getCourseId()))
                    .map(SuiviPedagogiqueService::vueRendu).toList();
            res.put("rendus", r);
        }
        return res;
    }

    // ==================== OUTILS ====================

    /** Cours de l'école de la requête (filtre @TenantId + contrôle explicite), sinon 404. */
    CourseJpaEntity coursDeLEcole(String courseId) {
        String ecole = comptesEcole.ecoleCourante();
        return cours.findById(courseId)
                .filter(x -> x.getOrganizationId() == null || ecole.equals(x.getOrganizationId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cours introuvable dans cet établissement."));
    }

    /** Élèves actifs de l'école affectés à la classe du cours (même règle que la liste d'appel du professeur). */
    List<UserJpaEntity> elevesDuCours(CourseJpaEntity c) {
        Set<String> classe = c.getClasse() == null ? null
                : inscriptions.findByClasseId(c.getClasse()).stream().map(InscriptionClasseJpaEntity::getUserId)
                    .collect(java.util.stream.Collectors.toSet());
        return comptesEcole.membres("ETUDIANT").stream()
                .filter(e -> "ACTIVE".equalsIgnoreCase(e.getValue().getStatus()))
                .map(Map.Entry::getKey)
                .filter(u -> classe == null || classe.contains(u.getId()))
                .toList();
    }

    private Map<String, Double> moyennesParEleve(String courseId) {
        Map<String, Double> m = new HashMap<>();
        for (GradeJpaEntity g : notes.findByCourseId(courseId)) {
            if (g.getStudentId() != null && g.getAverage() != null && (g.getCc() != null || g.getExamen() != null)) {
                m.put(g.getStudentId(), g.getAverage());
            }
        }
        return m;
    }

    private static String statut(AttendanceJpaEntity a) {
        return a.getStatus() == null ? "" : a.getStatus().trim().toUpperCase(Locale.ROOT);
    }

    private static Map<String, Object> vueEleve(UserJpaEntity u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("prenom", u.getPrenom());
        m.put("nom", u.getNom());
        m.put("matricule", u.getMatricule());
        m.put("email", u.getEmail());
        return m;
    }

    private static Map<String, Object> vueRendu(StudentHomeworkJpaEntity h) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", h.getId());
        m.put("titre", h.getTitre());
        m.put("statut", h.getStatut());
        m.put("dateLimite", h.getDeadline() == null ? null : h.getDeadline().toString());
        m.put("rendu", h.getSubmittedAt() == null ? null : h.getSubmittedAt().toString());
        m.put("fichier", h.getFileName());
        return m;
    }

    private Map<String, Object> vueEntree(CahierDeTextesJpaEntity e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("courseId", e.getCourseId());
        m.put("sessionId", e.getSessionId());
        m.put("date", e.getDate());
        m.put("contenu", e.getContenu());
        m.put("travailAFaire", e.getTravailAFaire());
        m.put("auteurId", e.getAuteurId());
        m.put("createdAt", e.getCreatedAt() == null ? null : e.getCreatedAt().toString());
        ScheduleSessionJpaEntity s = e.getSessionId() == null ? null : seances.findById(e.getSessionId()).orElse(null);
        m.put("seance", s == null ? null : Objects.toString(s.getDay(), "") + " " + Objects.toString(s.getTime(), ""));
        return m;
    }

    private static double arrondi(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static String texte(Map<String, ?> corps, String cle) {
        if (corps == null) return null;
        Object v = corps.get(cle);
        return v == null || v.toString().isBlank() ? null : v.toString().trim();
    }
}
