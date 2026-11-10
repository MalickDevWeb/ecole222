package ecole_221.sn.ecole.modules.student.application.service;

import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.parent.application.service.JustificatifAbsenceService;
import ecole_221.sn.ecole.modules.professor.application.service.FeuilleAppelService;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaRepository;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaEntity;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaRepository;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * « Mes absences » — l'élève relit l'appel fait par ses professeurs (lot A) : taux d'assiduité global et
 * par cours, et le détail des séances où il n'était pas présent. Mêmes règles que la feuille d'appel du
 * professeur (statuts, taux, saisie la plus récente) : l'élève et le professeur voient les mêmes chiffres.
 * Seules les saisies rattachées à un cours de l'école sont prises en compte (pas les auto-pointages QR).
 */
@Service
public class AssiduiteEleveService {

    private final AttendanceJpaRepository presences;
    private final InscriptionClasseJpaRepository inscriptions;
    private final CourseJpaRepository cours;
    private final ScheduleSessionJpaRepository seances;
    private final UserJpaRepository comptes;
    private final JustificatifAbsenceService justificatifs;

    public AssiduiteEleveService(AttendanceJpaRepository presences, InscriptionClasseJpaRepository inscriptions,
                                 CourseJpaRepository cours, ScheduleSessionJpaRepository seances, UserJpaRepository comptes,
                                 JustificatifAbsenceService justificatifs) {
        this.presences = presences;
        this.inscriptions = inscriptions;
        this.cours = cours;
        this.seances = seances;
        this.comptes = comptes;
        this.justificatifs = justificatifs;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> assiduite(String eleveId) {
        String ecole = TenantContext.getCurrentOrganizationId().orElse(null);

        // Cours de sa (ses) classe(s), même sans aucun appel (taux « — »)
        Map<String, CourseJpaEntity> sesCours = new LinkedHashMap<>();
        for (InscriptionClasseJpaEntity i : inscriptions.findByUserId(eleveId)) {
            if (i.getClasseId() == null) continue;
            for (CourseJpaEntity c : cours.findByClasse(i.getClasseId())) {
                if (memeEcole(c, ecole)) sesCours.put(c.getId(), c);
            }
        }

        // Une seule saisie par (séance, date) : la plus récente — comme FeuilleAppelService.statistiques
        Map<String, AttendanceJpaEntity> uniques = new HashMap<>();
        for (AttendanceJpaEntity a : presences.findByStudentId(eleveId)) {
            if (a.getCourseId() == null || FeuilleAppelService.statutAffiche(a.getStatus()) == null) continue;
            if (!sesCours.containsKey(a.getCourseId())) {
                // appel d'un cours de son ancienne classe : gardé s'il s'agit bien d'un cours de l'école
                Optional<CourseJpaEntity> c = cours.findById(a.getCourseId());
                if (c.isEmpty() || !memeEcole(c.get(), ecole)) continue;
                sesCours.put(c.get().getId(), c.get());
            }
            String cle = a.getCourseId() + "|" + a.getSessionId() + "|" + jour(a.getDate());
            uniques.merge(cle, a, FeuilleAppelService::plusRecente);
        }

        Map<String, int[]> parCours = new HashMap<>();   // présent, retard, absent, excusé
        int[] global = new int[4];
        List<Map<String, Object>> manquements = new ArrayList<>();
        Map<String, Optional<ScheduleSessionJpaEntity>> cacheSeances = new HashMap<>();
        Map<String, Map<String, Object>> justifs = justificatifs.parPresence(eleveId);   // dépôt du parent, décision de l'école
        for (AttendanceJpaEntity a : uniques.values()) {
            String statut = FeuilleAppelService.statutAffiche(a.getStatus());
            int idx = switch (statut) { case "PRESENT" -> 0; case "RETARD" -> 1; case "ABSENT" -> 2; default -> 3; };
            parCours.computeIfAbsent(a.getCourseId(), k -> new int[4])[idx]++;
            global[idx]++;
            if (idx == 0) continue;
            CourseJpaEntity c = sesCours.get(a.getCourseId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("date", jour(a.getDate()));
            m.put("statut", statut);
            m.put("courseId", a.getCourseId());
            m.put("cours", c.getName());
            m.put("observation", vide(a.getNote()) ? null : a.getNote());
            Optional<ScheduleSessionJpaEntity> s = vide(a.getSessionId()) ? Optional.empty()
                    : cacheSeances.computeIfAbsent(a.getSessionId(), seances::findById);
            m.put("horaire", s.map(ScheduleSessionJpaEntity::getTime).orElse(null));
            m.put("salle", s.map(ScheduleSessionJpaEntity::getRoom).orElse(null));
            m.put("justificatif", justifs.get(a.getId()));
            manquements.add(m);
        }
        manquements.sort(Comparator.comparing((Map<String, Object> m) -> String.valueOf(m.get("date"))).reversed());

        Map<String, String> nomsProfs = new HashMap<>();
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (CourseJpaEntity c : sesCours.values()) {
            int[] n = parCours.getOrDefault(c.getId(), new int[4]);
            Map<String, Object> l = compteurs(n);
            l.put("courseId", c.getId());
            l.put("cours", c.getName());
            l.put("professeur", c.getProfId() == null ? null
                    : nomsProfs.computeIfAbsent(c.getProfId(), id -> comptes.findById(id).map(AssiduiteEleveService::nom).orElse(null)));
            lignes.add(l);
        }
        lignes.sort(Comparator.comparing(l -> String.valueOf(l.get("cours")), String.CASE_INSENSITIVE_ORDER));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("regle", FeuilleAppelService.REGLE_TAUX);
        res.put("global", compteurs(global));
        res.put("cours", lignes);
        res.put("seances", manquements);
        return res;
    }

    static Map<String, Object> compteurs(int[] n) {
        int total = n[0] + n[1] + n[2] + n[3];
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("present", n[0]);
        m.put("retard", n[1]);
        m.put("absent", n[2]);
        m.put("excuse", n[3]);
        m.put("seancesNotees", total);
        m.put("taux", FeuilleAppelService.taux(n[0] + n[1], total));
        return m;
    }

    /** Date AAAA-MM-JJ (les anciens envois stockaient parfois une date-heure). */
    static String jour(String brute) {
        if (brute == null) return null;
        String t = brute.trim();
        return t.length() >= 10 ? t.substring(0, 10) : t;
    }

    private static boolean memeEcole(CourseJpaEntity c, String ecole) {
        return ecole == null || c.getOrganizationId() == null || ecole.equals(c.getOrganizationId());
    }

    private static boolean vide(String s) {
        return s == null || s.isBlank();
    }

    private static String nom(UserJpaEntity u) {
        String n = ((u.getPrenom() == null ? "" : u.getPrenom()) + " " + (u.getNom() == null ? "" : u.getNom())).trim();
        return n.isEmpty() ? null : n;
    }
}
