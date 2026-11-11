package ecole_221.sn.ecole.modules.parent.application.service;

import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.JustificatifAbsenceJpaEntity;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.JustificatifAbsenceJpaRepository;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.ParentEnfantJpaEntity;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.ParentEnfantJpaRepository;
import ecole_221.sn.ecole.modules.professor.application.service.FeuilleAppelService;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaRepository;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaEntity;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

import static ecole_221.sn.ecole.modules.parent.infrastructure.persistence.JustificatifAbsenceJpaEntity.*;

/**
 * Justification d'une absence ou d'un retard : le PARENT dépose un motif (et un justificatif facultatif),
 * l'ADMINISTRATION accepte — l'absence devient alors « excusée » dans l'appel du professeur — ou refuse
 * avec un motif. Toutes les lectures sont filtrées par école (@TenantId).
 * Remplace l'ancien ParentService.justifyAbsence qui n'enregistrait rien et renvoyait un identifiant inventé.
 */
@Service
public class JustificatifAbsenceService {

    public static final int LONGUEUR_MAX = 500;
    /** Statut stocké d'une absence excusée (même valeur que la feuille d'appel du professeur). */
    static final String STOCKE_EXCUSE = "EXCUSED";

    private final JustificatifAbsenceJpaRepository justificatifs;
    private final AttendanceJpaRepository presences;
    private final ParentEnfantJpaRepository filiations;
    private final CourseJpaRepository cours;
    private final UserJpaRepository comptes;
    private final Clock horloge;

    @org.springframework.beans.factory.annotation.Autowired
    public JustificatifAbsenceService(JustificatifAbsenceJpaRepository justificatifs, AttendanceJpaRepository presences,
                                      ParentEnfantJpaRepository filiations, CourseJpaRepository cours, UserJpaRepository comptes) {
        this(justificatifs, presences, filiations, cours, comptes, Clock.systemDefaultZone());
    }

    JustificatifAbsenceService(JustificatifAbsenceJpaRepository justificatifs, AttendanceJpaRepository presences,
                               ParentEnfantJpaRepository filiations, CourseJpaRepository cours, UserJpaRepository comptes,
                               Clock horloge) {
        this.justificatifs = justificatifs;
        this.presences = presences;
        this.filiations = filiations;
        this.cours = cours;
        this.comptes = comptes;
        this.horloge = horloge;
    }

    // ==================== Parent ====================

    @Transactional
    public Map<String, Object> deposer(String parentId, String presenceId, String motifBrut, String fichierBrut) {
        AttendanceJpaEntity presence = presences.findById(presenceId == null ? "" : presenceId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Présence introuvable dans cet établissement."));
        boolean sonEnfant = filiations.findByParentId(parentId).stream()
                .map(ParentEnfantJpaEntity::getEnfantId).anyMatch(id -> id != null && id.equals(presence.getStudentId()));
        if (!sonEnfant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette présence ne concerne pas l'un de vos enfants.");
        }
        String statut = FeuilleAppelService.statutAffiche(presence.getStatus());
        if ("EXCUSE".equals(statut)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette absence est déjà excusée.");
        }
        if (!"ABSENT".equals(statut) && !"RETARD".equals(statut)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seule une absence ou un retard peut être justifié.");
        }
        String motif = motifValide(motifBrut, "Le motif de l'absence est obligatoire.");
        String fichier = fichierValide(fichierBrut);
        for (JustificatifAbsenceJpaEntity j : justificatifs.findByPresenceId(presence.getId())) {
            if (EN_ATTENTE.equals(j.getStatut())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Un justificatif est déjà en attente de validation pour cette absence.");
            }
            if (ACCEPTE.equals(j.getStatut())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Un justificatif a déjà été accepté pour cette absence.");
            }
        }
        JustificatifAbsenceJpaEntity j = new JustificatifAbsenceJpaEntity();
        j.setId("jus-" + UUID.randomUUID());
        j.setPresenceId(presence.getId());
        j.setEleveId(presence.getStudentId());
        j.setParentId(parentId);
        j.setMotif(motif);
        j.setFichierUrl(fichier);
        j.setStatut(EN_ATTENTE);
        j.setStatutInitial(statut);
        j.setDeposeLe(LocalDateTime.now(horloge));
        return vue(justificatifs.save(j), new Contexte());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> duParent(String parentId) {
        Contexte ctx = new Contexte();
        return justificatifs.findByParentIdOrderByDeposeLeDesc(parentId).stream().map(j -> vue(j, ctx)).toList();
    }

    // ==================== Administration ====================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> liste(String statutBrut) {
        Contexte ctx = new Contexte();
        List<JustificatifAbsenceJpaEntity> l;
        if (statutBrut == null || statutBrut.isBlank()) {
            l = justificatifs.findAllByOrderByDeposeLeDesc();
        } else {
            String s = statutBrut.trim().toUpperCase(Locale.ROOT);
            if (!Set.of(EN_ATTENTE, ACCEPTE, REFUSE).contains(s)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut inconnu : « " + statutBrut + " » (attendu : EN_ATTENTE, ACCEPTE ou REFUSE).");
            }
            l = justificatifs.findByStatutOrderByDeposeLeDesc(s);
        }
        return l.stream().map(j -> vue(j, ctx)).toList();
    }

    /** Accepté : une absence devient « excusée » dans l'appel ; un retard reste un retard (déjà compté comme assidu). */
    @Transactional
    public Map<String, Object> accepter(String adminId, String id) {
        JustificatifAbsenceJpaEntity j = aTraiter(id);
        presences.findById(j.getPresenceId()).ifPresent(p -> {
            if ("ABSENT".equals(FeuilleAppelService.statutAffiche(p.getStatus()))) {
                p.setStatus(STOCKE_EXCUSE);
                presences.save(p);
            }
        });
        j.setStatut(ACCEPTE);
        j.setTraitePar(adminId);
        j.setTraiteLe(LocalDateTime.now(horloge));
        return vue(justificatifs.save(j), new Contexte());
    }

    @Transactional
    public Map<String, Object> refuser(String adminId, String id, String motifBrut) {
        JustificatifAbsenceJpaEntity j = aTraiter(id);
        j.setMotifRefus(motifValide(motifBrut, "Le motif du refus est obligatoire : il sera communiqué au parent."));
        j.setStatut(REFUSE);
        j.setTraitePar(adminId);
        j.setTraiteLe(LocalDateTime.now(horloge));
        return vue(justificatifs.save(j), new Contexte());
    }

    // ==================== Élève ====================

    /** Dernier justificatif de chaque présence de l'élève (clé : identifiant de la présence). */
    @Transactional(readOnly = true)
    public Map<String, Map<String, Object>> parPresence(String eleveId) {
        Map<String, JustificatifAbsenceJpaEntity> derniers = new HashMap<>();
        for (JustificatifAbsenceJpaEntity j : justificatifs.findByEleveId(eleveId)) {
            // le plus récent ; à égalité d'horodatage, un dépôt en cours l'emporte sur un refus
            derniers.merge(j.getPresenceId(), j, (a, b) -> b.getDeposeLe().isAfter(a.getDeposeLe())
                    || (b.getDeposeLe().isEqual(a.getDeposeLe()) && REFUSE.equals(a.getStatut())) ? b : a);
        }
        Map<String, Map<String, Object>> res = new HashMap<>();
        derniers.forEach((presence, j) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("statut", j.getStatut());
            m.put("motifRefus", j.getMotifRefus());
            res.put(presence, m);
        });
        return res;
    }

    // ==================== Règles (testées unitairement) ====================

    static String motifValide(String brut, String siVide) {
        String m = brut == null ? "" : brut.trim();
        if (m.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, siVide);
        if (m.length() > LONGUEUR_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Motif trop long (" + LONGUEUR_MAX + " caractères au plus).");
        }
        return m;
    }

    /** Lien du justificatif (fichier déposé via /media/upload) : facultatif, http(s) uniquement. */
    static String fichierValide(String brut) {
        if (brut == null || brut.isBlank()) return null;
        String f = brut.trim();
        String bas = f.toLowerCase(Locale.ROOT);
        if (f.length() > 1000 || !(bas.startsWith("https://") || bas.startsWith("http://")) || f.contains(" ")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lien du justificatif invalide : déposez le fichier à nouveau.");
        }
        return f;
    }

    private JustificatifAbsenceJpaEntity aTraiter(String id) {
        JustificatifAbsenceJpaEntity j = justificatifs.findById(id == null ? "" : id.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Justificatif introuvable dans cet établissement."));
        if (!EN_ATTENTE.equals(j.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce justificatif a déjà été traité.");
        }
        return j;
    }

    /** Caches de lecture d'une requête (noms, cours, présences). */
    private class Contexte {
        final Map<String, Optional<AttendanceJpaEntity>> p = new HashMap<>();
        final Map<String, String> noms = new HashMap<>();
        final Map<String, String> titres = new HashMap<>();

        Optional<AttendanceJpaEntity> presence(String id) { return p.computeIfAbsent(id, presences::findById); }
        String nom(String id) {
            return id == null ? null : noms.computeIfAbsent(id, k -> comptes.findById(k).map(JustificatifAbsenceService::nomComplet).orElse(null));
        }
        String titre(String id) {
            return id == null ? null : titres.computeIfAbsent(id, k -> cours.findById(k).map(CourseJpaEntity::getName).orElse(null));
        }
    }

    private Map<String, Object> vue(JustificatifAbsenceJpaEntity j, Contexte ctx) {
        Optional<AttendanceJpaEntity> p = ctx.presence(j.getPresenceId());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", j.getId());
        m.put("presenceId", j.getPresenceId());
        m.put("eleveId", j.getEleveId());
        m.put("eleve", ctx.nom(j.getEleveId()));
        m.put("parent", ctx.nom(j.getParentId()));
        m.put("date", p.map(a -> a.getDate() == null ? null : a.getDate().length() >= 10 ? a.getDate().substring(0, 10) : a.getDate()).orElse(null));
        m.put("courseId", p.map(AttendanceJpaEntity::getCourseId).orElse(null));
        m.put("cours", p.map(a -> ctx.titre(a.getCourseId())).orElse(null));
        m.put("statutPresence", j.getStatutInitial());
        m.put("motif", j.getMotif());
        m.put("fichierUrl", j.getFichierUrl());
        m.put("statut", j.getStatut());
        m.put("motifRefus", j.getMotifRefus());
        m.put("deposeLe", j.getDeposeLe() == null ? null : j.getDeposeLe().toString());
        m.put("traiteLe", j.getTraiteLe() == null ? null : j.getTraiteLe().toString());
        return m;
    }

    private static String nomComplet(UserJpaEntity u) {
        String n = ((u.getPrenom() == null ? "" : u.getPrenom()) + " " + (u.getNom() == null ? "" : u.getNom())).trim();
        return n.isEmpty() ? null : n;
    }
}
