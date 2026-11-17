package ecole_221.sn.ecole.modules.student.application.service;

import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.professor.application.service.EvaluationService;
import ecole_221.sn.ecole.modules.professor.domain.model.CalculMoyenneEvaluations;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaRepository;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Bulletin d'un élève à partir des évaluations multiples (lot C) : pour chaque cours de sa classe, sa moyenne,
 * sa mention, son rang et le détail de ses notes — calculés par {@link EvaluationService#moyennes}, la même règle
 * que l'écran du professeur. Une moyenne est « provisoire » tant qu'une évaluation comptée n'est pas verrouillée
 * (validée) par l'administration. Rien n'est inventé : sans note, la moyenne vaut null (« — »).
 */
@Service
public class BulletinEleveService {

    /** Les cours n'ont pas de coefficient de matière : la moyenne générale est une moyenne simple. */
    public static final String REGLE_GENERALE = "Moyenne générale = moyenne simple des moyennes des matières notées "
            + "(aucun coefficient de matière n'est défini) ; une matière sans note n'est pas comptée.";

    private final InscriptionClasseJpaRepository inscriptions;
    private final CourseJpaRepository cours;
    private final EvaluationService evaluations;
    private final UserJpaRepository comptes;

    public BulletinEleveService(InscriptionClasseJpaRepository inscriptions, CourseJpaRepository cours,
                                EvaluationService evaluations, UserJpaRepository comptes) {
        this.inscriptions = inscriptions;
        this.cours = cours;
        this.evaluations = evaluations;
        this.comptes = comptes;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Map<String, Object> bulletin(String eleveId) {
        String ecole = TenantContext.getCurrentOrganizationId().orElse(null);
        Map<String, CourseJpaEntity> sesCours = new LinkedHashMap<>();
        for (InscriptionClasseJpaEntity i : inscriptions.findByUserId(eleveId)) {
            if (i.getClasseId() == null) continue;
            for (CourseJpaEntity c : cours.findByClasse(i.getClasseId())) {
                if (ecole == null || c.getOrganizationId() == null || ecole.equals(c.getOrganizationId())) sesCours.put(c.getId(), c);
            }
        }

        List<Map<String, Object>> matieres = new ArrayList<>();
        List<Double> moyennesMatieres = new ArrayList<>();
        boolean provisoire = false;
        Map<String, String> profs = new HashMap<>();
        for (CourseJpaEntity c : sesCours.values()) {
            Map<String, Object> m = evaluations.moyennes(c.getId());
            List<Map<String, Object>> evals = (List<Map<String, Object>>) m.get("evaluations");
            Map<String, Object> moi = ((List<Map<String, Object>>) m.get("eleves")).stream()
                    .filter(e -> eleveId.equals(e.get("studentId"))).findFirst().orElse(null);
            Map<String, Map<String, Object>> mesNotes = moi == null ? Map.of() : (Map<String, Map<String, Object>>) moi.get("notes");

            List<Map<String, Object>> lignes = new ArrayList<>();
            boolean matiereProvisoire = false;
            for (Map<String, Object> e : evals) {
                Map<String, Object> n = mesNotes.get(String.valueOf(e.get("id")));
                Map<String, Object> l = new LinkedHashMap<>();
                l.put("id", e.get("id"));
                l.put("titre", e.get("titre"));
                l.put("type", e.get("type"));
                l.put("date", e.get("date"));
                l.put("coefficient", e.get("coefficient"));
                l.put("noteSur", e.get("noteSur"));
                l.put("session", e.get("session"));
                l.put("verrouillee", e.get("verrouillee"));
                l.put("valeur", n == null ? null : n.get("valeur"));
                l.put("absence", n == null ? null : n.get("absence"));
                lignes.add(l);
                if (n != null && !Boolean.TRUE.equals(e.get("verrouillee"))) matiereProvisoire = true;
            }
            Double moyenne = moi == null ? null : (Double) moi.get("moyenne");
            if (moyenne != null) moyennesMatieres.add(moyenne);
            provisoire |= matiereProvisoire && moyenne != null;

            Map<String, Object> mat = new LinkedHashMap<>();
            mat.put("courseId", c.getId());
            mat.put("cours", c.getName());
            mat.put("professeur", c.getProfId() == null ? null : profs.computeIfAbsent(c.getProfId(), id -> comptes.findById(id)
                    .map(u -> (Objects.toString(u.getPrenom(), "") + " " + Objects.toString(u.getNom(), "")).trim())
                    .filter(s -> !s.isEmpty()).orElse(null)));
            mat.put("moyenne", moyenne);
            mat.put("mention", moi == null ? null : moi.get("mention"));
            mat.put("rang", moi == null ? null : moi.get("rang"));
            mat.put("effectifClasse", moi == null ? null : moi.get("nombreElevesClasses"));
            mat.put("moyenneClasse", m.get("moyenneClasse"));
            mat.put("provisoire", matiereProvisoire && moyenne != null);
            mat.put("evaluations", lignes);
            matieres.add(mat);
        }
        matieres.sort(Comparator.comparing(x -> String.valueOf(x.get("cours")), String.CASE_INSENSITIVE_ORDER));

        Double generale = moyennesMatieres.isEmpty() ? null
                : CalculMoyenneEvaluations.arrondi(moyennesMatieres.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("moyenneGenerale", generale);
        res.put("mention", CalculMoyenneEvaluations.mention(generale));
        res.put("matieresNotees", moyennesMatieres.size());
        res.put("provisoire", provisoire);
        res.put("regleMatiere", EvaluationService.REGLE);
        res.put("regleGenerale", REGLE_GENERALE);
        res.put("matieres", matieres);
        return res;
    }
}
