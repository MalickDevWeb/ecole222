package ecole_221.sn.ecole.modules.professor.application.service;

import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.professor.domain.model.CalculMoyenneEvaluations;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.*;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Évaluations d'un cours (plusieurs par cours, coefficients, barème, session normale / rattrapage),
 * saisie des notes EN LOT, moyennes / mentions / rangs, journal des modifications et verrouillage.
 *
 * <p>Ce modèle s'ajoute à l'ancien couple CC / Examen (table « grades ») sans le modifier : les endpoints
 * /professor/courses/{courseId}/grades restent inchangés.</p>
 *
 * <p>Contrôles : le cours (école + professeur) est vérifié en amont par AccesCoursInterceptor pour /professor/** ;
 * ici on vérifie que l'évaluation appartient bien au cours de l'URL, que l'élève est affecté à la classe du cours
 * et que l'évaluation n'est pas verrouillée (409).</p>
 */
@Service
public class EvaluationService {

    public static final Set<String> TYPES = new LinkedHashSet<>(List.of("DEVOIR_SURVEILLE", "EXAMEN", "TP", "ORAL", "AUTRE"));
    public static final Set<String> SESSIONS = new LinkedHashSet<>(List.of("NORMALE", "RATTRAPAGE"));
    public static final Set<String> ABSENCES = Set.of(CalculMoyenneEvaluations.ABS, CalculMoyenneEvaluations.EXCUSE);
    public static final double COEFFICIENT_MAX = 10;
    public static final double NOTE_SUR_MAX = 100;

    /** Texte de la règle de calcul, renvoyé avec les moyennes pour l'affichage. */
    public static final String REGLE = "Moyenne sur 20 pondérée par les coefficients des évaluations de la session normale. "
            + "Une note de rattrapage remplace la note de l'évaluation rattrapée si elle est meilleure (ou si celle-ci manque). "
            + "ABS (absence non justifiée) = 0 ; EXCUSE (absence justifiée) = évaluation neutralisée ; "
            + "évaluation sans note = non comptée. Aucune note comptée = pas de moyenne.";

    private final EvaluationJpaRepository evaluations;
    private final NoteEvaluationJpaRepository notes;
    private final JournalNoteJpaRepository journal;
    private final CourseJpaRepository cours;
    private final InscriptionClasseJpaRepository inscriptions;
    private final ComptesEcoleService comptesEcole;

    public EvaluationService(EvaluationJpaRepository evaluations, NoteEvaluationJpaRepository notes, JournalNoteJpaRepository journal,
                             CourseJpaRepository cours, InscriptionClasseJpaRepository inscriptions, ComptesEcoleService comptesEcole) {
        this.evaluations = evaluations;
        this.notes = notes;
        this.journal = journal;
        this.cours = cours;
        this.inscriptions = inscriptions;
        this.comptesEcole = comptesEcole;
    }

    // ================================================================ évaluations

    @Transactional(readOnly = true)
    public List<Map<String, Object>> evaluations(String courseId) {
        Map<String, Long> nombres = notes.findByCourseId(courseId).stream()
                .collect(Collectors.groupingBy(NoteEvaluationJpaEntity::getEvaluationId, Collectors.counting()));
        return evaluations.findByCourseIdOrderByDateEvaluationAscCreeLeAsc(courseId).stream()
                .map(e -> vue(e, nombres.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    @Transactional
    public Map<String, Object> creer(String courseId, Map<String, Object> corps) {
        if (corps == null) throw erreur("Renseignez l'évaluation (titre, type, date, coefficient).");
        EvaluationJpaEntity e = new EvaluationJpaEntity();
        e.setId("eval-" + UUID.randomUUID());
        e.setCourseId(courseId);
        e.setNoteSur(20);
        e.setSession("NORMALE");
        appliquer(e, corps, true);
        e.setVerrouillee(false);
        Auteur a = auteur();
        e.setCreePar(a.id());
        e.setCreeLe(LocalDateTime.now());
        return vue(evaluations.save(e), 0);
    }

    @Transactional
    public Map<String, Object> modifier(String courseId, String evaluationId, Map<String, Object> corps) {
        EvaluationJpaEntity e = evaluation(courseId, evaluationId);
        refuserSiVerrouillee(e, "modifiée");
        if (corps == null || corps.isEmpty()) throw erreur("Aucune modification envoyée.");
        List<NoteEvaluationJpaEntity> existantes = notes.findByEvaluationId(evaluationId);
        String ancienneSession = e.getSession();
        appliquer(e, corps, false);
        if (!existantes.isEmpty()) {
            if (!ancienneSession.equals(e.getSession())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Des notes sont déjà saisies pour cette évaluation : sa session ne peut plus changer.");
            }
            double max = existantes.stream().map(NoteEvaluationJpaEntity::getValeur).filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue).max().orElse(0);
            if (max > e.getNoteSur()) {
                throw erreur("Une note saisie (" + format(max) + ") dépasse le nouveau barème (" + format(e.getNoteSur())
                        + ") : corrigez d'abord les notes concernées.");
            }
            if (e.getDateEvaluation().isAfter(LocalDate.now())) {
                throw erreur("Des notes sont déjà saisies : la date de l'évaluation ne peut pas être dans le futur.");
            }
        }
        if ("NORMALE".equals(ancienneSession) && "RATTRAPAGE".equals(e.getSession())
                && evaluations.existsByEvaluationRattrapeeId(e.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Une évaluation de rattrapage vise cette évaluation : elle doit rester en session normale.");
        }
        return vue(evaluations.save(e), existantes.size());
    }

    @Transactional
    public void supprimer(String courseId, String evaluationId) {
        EvaluationJpaEntity e = evaluation(courseId, evaluationId);
        refuserSiVerrouillee(e, "supprimée");
        long n = notes.countByEvaluationId(evaluationId);
        if (n > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Impossible de supprimer l'évaluation « " + e.getTitre()
                    + " » : " + n + (n > 1 ? " notes sont saisies" : " note est saisie") + ". Effacez d'abord les notes.");
        }
        if (evaluations.existsByEvaluationRattrapeeId(evaluationId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible de supprimer l'évaluation « " + e.getTitre() + " » : une évaluation de rattrapage la vise.");
        }
        evaluations.delete(e);
    }

    // ================================================================ notes

    /** L'évaluation et TOUS les élèves de la classe du cours, avec leur note (ou null). */
    @Transactional(readOnly = true)
    public Map<String, Object> notes(String courseId, String evaluationId) {
        EvaluationJpaEntity e = evaluation(courseId, evaluationId);
        Map<String, NoteEvaluationJpaEntity> parEleve = new HashMap<>();
        for (NoteEvaluationJpaEntity n : notes.findByEvaluationId(evaluationId)) parEleve.put(n.getStudentId(), n);
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (UserJpaEntity u : elevesDuCours(courseId).values()) {
            NoteEvaluationJpaEntity n = parEleve.get(u.getId());
            Map<String, Object> l = eleve(u);
            l.put("valeur", n == null ? null : n.getValeur());
            l.put("absence", n == null ? null : n.getAbsence());
            l.put("commentaire", n == null ? null : n.getCommentaire());
            l.put("modifieLe", n == null || n.getModifieLe() == null ? null : n.getModifieLe().toString());
            lignes.add(l);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("evaluation", vue(e, parEleve.size()));
        r.put("notes", lignes);
        return r;
    }

    /** Ligne de saisie validée. */
    private record Ligne(String studentId, Double valeur, String absence, String commentaire) {}

    /**
     * Enregistre un LOT de notes en une transaction : si une seule ligne est invalide, rien n'est enregistré
     * (400 avec le détail ligne par ligne). Une ligne sans valeur ni absence efface la note existante.
     */
    @Transactional
    public Map<String, Object> enregistrerNotes(String courseId, String evaluationId, Object corps) {
        EvaluationJpaEntity e = evaluation(courseId, evaluationId);
        refuserSiVerrouillee(e, null);
        if (e.getDateEvaluation() != null && e.getDateEvaluation().isAfter(LocalDate.now())) {
            throw erreur("L'évaluation est prévue le " + e.getDateEvaluation()
                    + " : les notes ne peuvent pas être saisies avant cette date.");
        }
        List<?> brutes = lignesDuCorps(corps);
        if (brutes.isEmpty()) throw erreur("Aucune note à enregistrer.");
        Map<String, UserJpaEntity> classe = elevesDuCours(courseId);

        List<String> erreurs = new ArrayList<>();
        List<Ligne> lignes = new ArrayList<>();
        Set<String> vus = new HashSet<>();
        for (int i = 0; i < brutes.size(); i++) {
            String prefixe = "Ligne " + (i + 1) + " : ";
            if (!(brutes.get(i) instanceof Map<?, ?> m)) {
                erreurs.add(prefixe + "format invalide.");
                continue;
            }
            Object sid = m.get("studentId");
            String studentId = sid == null ? "" : sid.toString().trim();
            if (studentId.isEmpty()) {
                erreurs.add(prefixe + "élève non indiqué.");
                continue;
            }
            UserJpaEntity u = classe.get(studentId);
            String qui = u == null ? studentId : nomEleve(u);
            if (u == null) {
                erreurs.add(prefixe + "l'élève « " + studentId + " » n'est pas inscrit dans la classe de ce cours.");
                continue;
            }
            if (!vus.add(studentId)) {
                erreurs.add(prefixe + qui + " apparaît plusieurs fois dans le lot.");
                continue;
            }
            String absence = null;
            Object abs = m.get("absence");
            if (abs != null && !abs.toString().isBlank() && !"false".equalsIgnoreCase(abs.toString())) {
                absence = abs.toString().trim().toUpperCase(Locale.ROOT);
                if (!ABSENCES.contains(absence)) {
                    erreurs.add(prefixe + qui + " : absence inconnue « " + abs + " » (attendu : ABS ou EXCUSE).");
                    continue;
                }
            }
            Double valeur;
            try {
                valeur = nombre(m.get("valeur"));
            } catch (IllegalArgumentException ex) {
                erreurs.add(prefixe + qui + " : la note « " + m.get("valeur") + " » n'est pas un nombre.");
                continue;
            }
            if (valeur != null && absence != null) {
                erreurs.add(prefixe + qui + " : indiquez une note OU une absence, pas les deux.");
                continue;
            }
            if (valeur != null) {
                if (valeur < 0 || valeur > e.getNoteSur()) {
                    erreurs.add(prefixe + qui + " : la note " + format(valeur) + " doit être comprise entre 0 et " + format(e.getNoteSur()) + ".");
                    continue;
                }
                if (Math.abs(valeur * 100 - Math.rint(valeur * 100)) > 1e-6) {
                    erreurs.add(prefixe + qui + " : deux décimales au maximum (" + m.get("valeur") + ").");
                    continue;
                }
            }
            Object com = m.get("commentaire");
            String commentaire = com == null || com.toString().isBlank() ? null : com.toString().trim();
            if (commentaire != null && commentaire.length() > 500) {
                erreurs.add(prefixe + qui + " : commentaire trop long (500 caractères au maximum).");
                continue;
            }
            if (valeur == null && absence == null && commentaire != null) {
                erreurs.add(prefixe + qui + " : un commentaire doit accompagner une note ou une absence.");
                continue;
            }
            lignes.add(new Ligne(studentId, valeur, absence, commentaire));
        }
        if (!erreurs.isEmpty()) {
            String detail = erreurs.stream().limit(10).collect(Collectors.joining(" ; "));
            if (erreurs.size() > 10) detail += " ; … (" + (erreurs.size() - 10) + " autres erreurs)";
            throw erreur("Aucune note enregistrée : " + erreurs.size() + (erreurs.size() > 1 ? " lignes invalides. " : " ligne invalide. ") + detail);
        }

        Map<String, NoteEvaluationJpaEntity> existantes = new HashMap<>();
        for (NoteEvaluationJpaEntity n : notes.findByEvaluationId(evaluationId)) existantes.put(n.getStudentId(), n);
        Auteur a = auteur();
        LocalDateTime maintenant = LocalDateTime.now();
        int creees = 0, modifiees = 0, supprimees = 0;
        for (Ligne l : lignes) {
            NoteEvaluationJpaEntity n = existantes.get(l.studentId());
            boolean vide = l.valeur() == null && l.absence() == null;
            if (n == null) {
                if (vide) continue;
                n = new NoteEvaluationJpaEntity();
                n.setId("note-" + UUID.randomUUID());
                n.setEvaluationId(evaluationId);
                n.setCourseId(courseId);
                n.setStudentId(l.studentId());
                n.setValeur(l.valeur());
                n.setAbsence(l.absence());
                n.setCommentaire(l.commentaire());
                n.setModifiePar(a.id());
                n.setModifieLe(maintenant);
                notes.save(n);
                tracer(e, l.studentId(), "CREATION", null, affichage(l.valeur(), l.absence()), null, l.commentaire(), a, maintenant);
                creees++;
                continue;
            }
            String avant = affichage(n.getValeur(), n.getAbsence());
            String ancienCommentaire = n.getCommentaire();
            if (vide) {
                notes.delete(n);
                tracer(e, l.studentId(), "SUPPRESSION", avant, null, ancienCommentaire, null, a, maintenant);
                supprimees++;
                continue;
            }
            String apres = affichage(l.valeur(), l.absence());
            if (Objects.equals(avant, apres) && Objects.equals(ancienCommentaire, l.commentaire())) continue;   // inchangée
            n.setValeur(l.valeur());
            n.setAbsence(l.absence());
            n.setCommentaire(l.commentaire());
            n.setModifiePar(a.id());
            n.setModifieLe(maintenant);
            notes.save(n);
            tracer(e, l.studentId(), "MODIFICATION", avant, apres, ancienCommentaire, l.commentaire(), a, maintenant);
            modifiees++;
        }
        Map<String, Object> r = notes(courseId, evaluationId);
        r.put("creees", creees);
        r.put("modifiees", modifiees);
        r.put("supprimees", supprimees);
        return r;
    }

    // ================================================================ moyennes

    /** Moyenne pondérée, mention et rang de chaque élève de la classe (règle : {@link CalculMoyenneEvaluations}). */
    @Transactional(readOnly = true)
    public Map<String, Object> moyennes(String courseId) {
        List<EvaluationJpaEntity> evals = evaluations.findByCourseIdOrderByDateEvaluationAscCreeLeAsc(courseId);
        List<CalculMoyenneEvaluations.Evaluation> calcul = evals.stream()
                .map(e -> new CalculMoyenneEvaluations.Evaluation(e.getId(), e.getCoefficient(), e.getNoteSur(),
                        "RATTRAPAGE".equals(e.getSession()), e.getEvaluationRattrapeeId()))
                .toList();
        Map<String, Map<String, NoteEvaluationJpaEntity>> parEleve = new HashMap<>();
        for (NoteEvaluationJpaEntity n : notes.findByCourseId(courseId)) {
            parEleve.computeIfAbsent(n.getStudentId(), k -> new HashMap<>()).put(n.getEvaluationId(), n);
        }
        Map<String, UserJpaEntity> classe = elevesDuCours(courseId);
        Map<String, CalculMoyenneEvaluations.Resultat> resultats = new LinkedHashMap<>();
        Map<String, Double> moyennes = new HashMap<>();
        for (String id : classe.keySet()) {
            Map<String, CalculMoyenneEvaluations.Note> sesNotes = new HashMap<>();
            parEleve.getOrDefault(id, Map.of()).forEach((evalId, n) ->
                    sesNotes.put(evalId, new CalculMoyenneEvaluations.Note(n.getValeur(), n.getAbsence())));
            CalculMoyenneEvaluations.Resultat r = CalculMoyenneEvaluations.moyenne(calcul, sesNotes);
            resultats.put(id, r);
            moyennes.put(id, r.moyenne());
        }
        Map<String, Integer> rangs = CalculMoyenneEvaluations.rangs(moyennes);
        long classes = moyennes.values().stream().filter(Objects::nonNull).count();

        List<Map<String, Object>> eleves = new ArrayList<>();
        for (UserJpaEntity u : classe.values()) {
            CalculMoyenneEvaluations.Resultat r = resultats.get(u.getId());
            Map<String, Object> l = eleve(u);
            l.put("moyenne", r.moyenne());
            l.put("mention", CalculMoyenneEvaluations.mention(r.moyenne()));
            l.put("rang", rangs.get(u.getId()));
            l.put("nombreElevesClasses", classes);
            l.put("evaluationsComptees", r.evaluationsComptees());
            Map<String, Object> detail = new LinkedHashMap<>();
            parEleve.getOrDefault(u.getId(), Map.of()).forEach((evalId, n) -> {
                Map<String, Object> d = new LinkedHashMap<>();
                d.put("valeur", n.getValeur());
                d.put("absence", n.getAbsence());
                detail.put(evalId, d);
            });
            l.put("notes", detail);
            eleves.add(l);
        }
        Map<String, Object> rep = new LinkedHashMap<>();
        rep.put("regle", REGLE);
        rep.put("evaluations", evals.stream().map(e -> vue(e, -1)).toList());
        rep.put("eleves", eleves);
        Double moyClasse = moyennes.values().stream().filter(Objects::nonNull).mapToDouble(Double::doubleValue).average().stream()
                .map(CalculMoyenneEvaluations::arrondi).boxed().findFirst().orElse(null);
        rep.put("moyenneClasse", moyClasse);
        return rep;
    }

    // ================================================================ journal

    @Transactional(readOnly = true)
    public List<Map<String, Object>> journal(String courseId, String evaluationId) {
        evaluation(courseId, evaluationId);
        Map<String, UserJpaEntity> classe = elevesDuCours(courseId);
        return journal.findByEvaluationIdOrderByHorodatageDesc(evaluationId).stream().map(j -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", j.getId());
            m.put("evaluationId", j.getEvaluationId());
            m.put("action", j.getAction());
            m.put("studentId", j.getStudentId());
            UserJpaEntity u = j.getStudentId() == null ? null : classe.get(j.getStudentId());
            m.put("eleve", u == null ? null : nomEleve(u));
            m.put("matricule", u == null ? null : u.getMatricule());
            m.put("ancienneValeur", j.getAncienneValeur());
            m.put("nouvelleValeur", j.getNouvelleValeur());
            m.put("ancienCommentaire", j.getAncienCommentaire());
            m.put("nouveauCommentaire", j.getNouveauCommentaire());
            m.put("auteurId", j.getAuteurId());
            m.put("auteur", j.getAuteurNom());
            m.put("horodatage", j.getHorodatage() == null ? null : j.getHorodatage().toString());
            return m;
        }).toList();
    }

    // ================================================================ verrouillage (administration)

    /**
     * Verrouille / déverrouille une évaluation (réservé à l'administration de l'école : /admin/** dans SecurityConfig).
     * Le cours doit exister dans l'école de la requête et l'évaluation lui appartenir (404 sinon).
     */
    @Transactional
    public Map<String, Object> verrouiller(String courseId, String evaluationId, boolean verrouiller) {
        String ecole = TenantContext.getCurrentOrganizationId().orElse(null);
        cours.findById(courseId)
                .filter(c -> ecole == null || c.getOrganizationId() == null || ecole.equals(c.getOrganizationId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cours introuvable dans cet établissement."));
        EvaluationJpaEntity e = evaluation(courseId, evaluationId);
        if (e.isVerrouillee() == verrouiller) {
            return vue(e, notes.countByEvaluationId(evaluationId));   // déjà dans l'état demandé : rien à tracer
        }
        Auteur a = auteur();
        LocalDateTime maintenant = LocalDateTime.now();
        e.setVerrouillee(verrouiller);
        e.setVerrouilleePar(verrouiller ? a.id() : null);
        e.setVerrouilleeLe(verrouiller ? maintenant : null);
        evaluations.save(e);
        tracer(e, null, verrouiller ? "VERROUILLAGE" : "DEVERROUILLAGE", null, null, null, null, a, maintenant);
        return vue(e, notes.countByEvaluationId(evaluationId));
    }

    // ================================================================ outils

    /** L'évaluation existe et appartient au cours de l'URL (sinon 404 : une évaluation d'un autre cours est invisible). */
    public EvaluationJpaEntity evaluation(String courseId, String evaluationId) {
        return evaluations.findById(evaluationId)
                .filter(e -> Objects.equals(e.getCourseId(), courseId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Évaluation introuvable dans ce cours."));
    }

    private static void refuserSiVerrouillee(EvaluationJpaEntity e, String action) {
        if (!e.isVerrouillee()) return;
        String quoi = action == null
                ? "ses notes ne peuvent plus être modifiées"
                : "elle ne peut plus être " + action;
        throw new ResponseStatusException(HttpStatus.CONFLICT, "L'évaluation « " + e.getTitre()
                + " » est verrouillée par l'administration : " + quoi + ". Demandez son déverrouillage.");
    }

    /** Élèves ACTIFS de l'école affectés à la classe du cours, triés par nom puis prénom. Cours sans classe → aucun élève. */
    Map<String, UserJpaEntity> elevesDuCours(String courseId) {
        CourseJpaEntity c = cours.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cours introuvable dans cet établissement."));
        if (c.getClasse() == null || c.getClasse().isBlank()) return new LinkedHashMap<>();
        Set<String> inscrits = inscriptions.findByClasseId(c.getClasse()).stream()
                .map(InscriptionClasseJpaEntity::getUserId).collect(Collectors.toSet());
        Map<String, UserJpaEntity> r = new LinkedHashMap<>();
        comptesEcole.membres("ETUDIANT").stream()
                .filter(m -> "ACTIVE".equalsIgnoreCase(m.getValue().getStatus()))
                .map(Map.Entry::getKey)
                .filter(u -> inscrits.contains(u.getId()))
                .sorted(Comparator.comparing((UserJpaEntity u) -> Objects.toString(u.getNom(), "").toLowerCase(Locale.ROOT))
                        .thenComparing(u -> Objects.toString(u.getPrenom(), "").toLowerCase(Locale.ROOT)))
                .forEach(u -> r.put(u.getId(), u));
        return r;
    }

    private void appliquer(EvaluationJpaEntity e, Map<String, Object> corps, boolean creation) {
        if (creation || corps.containsKey("titre")) {
            Object t = corps.get("titre");
            String titre = t == null ? "" : t.toString().trim();
            if (titre.isEmpty()) throw erreur("Le titre de l'évaluation est obligatoire.");
            if (titre.length() > 150) throw erreur("Le titre de l'évaluation est trop long (150 caractères au maximum).");
            e.setTitre(titre);
        }
        if (creation || corps.containsKey("type")) {
            Object t = corps.get("type");
            String type = t == null ? "" : t.toString().trim().toUpperCase(Locale.ROOT);
            if (type.isEmpty()) throw erreur("Le type de l'évaluation est obligatoire (" + String.join(", ", TYPES) + ").");
            if (!TYPES.contains(type)) throw erreur("Type d'évaluation inconnu : « " + t + " » (attendu : " + String.join(", ", TYPES) + ").");
            e.setType(type);
        }
        if (creation || corps.containsKey("date")) {
            Object d = corps.get("date");
            if (d == null || d.toString().isBlank()) throw erreur("La date de l'évaluation est obligatoire.");
            try {
                e.setDateEvaluation(LocalDate.parse(d.toString().trim()));
            } catch (DateTimeParseException ex) {
                throw erreur("Date d'évaluation invalide (format attendu AAAA-MM-JJ) : " + d);
            }
        }
        if (creation || corps.containsKey("coefficient")) {
            Double c;
            try {
                c = nombre(corps.get("coefficient"));
            } catch (IllegalArgumentException ex) {
                throw erreur("Le coefficient doit être un nombre : « " + corps.get("coefficient") + " ».");
            }
            if (c == null) throw erreur("Le coefficient de l'évaluation est obligatoire.");
            if (c <= 0 || c > COEFFICIENT_MAX) throw erreur("Le coefficient doit être strictement positif et au plus " + format(COEFFICIENT_MAX) + " (reçu : " + format(c) + ").");
            e.setCoefficient(c);
        }
        if (corps.containsKey("noteSur")) {
            Double s;
            try {
                s = nombre(corps.get("noteSur"));
            } catch (IllegalArgumentException ex) {
                throw erreur("Le barème (note sur) doit être un nombre : « " + corps.get("noteSur") + " ».");
            }
            if (s == null) s = 20.0;
            if (s <= 0 || s > NOTE_SUR_MAX) throw erreur("Le barème (note sur) doit être compris entre 1 et " + format(NOTE_SUR_MAX) + " (reçu : " + format(s) + ").");
            e.setNoteSur(s);
        }
        if (corps.containsKey("session")) {
            Object s = corps.get("session");
            String session = s == null || s.toString().isBlank() ? "NORMALE" : s.toString().trim().toUpperCase(Locale.ROOT);
            if (!SESSIONS.contains(session)) throw erreur("Session inconnue : « " + s + " » (attendu : NORMALE ou RATTRAPAGE).");
            e.setSession(session);
        }
        if ("RATTRAPAGE".equals(e.getSession())) {
            Object r = corps.containsKey("evaluationRattrapeeId") ? corps.get("evaluationRattrapeeId") : e.getEvaluationRattrapeeId();
            String cible = r == null ? "" : r.toString().trim();
            if (cible.isEmpty()) throw erreur("Une évaluation de rattrapage doit indiquer l'évaluation qu'elle rattrape.");
            if (cible.equals(e.getId())) throw erreur("Une évaluation ne peut pas se rattraper elle-même.");
            EvaluationJpaEntity origine = evaluations.findById(cible)
                    .filter(o -> Objects.equals(o.getCourseId(), e.getCourseId()))
                    .orElseThrow(() -> erreur("L'évaluation rattrapée est introuvable dans ce cours."));
            if (!"NORMALE".equals(origine.getSession())) throw erreur("On ne peut rattraper qu'une évaluation de la session normale.");
            if (origine.getDateEvaluation() != null && e.getDateEvaluation() != null && e.getDateEvaluation().isBefore(origine.getDateEvaluation())) {
                throw erreur("La date du rattrapage ne peut pas précéder celle de l'évaluation rattrapée (" + origine.getDateEvaluation() + ").");
            }
            e.setEvaluationRattrapeeId(cible);
        } else {
            e.setEvaluationRattrapeeId(null);
        }
    }

    private void tracer(EvaluationJpaEntity e, String studentId, String action, String avant, String apres,
                        String ancienCommentaire, String nouveauCommentaire, Auteur a, LocalDateTime quand) {
        JournalNoteJpaEntity j = new JournalNoteJpaEntity();
        j.setId("jnote-" + UUID.randomUUID());
        j.setEvaluationId(e.getId());
        j.setCourseId(e.getCourseId());
        j.setStudentId(studentId);
        j.setAction(action);
        j.setAncienneValeur(avant);
        j.setNouvelleValeur(apres);
        j.setAncienCommentaire(ancienCommentaire);
        j.setNouveauCommentaire(nouveauCommentaire);
        j.setAuteurId(a.id());
        j.setAuteurNom(a.nom());
        j.setHorodatage(quand);
        journal.save(j);
    }

    private record Auteur(String id, String nom) {}

    private static Auteur auteur() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User u) {
            String nom = ((u.getPrenom() == null ? "" : u.getPrenom()) + " " + (u.getNom() == null ? "" : u.getNom())).trim();
            return new Auteur(u.getId(), nom.isEmpty() ? u.getEmail() : nom);
        }
        return new Auteur(null, auth != null ? auth.getName() : null);
    }

    private static Map<String, Object> eleve(UserJpaEntity u) {
        Map<String, Object> l = new LinkedHashMap<>();
        l.put("studentId", u.getId());
        l.put("matricule", u.getMatricule());
        l.put("nom", u.getNom());
        l.put("prenom", u.getPrenom());
        return l;
    }

    private static String nomEleve(UserJpaEntity u) {
        return ComptesEcoleService.nomComplet(u);
    }

    static Map<String, Object> vue(EvaluationJpaEntity e, long nombreNotes) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("courseId", e.getCourseId());
        m.put("titre", e.getTitre());
        m.put("type", e.getType());
        m.put("date", e.getDateEvaluation() == null ? null : e.getDateEvaluation().toString());
        m.put("coefficient", e.getCoefficient());
        m.put("noteSur", e.getNoteSur());
        m.put("session", e.getSession());
        m.put("evaluationRattrapeeId", e.getEvaluationRattrapeeId());
        m.put("verrouillee", e.isVerrouillee());
        m.put("verrouilleeLe", e.getVerrouilleeLe() == null ? null : e.getVerrouilleeLe().toString());
        if (nombreNotes >= 0) m.put("nombreNotes", nombreNotes);
        return m;
    }

    @SuppressWarnings("unchecked")
    private static List<?> lignesDuCorps(Object corps) {
        if (corps instanceof List<?> l) return l;
        if (corps instanceof Map<?, ?> m && m.get("notes") instanceof List<?> l) return l;
        if (corps == null) return List.of();
        throw erreur("Format attendu : une liste [{studentId, valeur, absence, commentaire}].");
    }

    /** Nombre JSON, ou texte numérique (virgule acceptée) ; null / vide → null ; sinon IllegalArgumentException. */
    static Double nombre(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) {
            double d = n.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) throw new IllegalArgumentException();
            return d;
        }
        String s = v.toString().trim().replace(',', '.');
        if (s.isEmpty()) return null;
        try {
            double d = Double.parseDouble(s);
            if (Double.isNaN(d) || Double.isInfinite(d)) throw new IllegalArgumentException();
            return d;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /** Représentation d'une note dans le journal : « 12.5 », « ABS », « EXCUSE » ou null. */
    static String affichage(Double valeur, String absence) {
        if (absence != null) return absence;
        return valeur == null ? null : format(valeur);
    }

    static String format(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(CalculMoyenneEvaluations.arrondi(v));
    }

    private static ResponseStatusException erreur(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
