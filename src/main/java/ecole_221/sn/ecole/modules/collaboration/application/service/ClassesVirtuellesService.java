package ecole_221.sn.ecole.modules.collaboration.application.service;

import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.notifications.infrastructure.persistence.NotificationJpaEntity;
import ecole_221.sn.ecole.modules.notifications.infrastructure.persistence.NotificationJpaRepository;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.ParentEnfantJpaEntity;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.ParentEnfantJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Classes virtuelles (Collaboration › Classes virtuelles) vues par les élèves et leurs parents — 2026-11-16.
 *
 * <p>Avant : GET /collaboration/meets renvoyait à tout compte les classes virtuelles de TOUTES les classes, et la
 * publication ne prévenait personne ; les élèves de la classe ne la trouvaient qu'en ouvrant « Collaboration ».
 * Désormais : un élève ne reçoit que les classes virtuelles publiées pour ses classes, un parent celles des classes de
 * ses enfants, un professeur celles des classes où il enseigne (ses matières et ses séances d'emploi du temps), celles
 * qui portent son nom et celles qu'il a créées ; le personnel les voit toutes. À la publication, chaque élève de la
 * classe, chacun de ses parents et chaque professeur de la classe (sauf l'auteur) reçoit une notification (cloche).</p>
 */
@Service
public class ClassesVirtuellesService {

    private final InscriptionClasseJpaRepository inscriptions;
    private final ParentEnfantJpaRepository liens;
    private final NotificationJpaRepository notifications;
    private final CourseJpaRepository cours;
    private final ScheduleSessionJpaRepository seances;

    public ClassesVirtuellesService(InscriptionClasseJpaRepository inscriptions, ParentEnfantJpaRepository liens,
                                    NotificationJpaRepository notifications, CourseJpaRepository cours,
                                    ScheduleSessionJpaRepository seances) {
        this.inscriptions = inscriptions;
        this.liens = liens;
        this.notifications = notifications;
        this.cours = cours;
        this.seances = seances;
    }

    private static boolean professeur(User u) {
        String r = role(u);
        return r.equals("PROFESSEUR") || r.equals("TEACHER");
    }

    private static String cle(Object s) {
        return s == null ? "" : String.valueOf(s).trim().toLowerCase(Locale.ROOT);
    }

    private static boolean active(ScheduleSessionJpaEntity s) {
        return !"retire".equals(s.getStatus());
    }

    /** Classes où le professeur enseigne : ses matières et ses séances d'emploi du temps encore planifiées. */
    private Set<String> classesDuProfesseur(String profId) {
        Set<String> classes = new LinkedHashSet<>();
        cours.findByProfId(profId).stream().map(CourseJpaEntity::getClasse).filter(Objects::nonNull).forEach(classes::add);
        seances.findByProfessorId(profId).stream().filter(ClassesVirtuellesService::active)
                .map(ScheduleSessionJpaEntity::getClasse).filter(Objects::nonNull).forEach(classes::add);
        return classes;
    }

    private static String role(User u) {
        return u == null || u.getRole() == null ? "" : u.getRole().value().toUpperCase(Locale.ROOT);
    }

    private Set<String> classesDe(String eleveId) {
        return inscriptions.findByUserId(eleveId).stream().map(InscriptionClasseJpaEntity::getClasseId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /** Classes dont le compte voit les classes virtuelles ; null = toutes (personnel, professeurs). */
    @Transactional(readOnly = true)
    public Set<String> classesVisibles(User u) {
        return switch (role(u)) {
            case "ETUDIANT", "STUDENT" -> classesDe(u.getId());
            case "PARENT" -> liens.findByParentId(u.getId()).stream().map(ParentEnfantJpaEntity::getEnfantId)
                    .filter(Objects::nonNull).flatMap(e -> classesDe(e).stream()).collect(Collectors.toSet());
            case "PROFESSEUR", "TEACHER" -> classesDuProfesseur(u.getId());
            default -> null;
        };
    }

    /** Élève ou parent : seulement les classes virtuelles PUBLIÉES pour ses classes (ou celles de ses enfants). */
    public List<Map<String, Object>> filtrer(List<Map<String, Object>> meets, User u) {
        Set<String> classes = classesVisibles(u);
        if (classes == null) return meets;
        if (professeur(u)) {
            // Professeur : ses classes, les classes virtuelles à son nom et celles qu'il a créées (publiées ou non)
            String nom = cle(((u.getPrenom() == null ? "" : u.getPrenom()) + " " + (u.getNom() == null ? "" : u.getNom())).trim());
            return meets.stream().filter(m -> u.getId().equals(m.get("auteurId"))
                    || (!nom.isEmpty() && nom.equals(cle(m.get("teacherName"))))
                    || (m.get("classIds") instanceof Collection<?> ids && ids.stream().map(String::valueOf).anyMatch(classes::contains)))
                    .toList();
        }
        // 16/11 (demande du Tech Lead) : élève et parent ne voient la classe virtuelle que jusqu'à son heure de fin, et
        // son lien seulement à partir de l'heure de début (avant : lien visible en permanence, même des semaines après).
        return meets.stream()
                .filter(m -> Boolean.TRUE.equals(m.get("isPublished")))
                .filter(m -> m.get("classIds") instanceof Collection<?> ids && ids.stream().map(String::valueOf).anyMatch(classes::contains))
                .filter(m -> HoraireClasseVirtuelle.A_VENIR.equals(m.get("statut")) || HoraireClasseVirtuelle.EN_COURS.equals(m.get("statut")))
                .map(m -> {
                    if (!HoraireClasseVirtuelle.A_VENIR.equals(m.get("statut"))) return m;
                    Map<String, Object> sansLien = new java.util.HashMap<>(m);
                    sansLien.put("link", null);
                    sansLien.put("meetLink", null);
                    return sansLien;
                })
                .toList();
    }

    /**
     * Prévient chaque élève inscrit dans ces classes, ses parents et les professeurs de ces classes, sauf l'auteur (une
     * notification par compte, même s'il est concerné par plusieurs classes). Renvoie {eleves, parents, professeurs} :
     * nombres RÉELS de comptes prévenus.
     */
    @Transactional
    public Map<String, Integer> prevenir(Collection<String> classIds, String titre, String texte, String auteurId) {
        Set<String> eleves = new LinkedHashSet<>();
        for (String c : classIds == null ? List.<String>of() : classIds) {
            if (c == null || c.isBlank()) continue;
            inscriptions.findByClasseId(c).stream().map(InscriptionClasseJpaEntity::getUserId).filter(Objects::nonNull).forEach(eleves::add);
        }
        Set<String> parents = new LinkedHashSet<>();
        for (String e : eleves) {
            liens.findByEnfantId(e).stream().map(ParentEnfantJpaEntity::getParentId).filter(Objects::nonNull).forEach(parents::add);
        }
        List<String> ids = classIds == null ? List.of() : classIds.stream().filter(c -> c != null && !c.isBlank()).toList();
        Set<String> professeurs = new LinkedHashSet<>();
        for (String c : ids) {
            cours.findByClasse(c).stream().map(CourseJpaEntity::getProfId).filter(Objects::nonNull).forEach(professeurs::add);
        }
        if (!ids.isEmpty()) {
            seances.findByClasseIn(ids).stream().filter(ClassesVirtuellesService::active)
                    .map(ScheduleSessionJpaEntity::getProfessorId).filter(Objects::nonNull).forEach(professeurs::add);
        }
        if (auteurId != null) professeurs.remove(auteurId);   // l'auteur sait déjà qu'il l'a publiée
        eleves.forEach(id -> notifier(id, "ETUDIANT", titre, texte));
        parents.forEach(id -> notifier(id, "PARENT", titre, texte));
        professeurs.forEach(id -> notifier(id, "PROFESSEUR", titre, texte));
        Map<String, Integer> r = new LinkedHashMap<>();
        r.put("eleves", eleves.size());
        r.put("parents", parents.size());
        r.put("professeurs", professeurs.size());
        return r;
    }

    private void notifier(String compte, String role, String titre, String texte) {
        NotificationJpaEntity n = new NotificationJpaEntity(UUID.randomUUID().toString(), compte, role, titre, texte,
                "info", "schedule", "normal");
        n.setRecipientType("user");
        n.setIsRead(false);
        notifications.save(n);
    }
}
