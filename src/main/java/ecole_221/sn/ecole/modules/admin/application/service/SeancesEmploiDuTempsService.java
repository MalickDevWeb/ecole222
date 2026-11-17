package ecole_221.sn.ecole.modules.admin.application.service;

import ecole_221.sn.ecole.modules.admin.domain.model.TimeSlot;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaRepository;
import ecole_221.sn.ecole.partage.TexteNormalise;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Emploi du temps de l'administration → séances des professeurs.
 *
 * <p>L'administration construit l'emploi du temps (créneaux de Gestion École › Planning), mais l'emploi du
 * temps du professeur, sa feuille d'appel, le cahier de textes, les absences de l'élève et le planning du parent
 * lisent les SÉANCES ({@code schedule_sessions}).</p>
 *
 * <p>Ici, à chaque enregistrement de l'emploi du temps : un créneau rattaché à une classe = une séance
 * « cre-&lt;créneau&gt; », reliée au cours de la même classe et de même intitulé (appel possible) et au professeur
 * choisi (par son nom, parmi les professeurs de l'école). Un créneau retiré ne supprime pas sa séance (l'appel, le
 * cahier de textes et les demandes de report qui la citent restent lisibles) : elle passe au statut « retire » et
 * disparaît des emplois du temps. Une séance annulée ou reportée par le professeur garde son statut tant que son
 * jour, son horaire et sa salle ne changent pas.</p>
 */
@Service
public class SeancesEmploiDuTempsService {

    public static final String PREFIXE = "cre-";
    public static final String RETIRE = "retire";
    public static final String PLANIFIE = "PLANIFIE";

    /** Bilan renvoyé à l'écran : créneaux sans cours (appel impossible) ou sans professeur reconnu. */
    public record Bilan(int seances, int retirees, List<String> sansCours, List<String> sansProfesseur) {
    }

    private final ComptesEcoleService comptes;
    private final CourseJpaRepository cours;
    private final ScheduleSessionJpaRepository seances;

    public SeancesEmploiDuTempsService(ComptesEcoleService comptes, CourseJpaRepository cours, ScheduleSessionJpaRepository seances) {
        this.comptes = comptes;
        this.cours = cours;
        this.seances = seances;
    }

    /** Clé de comparaison des noms ; jamais null (TexteNormalise.cle renvoie null pour un texte vide). */
    static String cle(String s) {
        String k = TexteNormalise.cle(s);
        return k == null ? "" : k;
    }

    private static final java.util.regex.Pattern HEURE = java.util.regex.Pattern.compile("^(\\d{1,2})\\s*[:hH]\\s*(\\d{2})?$");

    /** « 8h », « 10h30 », « 08:00 » → « 08:00 », « 10:30 » (format lu par tous les écrans) ; sinon inchangé. */
    static String heure(String brut) {
        String t = brut == null ? "" : brut.trim();
        java.util.regex.Matcher m = HEURE.matcher(t);
        return m.matches() ? String.format("%02d:%s", Integer.parseInt(m.group(1)), m.group(2) == null ? "00" : m.group(2)) : t;
    }

    static String horaire(TimeSlot c) {
        String d = heure(c.getHeureDebut());
        String f = heure(c.getHeureFin());
        return f.isEmpty() ? d : d + "-" + f;   // format du serveur « 08:00-10:00 »
    }

    private static String libelle(TimeSlot c) {
        String nom = c.getNom() == null || c.getNom().isBlank() ? "Créneau sans intitulé" : c.getNom().trim();
        return nom + " (" + (c.getDay() == null ? "" : c.getDay()) + " " + horaire(c) + ")";
    }

    @Transactional
    public Bilan synchroniser(List<TimeSlot> creneaux) {
        // Professeurs de l'école par nom normalisé ; un nom porté par deux professeurs n'est pas attribué au hasard.
        Map<String, String> profParNom = new HashMap<>();
        Set<String> ambigus = new HashSet<>();
        for (var m : comptes.membres("PROFESSEUR")) {
            UserJpaEntity u = m.getKey();
            String cle = cle(ComptesEcoleService.nomComplet(u));
            if (cle.isEmpty()) continue;
            if (profParNom.putIfAbsent(cle, u.getId()) != null) ambigus.add(cle);
        }
        ambigus.forEach(profParNom::remove);

        List<CourseJpaEntity> tousCours = cours.findAll();   // @TenantId : cours de cette école
        Map<String, ScheduleSessionJpaEntity> derivees = new HashMap<>();
        for (ScheduleSessionJpaEntity s : seances.findAll()) {
            if (s.getId() != null && s.getId().startsWith(PREFIXE)) derivees.put(s.getId(), s);
        }

        Set<String> gardees = new HashSet<>();
        List<String> sansCours = new ArrayList<>();
        List<String> sansProfesseur = new ArrayList<>();
        LocalDateTime maintenant = LocalDateTime.now();
        for (TimeSlot c : creneaux) {
            if (c.getId() == null || c.getClasseId() == null || c.getClasseId().isBlank()) continue;
            String profId = c.getProfesseur() == null ? null : profParNom.get(cle(c.getProfesseur()));
            String cleNom = cle(c.getNom());
            CourseJpaEntity crs = tousCours.stream()
                    .filter(x -> c.getClasseId().equals(x.getClasse()) && !cleNom.isEmpty() && cleNom.equals(cle(x.getName())))
                    .min((a, b) -> Boolean.compare(!Objects.equals(a.getProfId(), profId), !Objects.equals(b.getProfId(), profId)))
                    .orElse(null);
            if (crs == null) sansCours.add(libelle(c));
            String prof = profId != null ? profId : crs == null ? null : crs.getProfId();
            if (prof == null) sansProfesseur.add(libelle(c));

            String id = PREFIXE + c.getId();
            ScheduleSessionJpaEntity s = derivees.get(id);
            boolean nouvelle = s == null;
            if (nouvelle) {
                s = new ScheduleSessionJpaEntity();
                s.setId(id);
                s.setCreatedAt(maintenant);
            }
            String time = horaire(c);
            boolean deplacee = !nouvelle && !(Objects.equals(s.getDay(), c.getDay()) && Objects.equals(s.getTime(), time)
                    && Objects.equals(s.getRoom(), c.getSalle()));
            s.setCourseId(crs == null ? null : crs.getId());
            s.setCourseTitle(c.getNom());
            s.setClasse(c.getClasseId());
            s.setDay(c.getDay());
            s.setTime(time);
            s.setRoom(c.getSalle());
            s.setType(c.getType());
            s.setProfessorId(prof);
            if (nouvelle || deplacee || RETIRE.equals(s.getStatus()) || s.getStatus() == null) {
                s.setStatus(PLANIFIE);
                s.setCancellationReason(null);
            }
            s.setUpdatedAt(maintenant);
            seances.save(s);
            gardees.add(id);
        }

        int retirees = 0;
        for (ScheduleSessionJpaEntity s : derivees.values()) {
            if (!gardees.contains(s.getId()) && !RETIRE.equals(s.getStatus())) {
                s.setStatus(RETIRE);
                s.setUpdatedAt(maintenant);
                seances.save(s);
                retirees++;
            }
        }
        return new Bilan(gardees.size(), retirees, sansCours, sansProfesseur);
    }
}
