package ecole_221.sn.ecole.modules.collaboration.infrastructure.web;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ecole_221.sn.ecole.modules.collaboration.application.port.usecase.CollaborationUseCase;
import ecole_221.sn.ecole.infrastructure.logging.ActionLogger;

import static ecole_221.sn.ecole.partage.ChampsRequete.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/collaboration")
@Tag(name = "Collaboration", description = "Collaboration — groupes de travail, documents, tâches, messages, réunions")
public class CollaborationController {
    private static final Logger logger = LoggerFactory.getLogger(CollaborationController.class);

    /** Lien d'une réunion Google Meet (même expression que POST /student/live-sessions, StudentController). */
    static final String LIEN_MEET = "(?i)^https://meet\\.google\\.com/[a-z]{3}-[a-z]{4}-[a-z]{3}(\\?\\S*)?$";

    private final CollaborationUseCase collaborationUseCase;
    private final ActionLogger actionLogger;

    /** Wiki, présences et devoirs de groupe enregistrés en base par école (V8). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.modules.collaboration.application.service.ElementsCollaboratifsService elementsCollaboratifs;

    /** Membres, chef et classe des groupes (V10). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.modules.collaboration.application.service.MembresGroupeService membresGroupe;

    private final ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort appartenances;
    private final ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort comptes;

    /** Classes virtuelles filtrées par classe pour l'élève et le parent, et notifiées à la publication. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.modules.collaboration.application.service.ClassesVirtuellesService classesVirtuelles;

    @org.springframework.beans.factory.annotation.Autowired
    public CollaborationController(CollaborationUseCase collaborationUseCase, ActionLogger actionLogger,
                                   ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort appartenances,
                                   ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort comptes) {
        this.collaborationUseCase = collaborationUseCase;
        this.actionLogger = actionLogger;
        this.appartenances = appartenances;
        this.comptes = comptes;
    }

    /** Compatibilité (tests) : sans annuaire, aucun étudiant n'est listé. */
    public CollaborationController(CollaborationUseCase collaborationUseCase, ActionLogger actionLogger) {
        this(collaborationUseCase, actionLogger, null, null);
    }

    @GetMapping("/workgroups")
    @Operation(summary = "Liste des groupes de travail", description = "Groupes de l'école avec leurs membres réels (id, name, email, leader), chef (leaderId, leaderName), classe (classId) et nombre de membres.")
    public ResponseEntity<List<Map<String, Object>>> getWorkgroups() {
        Map<String, Map<String, Object>> details = membresGroupe == null ? Map.of() : membresGroupe.detailsParGroupe();
        List<Map<String, Object>> result = collaborationUseCase.getWorkgroups().stream()
            .map(dto -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", dto.id());
                map.put("name", dto.name());
                map.put("nom", dto.name()); // Compatibilité frontend
                map.put("description", dto.description());
                Map<String, Object> d = details.get(dto.id());
                if (d != null) {
                    map.putAll(d);   // members, leaderId, leaderName, classId (réels)
                    map.put("membersCount", ((List<?>) d.get("members")).size());
                } else {
                    map.put("members", List.of());
                    map.put("membersCount", 0);
                }
                return map;
            })
            .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/students")
    @Operation(summary = "Élèves pour former un groupe", description = "Élèves actifs de l'école (id, nom, email) ; ?classeId= : seulement ceux inscrits dans la classe (404 si elle est inconnue).")
    public ResponseEntity<List<Map<String, Object>>> getStudents(
            @Parameter(description = "Classe : seuls les élèves inscrits sont listés") @RequestParam(value = "classeId", required = false) String classeId) {
        java.util.Set<String> filtre = classeId == null || classeId.isBlank() || membresGroupe == null ? null : membresGroupe.elevesDeLaClasse(classeId.strip());
        // Étudiants ACTIFS de l'école courante
        List<Map<String, Object>> students = new java.util.ArrayList<>();
        String ecole = ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext.getCurrentOrganizationId().orElse(null);
        if (ecole != null && appartenances != null && comptes != null) {
            for (var m : appartenances.findByOrganizationIdAndRole(ecole, "ETUDIANT")) {
                if (!m.isActive() || (filtre != null && !filtre.contains(m.getUserId()))) continue;
                comptes.findById(m.getUserId()).ifPresent(u -> {
                    Map<String, Object> e = new java.util.HashMap<>();
                    e.put("id", u.getId());
                    e.put("name", ((u.getPrenom() == null ? "" : u.getPrenom() + " ") + (u.getNom() == null ? "" : u.getNom())).trim());
                    e.put("email", u.getEmail());
                    students.add(e);
                });
            }
        }
        return ResponseEntity.ok(students);
    }

    @PostMapping("/workgroups")
    @Operation(summary = "Créer un groupe", description = "Corps {name (obligatoire, unique dans l'école → 409), description?, classId?, memberIds?, leaderId?}. Enseignant ou administration : membres = élèves actifs de l'école, inscrits dans la classe si elle est donnée, chef parmi les membres (premier membre par défaut). Élève : seul membre et chef ; memberIds, leaderId ou classId → 403. Tout est vérifié avant la création.")
    public ResponseEntity<Map<String, Object>> createWorkgroup(@RequestBody Map<String, Object> body) {
        // APP-QA-04 : nom obligatoire et texte, longueurs bornées
        String name = texte(body, "name", "Le nom du groupe", 100);
        String description = texteOptionnel(body, "description", "La description", 1_000);
        var moi = moi();
        String creatorId = moi.getId();   // créateur = compte connecté, jamais une valeur du client
        // Membres, chef, classe et nom unique vérifiés AVANT la création : rien n'est créé si un élément est refusé.
        // Le tout dans une transaction, sous verrou du nom (envois simultanés : un seul groupe créé).
        var workgroup = membresGroupe == null
                ? collaborationUseCase.createWorkgroup(name, description, creatorId)
                : membresGroupe.creerSousVerrou(name, () -> {
                    var composition = membresGroupe.preparer(body, name, moi);
                    var cree = collaborationUseCase.createWorkgroup(name, description, creatorId);
                    membresGroupe.enregistrer(cree.id(), composition);
                    return cree;
                });

        actionLogger.logCollaborationAction("CREATE_WORKGROUP", creatorId, "NEW",
            "Création groupe: " + name);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("id", workgroup.id());
        return ResponseEntity.ok(res);
    }

    @PutMapping("/workgroups/{groupId}/chef")
    @Operation(summary = "Changer le chef d'un groupe", description = "Corps {leaderId} : un membre du groupe. Réservé à l'encadrement et au chef actuel.")
    public ResponseEntity<Map<String, Object>> changerChef(@PathVariable("groupId") String groupId,
                                                           @RequestBody(required = false) Map<String, Object> body) {
        if (membresGroupe == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Membres des groupes indisponibles.");
        }
        return ResponseEntity.ok(membresGroupe.changerChef(groupId, body, moi()));
    }

    @GetMapping("/workgroups/{groupId}/documents")
    @Operation(summary = "Documents d'un groupe", description = "Retourne les documents partagés dans un groupe.")
    public ResponseEntity<List<Map<String, Object>>> getDocuments(
            @Parameter(description = "Identifiant du groupe") @PathVariable("groupId") String groupId) {
        return ResponseEntity.ok(collaborationUseCase.getDocuments(groupId));
    }

    @PostMapping("/workgroups/{groupId}/documents")
    @Operation(summary = "Ajouter un document", description = "Partage un document dans un groupe (name obligatoire ; url http(s), type, size facultatifs).")
    public ResponseEntity<Map<String, Object>> addDocument(
            @Parameter(description = "Identifiant du groupe") @PathVariable("groupId") String groupId,
            @RequestBody Map<String, Object> body) {
        // APP-QA-04 : champs contrôlés, déposant = compte connecté
        String nom = texte(body, "name", "Le nom du document", 255);
        String url = texteOptionnel(body, "url", "Le lien du document", 1_000);
        if (url != null && !url.matches("(?i)^https?://\\S+$")) {
            throw refus("Le lien du document doit commencer par http:// ou https://.");
        }
        collaborationUseCase.addDocument(groupId, nom, texteOptionnel(body, "type", "Le type", 50),
                texteOptionnel(body, "size", "La taille", 50), url, nomAffiche(moi()));
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/workgroups/{groupId}/homeworks")
    @Operation(summary = "Devoirs d'un groupe", description = "Retourne les devoirs de groupe partagés dans un groupe.")
    public ResponseEntity<List<Map<String, Object>>> getHomeworks(
            @Parameter(description = "Identifiant du groupe") @PathVariable("groupId") String groupId) {
        return ResponseEntity.ok(elementsCollaboratifs.lister(
                ecole_221.sn.ecole.modules.collaboration.application.service.ElementsCollaboratifsService.DEVOIR, groupId));
    }

    @PostMapping("/workgroups/{groupId}/homeworks")
    @Operation(summary = "Enregistrer un devoir de groupe", description = "Crée ou met à jour (même id) un devoir de groupe et ses remises.")
    public ResponseEntity<Map<String, Object>> addHomework(
            @Parameter(description = "Identifiant du groupe") @PathVariable("groupId") String groupId,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> devoir = elementsCollaboratifs.enregistrer(
                ecole_221.sn.ecole.modules.collaboration.application.service.ElementsCollaboratifsService.DEVOIR, body, groupId);
        return ResponseEntity.ok(Map.of("success", true, "id", devoir.get("id")));
    }

    @GetMapping("/workgroups/{groupId}/tasks")
    @Operation(summary = "Tâches d'un groupe", description = "Retourne les tâches assignées dans un groupe.")
    public ResponseEntity<List<Map<String, Object>>> getTasks(
            @Parameter(description = "Identifiant du groupe") @PathVariable("groupId") String groupId) {
        return ResponseEntity.ok(collaborationUseCase.getTasksCompat(groupId));
    }

    @PostMapping("/workgroups/{groupId}/tasks")
    @Operation(summary = "Enregistrer une tâche", description = "Crée une tâche, ou la met à jour si son id existe déjà dans ce groupe (changement de statut).")
    public ResponseEntity<Map<String, Object>> addTask(
            @Parameter(description = "Identifiant du groupe") @PathVariable("groupId") String groupId,
            @RequestBody Map<String, Object> body) {
        // APP-QA-04 : titre obligatoire, statut dans la liste connue ; un changement de statut met à jour la tâche
        String id = collaborationUseCase.enregistrerTache(groupId,
                texteOptionnel(body, "id", "L'identifiant de la tâche", 100),
                texte(body, "title", "Le titre de la tâche", 200),
                statutTache(body),
                texteOptionnel(body, "assignedTo", "La personne assignée", 255));
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("id", id);
        return ResponseEntity.ok(res);
    }

    private static final List<String> STATUTS_TACHE = List.of("A faire", "À faire", "En cours", "A valider", "Terminé");
    /** Codes acceptés aussi (API) : ramenés au libellé affiché par l'écran. */
    private static final Map<String, String> CODES_STATUT = Map.of("A_FAIRE", "A faire", "TODO", "A faire",
            "EN_COURS", "En cours", "IN_PROGRESS", "En cours", "A_VALIDER", "A valider", "TERMINE", "Terminé", "DONE", "Terminé");

    private static String statutTache(Map<String, Object> body) {
        String brut = texteOptionnel(body, "status", "Le statut", 30);
        if (brut != null && CODES_STATUT.containsKey(brut.toUpperCase(java.util.Locale.ROOT))) {
            return CODES_STATUT.get(brut.toUpperCase(java.util.Locale.ROOT));
        }
        return choix(body, "status", "Le statut", STATUTS_TACHE, "A faire");
    }

    @GetMapping("/workgroups/{groupId}/messages")
    @Operation(summary = "Messages d'un groupe", description = "Retourne les messages du chat d'un groupe.")
    public ResponseEntity<List<Map<String, Object>>> getMessages(
            @Parameter(description = "Identifiant du groupe") @PathVariable("groupId") String groupId) {
        return ResponseEntity.ok(collaborationUseCase.getMessages(groupId));
    }

    @PostMapping("/workgroups/{groupId}/messages")
    @Operation(summary = "Envoyer un message", description = "Envoie un message dans le chat d'un groupe (sender, content).")
    public ResponseEntity<Map<String, Object>> addMessage(
            @Parameter(description = "Identifiant du groupe") @PathVariable("groupId") String groupId,
            @RequestBody Map<String, Object> body) {
        // Auteur = compte connecté, jamais le champ « sender » de la requête
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (!(auth != null && auth.getPrincipal() instanceof ecole_221.sn.ecole.modules.auth.domain.model.User u)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Veuillez vous connecter.");
        }
        String auteur = ((u.getPrenom() == null ? "" : u.getPrenom()) + " " + (u.getNom() == null ? "" : u.getNom())).trim();
        Object contenu = body == null ? null : body.get("content");
        collaborationUseCase.addMessage(groupId, auteur.isEmpty() ? u.getEmail() : auteur, contenu instanceof String t ? t : null);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/meets")
    @Operation(summary = "Liste des réunions", description = "Classes virtuelles planifiées. Élève : celles publiées pour ses classes ; "
            + "parent : celles des classes de ses enfants ; personnel et professeurs : toutes.")
    public ResponseEntity<List<Map<String, Object>>> getMeets() {
        List<Map<String, Object>> meets = collaborationUseCase.getMeets();
        if (classesVirtuelles != null) meets = classesVirtuelles.filtrer(meets, moiOuNull());
        return ResponseEntity.ok(meets);
    }

    @PostMapping("/meets")
    @Operation(summary = "Créer une réunion", description = "Planifie une classe virtuelle (subjectName ou title, meetLink ou link : lien Google Meet https://meet.google.com/abc-defg-hij, debut et fin : AAAA-MM-JJTHH:MM, heure de Dakar ; 12 h au plus, fin à venir). Le lien n'est montré aux élèves et aux parents qu'entre le début et la fin.")
    public ResponseEntity<Map<String, Object>> createMeet(@RequestBody Map<String, Object> body) {
        // APP-QA-04 : seuls les champs connus, contrôlés
        corpsRequis(body);
        Map<String, Object> d = new HashMap<>();
        String titre = texteOptionnel(body, "subjectName", "La matière", 200);
        d.put("subjectName", titre != null ? titre : texte(body, "title", "Le titre de la réunion", 200));
        d.put("title", texteOptionnel(body, "title", "Le titre de la réunion", 200) != null ? texteOptionnel(body, "title", "Le titre de la réunion", 200) : d.get("subjectName"));
        String lien = texteOptionnel(body, "meetLink", "Le lien de la réunion", 1_000);
        lien = lien != null ? lien : texte(body, "link", "Le lien de la réunion", 1_000);
        // APP-TL-26 : seul un lien Google Meet collé est accepté, même règle que les cours en direct
        // (StudentController, POST /student/live-sessions).
        if (!lien.matches(LIEN_MEET)) {
            throw refus("Le lien de la réunion doit être un lien Google Meet, de la forme https://meet.google.com/abc-defg-hij.");
        }
        d.put("link", lien);
        d.put("meetLink", lien);
        // Heure de début (le lien apparaît aux élèves) et heure de fin (il disparaît),
        // obligatoires et contrôlées.
        var horaire = ecole_221.sn.ecole.modules.collaboration.application.service.HoraireClasseVirtuelle.lire(
                body, ecole_221.sn.ecole.modules.collaboration.application.service.HoraireClasseVirtuelle.maintenant());
        d.put("debut", horaire.debut());
        d.put("fin", horaire.fin());
        d.put("date", ecole_221.sn.ecole.modules.collaboration.application.service.HoraireClasseVirtuelle.jour(horaire.debut()));
        d.put("time", ecole_221.sn.ecole.modules.collaboration.application.service.HoraireClasseVirtuelle.plageHoraire(horaire.debut(), horaire.fin()));
        d.put("teacherName", texteOptionnel(body, "teacherName", "Le nom de l'enseignant", 200));
        d.put("classIds", listeTextes(body.get("classIds"), "Les classes"));
        d.put("classNames", listeTextes(body.get("classNames"), "Les noms de classes"));
        d.put("isPublished", Boolean.TRUE.equals(booleenOptionnel(body, "isPublished", "« Publiée »")));
        // APP-TL-26 : plus de « compte Google obligatoire » : l'application ne peut pas l'imposer (l'accès à la
        // réunion se règle dans Google Meet par son organisateur) ; le champ était enregistré sans effet.
        collaborationUseCase.createMeet(d);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        // Classe virtuelle publiée → les élèves de la classe et leurs parents sont prévenus
        @SuppressWarnings("unchecked")
        List<String> classes = (List<String>) d.get("classIds");
        if (classesVirtuelles != null && Boolean.TRUE.equals(d.get("isPublished")) && classes != null && !classes.isEmpty()) {
            String sujet = String.valueOf(d.get("subjectName"));
            String quand = " " + ecole_221.sn.ecole.modules.collaboration.application.service.HoraireClasseVirtuelle.libelle(horaire.debut(), horaire.fin());
            String prof = d.get("teacherName") != null ? " avec " + d.get("teacherName") : "";
            var auteur = moiOuNull();
            res.put("prevenus", classesVirtuelles.prevenir(classes, "Classe virtuelle : " + sujet,
                    "Cours en visioconférence" + prof + quand + ". Le lien apparaîtra sur votre accueil à l'heure de début.", auteur == null ? null : auteur.getId()));
        }
        return ResponseEntity.ok(res);
    }

    private static List<String> listeTextes(Object valeur, String libelle) {
        if (valeur == null) {
            return List.of();
        }
        if (!(valeur instanceof List<?> l) || l.size() > 100 || l.stream().anyMatch(x -> !(x instanceof String t) || t.isBlank() || t.length() > 100)) {
            throw refus(libelle + " : liste de 100 textes au maximum (100 caractères chacun).");
        }
        return l.stream().map(x -> ((String) x).strip()).toList();
    }

    @GetMapping("/admin-meets")
    @Operation(summary = "Réunions admin", description = "Retourne les réunions visibles par les administrateurs.")
    public ResponseEntity<List<Map<String, Object>>> getAdminMeets() {
        return ResponseEntity.ok(collaborationUseCase.getMeets());
    }

    @PostMapping("/voice-sessions")
    @Operation(summary = "Créer une session vocale (non disponible)", description = "Répond 501 : fonction non encore réalisée.")
    public ResponseEntity<Map<String, Object>> createVoiceSession(@RequestBody(required = false) Map<String, Object> body) {
        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_IMPLEMENTED, "Les sessions vocales ne sont pas encore disponibles.");
    }
    
    @GetMapping("/voice-sessions")
    @Operation(summary = "Liste des sessions vocales", description = "Retourne toutes les sessions vocales actives.")
    public ResponseEntity<List<Map<String, Object>>> getVoiceSessions() {
        // Retourne une liste vide pour l'instant (à connecter avec la persistance)
        List<Map<String, Object>> sessions = List.of();
        return ResponseEntity.ok(sessions);
    }
    
    @GetMapping("/voice-sessions/{sessionId}")
    @Operation(summary = "Détails d'une session vocale", description = "Aucune session vocale n'existe encore : 404.")
    public ResponseEntity<Map<String, Object>> getVoiceSession(@PathVariable String sessionId) {
        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Session vocale introuvable.");
    }
    
    @DeleteMapping("/voice-sessions/{sessionId}")
    @Operation(summary = "Terminer une session vocale", description = "Aucune session vocale n'existe encore : 404.")
    public ResponseEntity<Map<String, Object>> deleteVoiceSession(@PathVariable String sessionId) {
        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Session vocale introuvable.");
    }
    
    @GetMapping("/workgroups/{groupId}/voice")
    @Operation(summary = "Session vocale d'un groupe", description = "Retourne la session vocale active d'un groupe.")
    public ResponseEntity<Map<String, Object>> getGroupVoiceSession(
            @Parameter(description = "Identifiant du groupe") @PathVariable("groupId") String groupId) {
        Map<String, Object> session = new HashMap<>();
        session.put("groupId", groupId);
        session.put("hasActiveSession", false);
        return ResponseEntity.ok(session);
    }
    
    @PostMapping("/workgroups/{groupId}/voice")
    @Operation(summary = "Démarrer une session vocale de groupe (non disponible)", description = "Répond 501 : fonction non encore réalisée.")
    public ResponseEntity<Map<String, Object>> startGroupVoiceSession(
            @Parameter(description = "Identifiant du groupe") @PathVariable("groupId") String groupId,
            @RequestBody(required = false) Map<String, Object> body) {
        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_IMPLEMENTED, "Les sessions vocales ne sont pas encore disponibles.");
    }

    // ==================== ATTENDANCES & WIKI COLLABORATIF ====================

    @GetMapping("/attendances")
    @Operation(summary = "Présences collaboration", description = "Retourne la liste des présences aux réunions de collaboration.")
    public ResponseEntity<List<Map<String, Object>>> getCollaborationAttendances() {
        // Encadrants : toutes les présences de l'école ; élève : les siennes
        return ResponseEntity.ok(elementsCollaboratifs.lister(
                ecole_221.sn.ecole.modules.collaboration.application.service.ElementsCollaboratifsService.PRESENCE, null));
    }

    @PostMapping("/attendances")
    @Operation(summary = "Enregistrer présence réunion", description = "Enregistre une présence à une réunion de groupe.")
    public ResponseEntity<Map<String, Object>> saveCollaborationAttendance(@RequestBody Map<String, Object> body) {
        elementsCollaboratifs.enregistrer(
                ecole_221.sn.ecole.modules.collaboration.application.service.ElementsCollaboratifsService.PRESENCE, body, null);
        return ResponseEntity.ok(Map.of("success", true));
    }

    private static ecole_221.sn.ecole.modules.auth.domain.model.User moi() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof ecole_221.sn.ecole.modules.auth.domain.model.User u && u.getId() != null) {
            return u;
        }
        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Veuillez vous connecter.");
    }

    private static ecole_221.sn.ecole.modules.auth.domain.model.User moiOuNull() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof ecole_221.sn.ecole.modules.auth.domain.model.User u ? u : null;
    }

    private static String nomAffiche(ecole_221.sn.ecole.modules.auth.domain.model.User u) {
        String n = ((u.getPrenom() == null ? "" : u.getPrenom()) + " " + (u.getNom() == null ? "" : u.getNom())).trim();
        return n.isEmpty() ? u.getEmail() : n;
    }
}

@RestController
@RequestMapping("/wiki")
@Tag(name = "Wiki", description = "Documentation collaborative, notes et logs d'audit")
class WikiController {

    // Base de données par école (V8).
    private final ecole_221.sn.ecole.modules.collaboration.application.service.ElementsCollaboratifsService elements;

    WikiController(ecole_221.sn.ecole.modules.collaboration.application.service.ElementsCollaboratifsService elements) {
        this.elements = elements;
    }

    private static final String PAGE = ecole_221.sn.ecole.modules.collaboration.application.service.ElementsCollaboratifsService.PAGE;
    private static final String NOTE = ecole_221.sn.ecole.modules.collaboration.application.service.ElementsCollaboratifsService.NOTE;
    private static final String JOURNAL = ecole_221.sn.ecole.modules.collaboration.application.service.ElementsCollaboratifsService.JOURNAL;

    @GetMapping("/pages")
    @Operation(summary = "Pages wiki", description = "Liste des pages du wiki collaboratif.")
    public ResponseEntity<List<Map<String, Object>>> getWikiPages() {
        return ResponseEntity.ok(elements.lister(PAGE, null));
    }

    @PostMapping("/pages")
    @Operation(summary = "Sauvegarder page wiki", description = "Crée ou met à jour une page wiki.")
    public ResponseEntity<Map<String, Object>> saveWikiPage(@RequestBody Map<String, Object> body) {
        elements.enregistrer(PAGE, body, null);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/notes")
    @Operation(summary = "Notes collaboratives", description = "Liste des notes partagées.")
    public ResponseEntity<List<Map<String, Object>>> getNotes() {
        return ResponseEntity.ok(elements.lister(NOTE, null));
    }

    @PostMapping("/notes")
    @Operation(summary = "Sauvegarder note", description = "Crée ou met à jour une note collaborative.")
    public ResponseEntity<Map<String, Object>> saveNote(@RequestBody Map<String, Object> body) {
        elements.enregistrer(NOTE, body, null);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/audits")
    @Operation(summary = "Audits wiki", description = "Historique d'audit des actions sur le wiki.")
    public ResponseEntity<List<Map<String, Object>>> getAudits() {
        return ResponseEntity.ok(elements.lister(JOURNAL, null));
    }

    @PostMapping("/audits")
    @Operation(summary = "Sauvegarder audit", description = "Enregistre un événement d'audit.")
    public ResponseEntity<Map<String, Object>> saveAudit(@RequestBody Map<String, Object> body) {
        elements.enregistrer(JOURNAL, body, null);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
