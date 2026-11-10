package ecole_221.sn.ecole.modules.professor.application.service;

import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.media.application.port.usecase.UploadMediaUseCase;
import ecole_221.sn.ecole.modules.media.domain.exception.InvalidFileException;
import ecole_221.sn.ecole.modules.media.domain.model.UploadedFile;
import ecole_221.sn.ecole.modules.media.domain.valueobject.FormatsDocumentsAutorises;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.HomeworkJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.HomeworkJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.RenduDevoirJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.RenduDevoirJpaRepository;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Lot B — devoirs de classe : rendus des élèves, correction (note /20 + commentaire), notifications.
 *
 * <ul>
 *   <li>les élèves attendus sont ceux AFFECTÉS à la classe du cours (inscriptions_classes), membres actifs de l'école ;</li>
 *   <li>un rendu est « en retard » s'il est déposé après le jour de la date limite ;</li>
 *   <li>le fichier déposé est réellement stocké (module media) : vérification du type ET de la signature du contenu ;</li>
 *   <li>le contrôle « cours du professeur » est fait en amont par AccesCoursInterceptor ({courseId} dans l'URL) ;
 *       ici on vérifie que le devoir appartient bien à CE cours et que l'élève est bien dans SA classe.</li>
 * </ul>
 */
@Service
public class RendusDevoirsService {

    public static final int COMMENTAIRE_MAX = 2000;

    private final HomeworkJpaRepository devoirs;
    private final CourseJpaRepository cours;
    private final RenduDevoirJpaRepository rendus;
    private final InscriptionClasseJpaRepository inscriptions;
    private final ComptesEcoleService comptesEcole;
    private final UploadMediaUseCase stockage;
    private final NotificationsDevoirsService notifications;

    public RendusDevoirsService(HomeworkJpaRepository devoirs, CourseJpaRepository cours, RenduDevoirJpaRepository rendus,
                                InscriptionClasseJpaRepository inscriptions, ComptesEcoleService comptesEcole,
                                UploadMediaUseCase stockage, NotificationsDevoirsService notifications) {
        this.devoirs = devoirs;
        this.cours = cours;
        this.rendus = rendus;
        this.inscriptions = inscriptions;
        this.comptesEcole = comptesEcole;
        this.stockage = stockage;
        this.notifications = notifications;
    }

    // ------------------------------------------------------------------ côté professeur

    /** Pour chaque élève de la classe : rendu (date, fichier, retard) ou « non rendu », correction, statistiques. */
    @Transactional(readOnly = true)
    public Map<String, Object> rendus(String courseId, String homeworkId) {
        CourseJpaEntity c = coursDeLEcole(courseId);
        HomeworkJpaEntity d = devoirDuCours(courseId, homeworkId);
        LocalDate limite = dateLimite(d);
        Map<String, RenduDevoirJpaEntity> parEleve = rendus.findByHomeworkId(homeworkId).stream()
                .collect(Collectors.toMap(RenduDevoirJpaEntity::getStudentId, r -> r, (a, b) -> a));

        List<Map<String, Object>> lignes = new ArrayList<>();
        int rendusCount = 0, retards = 0, corriges = 0;
        for (UserJpaEntity eleve : elevesDeLaClasse(c)) {
            RenduDevoirJpaEntity r = parEleve.get(eleve.getId());
            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put("studentId", eleve.getId());
            ligne.put("nom", eleve.getNom());
            ligne.put("prenom", eleve.getPrenom());
            ligne.put("matricule", eleve.getMatricule());
            ligne.put("email", eleve.getEmail());
            boolean depose = r != null && r.estDepose();
            boolean retard = depose && enRetard(r.getDeposeLe(), limite);
            ligne.put("statut", depose ? (retard ? "EN_RETARD" : "RENDU") : "NON_RENDU");
            ligne.put("rendu", depose ? vueRendu(r, retard) : null);
            ligne.put("correction", r != null && r.estCorrige() ? vueCorrection(r) : null);
            if (depose) rendusCount++;
            if (retard) retards++;
            if (r != null && r.estCorrige()) corriges++;
            lignes.add(ligne);
        }
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("attendus", lignes.size());
        stats.put("rendus", rendusCount);
        stats.put("enRetard", retards);
        stats.put("nonRendus", lignes.size() - rendusCount);
        stats.put("corriges", corriges);

        Map<String, Object> rep = new LinkedHashMap<>();
        rep.put("devoir", vueDevoir(d, c));
        rep.put("statistiques", stats);
        rep.put("eleves", lignes);
        return rep;
    }

    /** Note 0–20 et commentaire (≤ 2000) pour un élève de la classe ; l'élève est notifié (échec signalé, jamais bloquant). */
    // Pas de @Transactional ici : un échec de notification (transaction du module notifications) ne doit pas
    // rendre la correction « rollback-only » ; l'enregistrement (save) a sa propre transaction.
    public Map<String, Object> corriger(String courseId, String homeworkId, String studentId, Map<String, Object> corps, String correcteur) {
        CourseJpaEntity c = coursDeLEcole(courseId);
        HomeworkJpaEntity d = devoirDuCours(courseId, homeworkId);
        double note = noteValide(corps == null ? null : corps.get("note"));
        String commentaire = commentaireValide(corps == null ? null : corps.get("commentaire"));
        if (!eleveDansLaClasse(studentId, c)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cet élève n'est pas inscrit dans la classe de ce cours.");
        }
        RenduDevoirJpaEntity r = rendus.findByHomeworkIdAndStudentId(homeworkId, studentId)
                .orElseGet(() -> new RenduDevoirJpaEntity("rendu-" + UUID.randomUUID(), homeworkId, courseId, studentId));
        r.setNote(note);
        r.setCommentaire(commentaire);
        r.setCorrigeLe(LocalDateTime.now());
        r.setCorrigePar(correcteur);
        RenduDevoirJpaEntity enregistre = rendus.save(r);

        Map<String, Object> rep = new LinkedHashMap<>();
        rep.put("studentId", studentId);
        rep.put("homeworkId", homeworkId);
        rep.putAll(vueCorrection(enregistre));
        rep.put("notification", notifications.correctionPubliee(studentId, d.getTitle(), note));
        return rep;
    }

    /** Notification « nouveau devoir » aux élèves de la classe du cours (appelée après l'enregistrement du devoir). */
    public Map<String, Object> notifierNouveauDevoir(String courseId, String titre, String dateLimite) {
        try {
            CourseJpaEntity c = coursDeLEcole(courseId);
            List<String> eleves = elevesDeLaClasse(c).stream().map(UserJpaEntity::getId).toList();
            return notifications.nouveauDevoir(eleves, c.getName(), titre, dateLimite);
        } catch (RuntimeException e) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("envoyees", 0);
            r.put("message", "Le devoir est créé, mais les élèves n'ont pas pu être notifiés.");
            return r;
        }
    }

    // ------------------------------------------------------------------ côté élève

    public boolean estDevoirDeClasse(String homeworkId) {
        return homeworkId != null && devoirs.findById(homeworkId).isPresent();
    }

    /** Dépôt d'un fichier par l'élève : stocké réellement, daté, marqué en retard si la date limite est dépassée. */
    @Transactional
    public Map<String, Object> deposer(String studentId, String courseId, String homeworkId,
                                       String nomFichier, String typeFichier, byte[] contenu) {
        HomeworkJpaEntity d = devoirDuCours(courseId, homeworkId);
        CourseJpaEntity c = coursDeLEcole(courseId);
        if (studentId == null || !eleveDansLaClasse(studentId, c)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce devoir ne concerne pas votre classe.");
        }
        RenduDevoirJpaEntity r = rendus.findByHomeworkIdAndStudentId(homeworkId, studentId)
                .orElseGet(() -> new RenduDevoirJpaEntity("rendu-" + UUID.randomUUID(), homeworkId, courseId, studentId));
        if (r.estCorrige()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce devoir a déjà été corrigé : un nouveau dépôt n'est plus possible.");
        }
        verifierFichier(nomFichier, typeFichier, contenu);
        UploadedFile f;
        try {
            f = stockage.upload(contenu, nomFichier, typeFichier.toLowerCase(Locale.ROOT));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Le fichier n'a pas pu être enregistré. Réessayez plus tard.");
        }
        r.setFichierNom(nomFichier);
        r.setFichierUrl(f.getUrl());
        r.setFichierType(typeFichier.toLowerCase(Locale.ROOT));
        r.setFichierTaille((long) contenu.length);
        r.setDeposeLe(LocalDateTime.now());
        RenduDevoirJpaEntity enregistre = rendus.save(r);
        boolean retard = enRetard(enregistre.getDeposeLe(), dateLimite(d));

        Map<String, Object> rep = new LinkedHashMap<>();
        rep.put("success", true);
        rep.put("homeworkId", homeworkId);
        rep.put("fileName", nomFichier);
        rep.put("fileUrl", f.getUrl());
        rep.put("size", contenu.length);
        rep.put("status", "Soumis");
        rep.put("deposeLe", enregistre.getDeposeLe());
        rep.put("enRetard", retard);
        rep.put("message", retard ? "Fichier déposé (après la date limite : rendu marqué en retard)." : "Fichier déposé.");
        return rep;
    }

    /** Devoirs des cours de la classe de l'élève, avec son rendu et sa correction (note + commentaire). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> devoirsEleve(String studentId) {
        Set<String> classes = inscriptions.findByUserId(studentId).stream()
                .map(InscriptionClasseJpaEntity::getClasseId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<String, RenduDevoirJpaEntity> mesRendus = rendus.findByStudentId(studentId).stream()
                .collect(Collectors.toMap(RenduDevoirJpaEntity::getHomeworkId, r -> r, (a, b) -> a));
        String ecole = TenantContext.getCurrentOrganizationId().orElse(null);
        List<Map<String, Object>> liste = new ArrayList<>();
        for (String classe : classes) {
            for (CourseJpaEntity c : cours.findByClasse(classe)) {
                if (ecole != null && c.getOrganizationId() != null && !ecole.equals(c.getOrganizationId())) continue;
                for (HomeworkJpaEntity d : devoirs.findByCourseId(c.getId())) {
                    RenduDevoirJpaEntity r = mesRendus.get(d.getId());
                    boolean depose = r != null && r.estDepose();
                    boolean retard = depose && enRetard(r.getDeposeLe(), dateLimite(d));
                    Map<String, Object> m = vueDevoir(d, c);
                    m.put("statut", depose ? (retard ? "EN_RETARD" : "RENDU") : "NON_RENDU");
                    m.put("rendu", depose ? vueRendu(r, retard) : null);
                    m.put("correction", r != null && r.estCorrige() ? vueCorrection(r) : null);
                    liste.add(m);
                }
            }
        }
        liste.sort(Comparator.comparing(m -> String.valueOf(m.get("deadline"))));
        return liste;
    }

    // ------------------------------------------------------------------ validations (testées unitairement)

    static double noteValide(Object valeur) {
        if (valeur == null || (valeur instanceof String s && s.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La note est obligatoire (nombre entre 0 et 20).");
        }
        double n;
        if (valeur instanceof Number num) {
            n = num.doubleValue();
        } else if (valeur instanceof String s) {
            try {
                n = Double.parseDouble(s.trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La note doit être un nombre entre 0 et 20.");
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La note doit être un nombre entre 0 et 20.");
        }
        if (Double.isNaN(n) || Double.isInfinite(n) || n < 0 || n > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La note doit être comprise entre 0 et 20.");
        }
        return Math.round(n * 100.0) / 100.0;
    }

    static String commentaireValide(Object valeur) {
        if (valeur == null) return null;
        if (!(valeur instanceof String s)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le commentaire doit être un texte.");
        }
        String t = s.trim();
        if (t.length() > COMMENTAIRE_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le commentaire ne doit pas dépasser " + COMMENTAIRE_MAX + " caractères (actuellement " + t.length() + ").");
        }
        return t.isEmpty() ? null : t;
    }

    static void verifierFichier(String nom, String type, byte[] contenu) {
        if (contenu == null || contenu.length == 0) {
            throw new InvalidFileException("Le fichier sélectionné est vide.");
        }
        if (contenu.length > FormatsDocumentsAutorises.TAILLE_MAX_OCTETS) {
            throw new InvalidFileException("Fichier trop volumineux (" + (contenu.length / 1024 / 1024) + " Mo). Limite : 10 Mo.");
        }
        if (nom == null || nom.isBlank()) {
            throw new InvalidFileException("Le fichier n'a pas de nom.");
        }
        if (!FormatsDocumentsAutorises.typeAutorise(type)) {
            throw new InvalidFileException("Format non autorisé (" + type + "). Formats acceptés : " + FormatsDocumentsAutorises.LIBELLE + ".");
        }
        if (!FormatsDocumentsAutorises.signatureConforme(contenu, type)) {
            throw new InvalidFileException("Le contenu du fichier ne correspond pas à son format déclaré (" + type + ").");
        }
    }

    static boolean enRetard(LocalDateTime depose, LocalDate limite) {
        return depose != null && limite != null && depose.toLocalDate().isAfter(limite);
    }

    // ------------------------------------------------------------------ outils

    private CourseJpaEntity coursDeLEcole(String courseId) {
        String ecole = TenantContext.getCurrentOrganizationId().orElse(null);
        return cours.findById(courseId)
                .filter(x -> ecole == null || x.getOrganizationId() == null || ecole.equals(x.getOrganizationId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cours introuvable dans cet établissement."));
    }

    private HomeworkJpaEntity devoirDuCours(String courseId, String homeworkId) {
        return devoirs.findById(homeworkId)
                .filter(d -> Objects.equals(courseId, d.getCourseId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Devoir introuvable dans ce cours."));
    }

    private List<UserJpaEntity> elevesDeLaClasse(CourseJpaEntity c) {
        if (c.getClasse() == null) return List.of();
        Set<String> inscrits = inscriptions.findByClasseId(c.getClasse()).stream()
                .map(InscriptionClasseJpaEntity::getUserId).collect(Collectors.toSet());
        return comptesEcole.membres("ETUDIANT").stream()
                .filter(e -> "ACTIVE".equalsIgnoreCase(e.getValue().getStatus()))
                .map(Map.Entry::getKey)
                .filter(u -> inscrits.contains(u.getId()))
                .sorted(Comparator.comparing((UserJpaEntity u) -> String.valueOf(u.getNom()).toLowerCase(Locale.ROOT))
                        .thenComparing(u -> String.valueOf(u.getPrenom()).toLowerCase(Locale.ROOT)))
                .toList();
    }

    private boolean eleveDansLaClasse(String studentId, CourseJpaEntity c) {
        return c.getClasse() != null && elevesDeLaClasse(c).stream().anyMatch(u -> u.getId().equals(studentId));
    }

    private static LocalDate dateLimite(HomeworkJpaEntity d) {
        String s = d.getDeadline();
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.length() > 10 ? s.substring(0, 10) : s);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static Map<String, Object> vueDevoir(HomeworkJpaEntity d, CourseJpaEntity c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("courseId", d.getCourseId());
        m.put("coursTitre", c == null ? null : c.getName());
        m.put("titre", d.getTitle());
        m.put("description", d.getDescription());
        m.put("priorite", d.getPriority());
        m.put("deadline", d.getDeadline());
        return m;
    }

    private static Map<String, Object> vueRendu(RenduDevoirJpaEntity r, boolean retard) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("fichierNom", r.getFichierNom());
        m.put("fichierUrl", r.getFichierUrl());
        m.put("fichierType", r.getFichierType());
        m.put("fichierTaille", r.getFichierTaille());
        m.put("deposeLe", r.getDeposeLe());
        m.put("enRetard", retard);
        return m;
    }

    private static Map<String, Object> vueCorrection(RenduDevoirJpaEntity r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("note", r.getNote());
        m.put("commentaire", r.getCommentaire());
        m.put("corrigeLe", r.getCorrigeLe());
        return m;
    }
}
