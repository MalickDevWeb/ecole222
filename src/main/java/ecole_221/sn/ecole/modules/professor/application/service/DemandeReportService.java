package ecole_221.sn.ecole.modules.professor.application.service;

import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.DemandeReportJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.DemandeReportJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaEntity;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.ScheduleSessionJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Report de séance : le professeur DEMANDE, l'administration ACCEPTE (le créneau de la séance est alors
 * modifié) ou REFUSE avec un motif (lot E, 2026-11-10). Toutes les lectures sont filtrées par école (@TenantId).
 */
@Service
public class DemandeReportService {

    private final DemandeReportJpaRepository demandes;
    private final ScheduleSessionJpaRepository seances;
    private final UserJpaRepository comptes;

    public DemandeReportService(DemandeReportJpaRepository demandes, ScheduleSessionJpaRepository seances,
                                UserJpaRepository comptes) {
        this.demandes = demandes;
        this.seances = seances;
        this.comptes = comptes;
    }

    @Transactional
    public Map<String, Object> demander(String professeurId, String sessionId, Map<String, ?> corps) {
        if (professeurId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Veuillez vous connecter.");
        ScheduleSessionJpaEntity s = seances.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Séance introuvable dans cet établissement."));
        if (!professeurId.equals(s.getProfessorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette séance n'est pas la vôtre.");
        }
        if (s.getStatus() != null && s.getStatus().toLowerCase(Locale.ROOT).startsWith("annul")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette séance est annulée : elle ne peut pas être reportée.");
        }
        String jour = texte(corps, "jour");
        String horaire = texte(corps, "horaire");
        String salle = texte(corps, "salle");
        String motif = texte(corps, "motif");
        if (jour == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nouveau jour est obligatoire.");
        if (horaire == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nouvel horaire est obligatoire.");
        if (salle == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La salle est obligatoire.");
        if (motif == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le motif de la demande est obligatoire.");
        if (jour.equals(s.getDay()) && horaire.equals(s.getTime()) && salle.equals(s.getRoom())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le créneau demandé est identique au créneau actuel.");
        }
        if (demandes.existsBySessionIdAndStatut(sessionId, DemandeReportJpaEntity.EN_ATTENTE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une demande de report est déjà en attente pour cette séance.");
        }
        DemandeReportJpaEntity d = new DemandeReportJpaEntity();
        d.setId("drp-" + UUID.randomUUID());
        d.setSessionId(s.getId());
        d.setCourseId(s.getCourseId());
        d.setCourseTitle(s.getCourseTitle());
        d.setClasse(s.getClasse());
        d.setProfessorId(professeurId);
        d.setJourActuel(s.getDay());
        d.setHoraireActuel(s.getTime());
        d.setSalleActuelle(s.getRoom());
        d.setJour(jour);
        d.setHoraire(horaire);
        d.setSalle(salle);
        d.setMotif(motif);
        d.setStatut(DemandeReportJpaEntity.EN_ATTENTE);
        d.setCreatedAt(LocalDateTime.now());
        return vue(demandes.save(d));
    }

    public List<Map<String, Object>> duProfesseur(String professeurId) {
        return demandes.findByProfessorIdOrderByCreatedAtDesc(professeurId).stream().map(DemandeReportService::vue).toList();
    }

    /** @param statut filtre facultatif (EN_ATTENTE, ACCEPTEE, REFUSEE) */
    public List<Map<String, Object>> toutes(String statut) {
        List<DemandeReportJpaEntity> liste = statut == null || statut.isBlank()
                ? demandes.findAllByOrderByCreatedAtDesc()
                : demandes.findByStatutOrderByCreatedAtDesc(statut.trim().toUpperCase(Locale.ROOT));
        Map<String, String> noms = new java.util.HashMap<>();
        if (comptes != null) {
            comptes.findAllById(liste.stream().map(DemandeReportJpaEntity::getProfessorId).distinct().toList())
                    .forEach(u -> noms.put(u.getId(), nomComplet(u)));
        }
        return liste.stream().map(d -> {
            Map<String, Object> m = vue(d);
            m.put("professeur", noms.get(d.getProfessorId()));   // null si le compte n'existe plus
            return m;
        }).toList();
    }

    private static String nomComplet(UserJpaEntity u) {
        String n = ((u.getPrenom() == null ? "" : u.getPrenom()) + " " + (u.getNom() == null ? "" : u.getNom())).trim();
        return n.isEmpty() ? u.getEmail() : n;
    }

    @Transactional
    public Map<String, Object> accepter(String demandeId, String adminId) {
        DemandeReportJpaEntity d = enAttente(demandeId);
        ScheduleSessionJpaEntity s = seances.findById(d.getSessionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "La séance de cette demande n'existe plus."));
        s.setDay(d.getJour());
        s.setTime(d.getHoraire());
        s.setRoom(d.getSalle());
        s.setStatus("reporte");
        s.setOriginalDate(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        seances.save(s);
        d.setStatut(DemandeReportJpaEntity.ACCEPTEE);
        d.setDecidePar(adminId);
        d.setDecideLe(LocalDateTime.now());
        return vue(demandes.save(d));
    }

    @Transactional
    public Map<String, Object> refuser(String demandeId, String adminId, String motif) {
        if (motif == null || motif.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le motif du refus est obligatoire.");
        }
        DemandeReportJpaEntity d = enAttente(demandeId);
        d.setStatut(DemandeReportJpaEntity.REFUSEE);
        d.setMotifRefus(motif.trim());
        d.setDecidePar(adminId);
        d.setDecideLe(LocalDateTime.now());
        return vue(demandes.save(d));
    }

    private DemandeReportJpaEntity enAttente(String demandeId) {
        DemandeReportJpaEntity d = demandes.findById(demandeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande de report introuvable dans cet établissement."));
        if (!DemandeReportJpaEntity.EN_ATTENTE.equals(d.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette demande a déjà été traitée.");
        }
        return d;
    }

    static Map<String, Object> vue(DemandeReportJpaEntity d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("sessionId", d.getSessionId());
        m.put("courseId", d.getCourseId());
        m.put("courseTitle", d.getCourseTitle());
        m.put("classe", d.getClasse());
        m.put("professorId", d.getProfessorId());
        m.put("jourActuel", d.getJourActuel());
        m.put("horaireActuel", d.getHoraireActuel());
        m.put("salleActuelle", d.getSalleActuelle());
        m.put("jour", d.getJour());
        m.put("horaire", d.getHoraire());
        m.put("salle", d.getSalle());
        m.put("motif", d.getMotif());
        m.put("statut", d.getStatut());
        m.put("motifRefus", d.getMotifRefus());
        m.put("decidePar", d.getDecidePar());
        m.put("decideLe", d.getDecideLe() == null ? null : d.getDecideLe().toString());
        m.put("createdAt", d.getCreatedAt() == null ? null : d.getCreatedAt().toString());
        return m;
    }

    private static String texte(Map<String, ?> corps, String cle) {
        if (corps == null) return null;
        Object v = corps.get(cle);
        return v == null || v.toString().isBlank() ? null : v.toString().trim();
    }
}
