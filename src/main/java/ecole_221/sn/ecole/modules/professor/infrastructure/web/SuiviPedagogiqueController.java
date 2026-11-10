package ecole_221.sn.ecole.modules.professor.infrastructure.web;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.professor.application.service.DemandeReportService;
import ecole_221.sn.ecole.modules.professor.application.service.SuiviPedagogiqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Suivi pédagogique (lot E, 2026-11-10) : demandes de report validées par l'administration, cahier de textes,
 * tableau de bord de la classe et fiche élève. Les routes /professor/courses/{courseId}/** sont contrôlées par
 * AccesCoursInterceptor (cours de l'école et du professeur) ; /admin/** est réservé à l'administration (SecurityConfig).
 */
@RestController
@Tag(name = "Suivi pédagogique", description = "Demandes de report, cahier de textes, tableau de bord de classe, fiche élève")
public class SuiviPedagogiqueController {

    private final DemandeReportService demandes;
    private final SuiviPedagogiqueService suivi;

    public SuiviPedagogiqueController(DemandeReportService demandes, SuiviPedagogiqueService suivi) {
        this.demandes = demandes;
        this.suivi = suivi;
    }

    // ---------- Demandes de report ----------

    @PostMapping("/professor/schedule/{sessionId}/demandes-report")
    @Operation(summary = "Demander le report d'une séance", description = "Corps {jour, horaire, salle, motif}. Séance du professeur connecté. Statut EN_ATTENTE.")
    public ResponseEntity<Map<String, Object>> demander(@PathVariable("sessionId") String sessionId,
                                                        @RequestBody(required = false) Map<String, Object> corps) {
        return ResponseEntity.status(HttpStatus.CREATED).body(demandes.demander(moi(), sessionId, corps));
    }

    @GetMapping("/professor/demandes-report")
    @Operation(summary = "Mes demandes de report")
    public List<Map<String, Object>> mesDemandes() {
        return demandes.duProfesseur(moi());
    }

    @GetMapping("/admin/demandes-report")
    @Operation(summary = "Demandes de report de l'école", description = "Filtre facultatif ?statut=EN_ATTENTE|ACCEPTEE|REFUSEE.")
    public List<Map<String, Object>> toutesLesDemandes(@RequestParam(value = "statut", required = false) String statut) {
        return demandes.toutes(statut);
    }

    @PostMapping("/admin/demandes-report/{id}/accepter")
    @Operation(summary = "Accepter une demande de report", description = "Applique le nouveau créneau à la séance.")
    public Map<String, Object> accepter(@PathVariable("id") String id) {
        return demandes.accepter(id, moi());
    }

    @PostMapping("/admin/demandes-report/{id}/refuser")
    @Operation(summary = "Refuser une demande de report", description = "Corps {motif} obligatoire.")
    public Map<String, Object> refuser(@PathVariable("id") String id, @RequestBody(required = false) Map<String, Object> corps) {
        Object motif = corps == null ? null : corps.get("motif");
        return demandes.refuser(id, moi(), motif == null ? null : motif.toString());
    }

    // ---------- Cahier de textes ----------

    @GetMapping("/professor/courses/{courseId}/cahier-de-textes")
    @Operation(summary = "Cahier de textes du cours")
    public List<Map<String, Object>> cahier(@PathVariable("courseId") String courseId) {
        return suivi.cahierDeTextes(courseId);
    }

    @PostMapping("/professor/courses/{courseId}/cahier-de-textes")
    @Operation(summary = "Remplir le cahier de textes d'une séance", description = "Corps {sessionId, date (AAAA-MM-JJ), contenu, travailAFaire}.")
    public ResponseEntity<Map<String, Object>> ajouter(@PathVariable("courseId") String courseId,
                                                       @RequestBody(required = false) Map<String, Object> corps) {
        return ResponseEntity.status(HttpStatus.CREATED).body(suivi.ajouterEntree(courseId, moi(), corps));
    }

    @GetMapping("/admin/courses/{courseId}/cahier-de-textes")
    @Operation(summary = "Cahier de textes d'un cours (administration)")
    public List<Map<String, Object>> cahierAdmin(@PathVariable("courseId") String courseId) {
        return suivi.cahierDeTextes(courseId);
    }

    @GetMapping("/student/courses/{courseId}/cahier-de-textes")
    @Operation(summary = "Cahier de textes d'un cours de ma classe", description = "404 si l'élève n'est pas inscrit dans la classe du cours.")
    public List<Map<String, Object>> cahierEleve(@PathVariable("courseId") String courseId) {
        return suivi.cahierDeTextesPourEleve(courseId, moi());
    }

    // ---------- Tableau de bord / fiche élève ----------

    @GetMapping("/professor/courses/{courseId}/tableau-de-bord")
    @Operation(summary = "Tableau de bord de la classe", description = "Effectif, moyenne (null sans note), élèves en difficulté (moyenne < 10 ou ≥ 3 absences), progression du programme (null si non calculable).")
    public Map<String, Object> tableauDeBord(@PathVariable("courseId") String courseId) {
        return suivi.tableauDeBord(courseId);
    }

    @GetMapping("/professor/courses/{courseId}/eleves/{studentId}/fiche")
    @Operation(summary = "Fiche d'un élève dans ce cours", description = "Notes, présences (liste et taux), rendus de devoirs.")
    public Map<String, Object> fiche(@PathVariable("courseId") String courseId, @PathVariable("studentId") String studentId) {
        return suivi.ficheEleve(courseId, studentId);
    }

    private static String moi() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User u) return u.getId();
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Veuillez vous connecter.");
    }
}
