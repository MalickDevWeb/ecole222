package ecole_221.sn.ecole.modules.collaboration.application.service;

import ecole_221.sn.ecole.modules.admin.rbac.CataloguePermissions;
import ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence.MeetJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaRepository;
import ecole_221.sn.ecole.partage.CompteCourant;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

/**
 * Qui peut arrêter (mettre à la corbeille) une séance en direct (2026-11-16, V22).
 *
 * <p>Avant : la table meets ne gardait pas l'auteur ; n'importe quel professeur de l'école arrêtait la séance d'un
 * collègue. Désormais :</p>
 * <ul>
 *   <li>l'administration de l'école peut toujours l'arrêter ;</li>
 *   <li>l'auteur de la séance (compte qui l'a lancée) ;</li>
 *   <li>l'enseignant du cours rattaché à la séance (course_id) ;</li>
 *   <li>pour une séance antérieure à V22 (ni auteur ni cours) : l'enseignant d'un cours de l'école portant
 *       l'intitulé de la séance ;</li>
 *   <li>tout autre compte : 403.</li>
 * </ul>
 * Les cours sont lus avec le filtre d'école (@TenantId) : un cours d'une autre école ne donne aucun droit.
 */
@Component
public class DroitsSeanceEnDirect {

    private final CourseJpaRepository cours;

    public DroitsSeanceEnDirect(CourseJpaRepository cours) {
        this.cours = cours;
    }

    /** Identifiant du cours du professeur qui porte exactement cet intitulé (casse et espaces ignorés), s'il est unique. */
    public String coursDuProfesseur(String intitule, String profId) {
        if (intitule == null || intitule.isBlank() || profId == null) return null;
        List<String> ids = cours.findByProfId(profId).stream()
                .filter(c -> memeIntitule(c.getName(), intitule))
                .map(CourseJpaEntity::getId)
                .toList();
        return ids.size() == 1 ? ids.get(0) : null;
    }

    /** 403 si le compte connecté n'est ni l'administration, ni l'auteur, ni l'enseignant du cours de la séance. */
    public void exigerArretAutorise(MeetJpaEntity seance) {
        if (administration()) return;
        String moi = CompteCourant.id();
        if (moi != null) {
            if (moi.equals(seance.getAuteurId())) return;
            if (seance.getCourseId() != null
                    && cours.findById(seance.getCourseId()).map(c -> moi.equals(c.getProfId())).orElse(false)) {
                return;
            }
            if (seance.getAuteurId() == null && seance.getCourseId() == null
                    && cours.findByProfId(moi).stream().anyMatch(c -> memeIntitule(c.getName(), seance.getSubjectName())
                            || memeIntitule(c.getName(), seance.getTitle()))) {
                return;   // séance antérieure à V22 : l'enseignant du cours de même intitulé
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Seul l'enseignant qui a lancé cette séance, l'enseignant du cours ou l'administration de l'école peut l'arrêter.");
    }

    private static boolean memeIntitule(String a, String b) {
        return a != null && b != null && normaliser(a).equals(normaliser(b)) && !normaliser(a).isEmpty();
    }

    private static String normaliser(String s) {
        return s.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static boolean administration() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        for (GrantedAuthority a : auth.getAuthorities()) {
            String r = a.getAuthority();
            if (r != null && r.startsWith("ROLE_") && CataloguePermissions.ROLES_ADMINISTRATION.contains(r.substring(5))) return true;
        }
        return false;
    }
}
