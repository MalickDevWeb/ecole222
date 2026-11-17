package ecole_221.sn.ecole.modules.notifications.application;

import ecole_221.sn.ecole.modules.notifications.infrastructure.persistence.AnnonceEcoleJpaEntity;
import ecole_221.sn.ecole.modules.notifications.infrastructure.persistence.AnnonceEcoleJpaRepository;
import ecole_221.sn.ecole.modules.notifications.infrastructure.persistence.NotificationJpaEntity;
import ecole_221.sn.ecole.modules.notifications.infrastructure.persistence.NotificationJpaRepository;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUser;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Annonces et alertes de l'école reçues par leurs destinataires (décision du Tech Lead du 15/11).
 *
 * <p>Avant : POST /v1/notifications enregistrait une seule ligne destinataire « ALL » que personne ne lisait
 * (findByRoleOrBroadcast n'était appelé nulle part). Désormais la liste des destinataires est calculée PAR LE
 * SERVEUR à partir des appartenances de l'école courante (actives, non expirées, rôle d'appartenance) ; chaque
 * compte reçoit SA notification (destinataire = id du compte, école courante par @TenantId), et l'annonce est
 * gardée dans l'historique de l'école avec le nombre de comptes touchés et l'auteur.</p>
 */
@Service
public class AnnoncesEcoleService {

    private static final Logger log = LoggerFactory.getLogger(AnnoncesEcoleService.class);
    static final int TITRE_MAX = 200;
    static final int MESSAGE_MAX = 5_000;
    static final int MES_MAX = 50;
    /** Marque des notifications issues d'une annonce (action_link = id de l'annonce). */
    static final String TYPE_ANNONCE = "annonce";
    static final String AUCUN_DESTINATAIRE = "Aucun destinataire actif pour ce choix.";
    /** Clés refusées : la liste des destinataires n'est JAMAIS fournie par le client. */
    private static final Set<String> CLES_INTERDITES = Set.of("recipientId", "recipientIds", "destinataireIds",
            "destinatairesIds", "comptes", "userIds");

    /** Choix de destinataires → rôles d'appartenance visés (null = tous les rôles de l'école). */
    public enum Destinataires {
        TOUTE_ECOLE(null),
        ELEVES(Set.of("ETUDIANT", "STUDENT")),
        PARENTS(Set.of("PARENT")),
        PROFESSEURS(Set.of("PROFESSEUR", "TEACHER")),
        /** Personnel hors professeurs : administration, direction, secrétariat, comptabilité, surveillance, RH, vigiles. */
        PERSONNEL(Set.of("ADMIN", "ADMINISTRATOR", "DIRECTEUR", "SECRETAIRE", "COMPTABLE", "SURVEILLANT", "RH", "VIGIL"));

        final Set<String> roles;

        Destinataires(Set<String> roles) {
            this.roles = roles;
        }

        boolean vise(String role) {
            String r = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
            if (ROLES_PLATEFORME.contains(r) || r.isEmpty()) {
                return false;   // un compte de la plateforme n'est pas un destinataire de l'école
            }
            return roles == null || roles.contains(r);
        }
    }

    private static final Set<String> ROLES_PLATEFORME = Set.of("SUPER_ADMIN", "PLATFORM_ADMIN");

    private final OrganizationUserRepositoryPort appartenances;
    private final NotificationJpaRepository notifications;
    private final AnnonceEcoleJpaRepository annonces;
    Clock horloge = Clock.systemDefaultZone();

    public AnnoncesEcoleService(OrganizationUserRepositoryPort appartenances, NotificationJpaRepository notifications,
                                AnnonceEcoleJpaRepository annonces) {
        this.appartenances = appartenances;
        this.notifications = notifications;
        this.annonces = annonces;
    }

    // ── Administration de l'école ───────────────────────────────────────────────────────────────────────────────

    /** {destinataires, nombre} : nombre RÉEL de comptes actifs de l'école courante visés. */
    @Transactional(readOnly = true)
    public Map<String, Object> apercu(String destinataires) {
        Destinataires d = destinataires(destinataires);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("destinataires", d.name());
        r.put("nombre", comptesVises(ecoleCourante(), d).size());
        return r;
    }

    /** 201 {id, destinatairesTouches, envoyeeLe} ; 400 champs ; 409 aucun destinataire actif. */
    @Transactional
    public Map<String, Object> envoyer(Map<String, Object> corps, String auteurId, String auteurNom) {
        if (corps == null || corps.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune donnée reçue.");
        }
        if (corps.keySet().stream().anyMatch(CLES_INTERDITES::contains)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La liste des destinataires est calculée par le serveur : indiquez seulement « destinataires ».");
        }
        String titre = texte(corps.get("titre"), "Le titre", TITRE_MAX);
        String message = texte(corps.get("message"), "Le message", MESSAGE_MAX);
        Destinataires d = destinataires(corps.get("destinataires") instanceof String s ? s : null);
        String nature = nature(corps.get("nature"));
        String ecole = ecoleCourante();

        List<String> comptes = comptesVises(ecole, d);
        if (comptes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, AUCUN_DESTINATAIRE);
        }
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        AnnonceEcoleJpaEntity a = new AnnonceEcoleJpaEntity();
        a.setId(UUID.randomUUID().toString());
        a.setTitre(titre);
        a.setMessage(message);
        a.setNature(nature);
        a.setDestinataires(d.name());
        a.setDestinatairesTouches(comptes.size());
        a.setAuteurId(auteurId);
        a.setAuteur(auteurNom);
        a.setEnvoyeeLe(maintenant);
        annonces.save(a);

        boolean urgence = "URGENCE".equals(nature);
        List<NotificationJpaEntity> lot = new ArrayList<>(comptes.size());
        for (String compte : comptes) {
            NotificationJpaEntity n = new NotificationJpaEntity(UUID.randomUUID().toString(), compte, d.name(), titre, message,
                    urgence ? "urgent" : "info", TYPE_ANNONCE, urgence ? "urgent" : "normal");
            n.setRecipientType("user");
            n.setIsRead(false);
            n.setActionType(TYPE_ANNONCE);
            n.setActionLink(a.getId());
            lot.add(n);
        }
        notifications.saveAll(lot);
        log.info("[ANNONCE] « {} » ({}, {}) envoyée par {} à {} compte(s) de l'école {}", titre, nature, d.name(), auteurId,
                comptes.size(), ecole);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", a.getId());
        r.put("destinatairesTouches", comptes.size());
        r.put("envoyeeLe", maintenant);
        return r;
    }

    /** Historique de l'école : [{id, titre, nature, destinataires, destinatairesTouches, lues, envoyeeLe, auteur}]. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> historique() {
        ecoleCourante();
        return annonces.findAllByOrderByEnvoyeeLeDesc().stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("titre", a.getTitre());
            m.put("nature", a.getNature());
            m.put("destinataires", a.getDestinataires());
            m.put("destinatairesTouches", a.getDestinatairesTouches());
            m.put("lues", notifications.countByActionTypeAndActionLinkAndIsReadTrue(TYPE_ANNONCE, a.getId()));
            m.put("envoyeeLe", a.getEnvoyeeLe());
            m.put("auteur", a.getAuteur());
            return m;
        }).toList();
    }

    // ── Destinataire (tous les rôles) ───────────────────────────────────────────────────────────────────────────

    /** Mes notifications dans l'école courante, récentes d'abord, 50 au plus. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> mes(String compteId) {
        ecoleCourante();
        return notifications.findTop50ByRecipientIdOrderByCreatedAtDesc(obligatoire(compteId)).stream().map(n -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getId());
            m.put("titre", n.getTitle());
            m.put("message", n.getMessage());
            m.put("nature", TYPE_ANNONCE.equals(n.getActionType())
                    ? ("urgent".equals(n.getPriority()) ? "URGENCE" : "ANNONCE") : "INFO");
            m.put("lue", Boolean.TRUE.equals(n.getIsRead()));
            m.put("recueLe", n.getCreatedAt());
            return m;
        }).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> nonLues(String compteId) {
        ecoleCourante();
        Long n = notifications.countUnreadByRecipientId(obligatoire(compteId));
        return Map.of("nombre", n == null ? 0L : n);
    }

    /** Marque SA notification comme lue ; 404 pour celle d'un autre compte ou d'une autre école. */
    @Transactional
    public void marquerLue(String id, String compteId) {
        ecoleCourante();
        String moi = obligatoire(compteId);
        NotificationJpaEntity n = notifications.findById(id == null ? "" : id)
                .filter(x -> moi.equals(x.getRecipientId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification introuvable."));
        if (!Boolean.TRUE.equals(n.getIsRead())) {
            n.setIsRead(true);
            n.setReadAt(LocalDateTime.now(horloge));
            notifications.save(n);
        }
    }

    // ── Règles ──────────────────────────────────────────────────────────────────────────────────────────────────

    /** Comptes visés : appartenances ACTIVES et NON EXPIRÉES de l'école, rôle d'appartenance dans le choix. */
    List<String> comptesVises(String ecole, Destinataires d) {
        return appartenances.findByOrganizationId(ecole).stream()
                .filter(m -> m != null && ecole.equals(m.getOrganizationId()) && m.isActive() && !m.isExpired())
                .filter(m -> d.vise(m.getRole()))
                .map(OrganizationUser::getUserId)
                .filter(u -> u != null && !u.isBlank())
                .distinct()
                .toList();
    }

    private static Destinataires destinataires(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Choisissez les destinataires : TOUTE_ECOLE, ELEVES, PARENTS, PROFESSEURS ou PERSONNEL.");
        }
        try {
            return Destinataires.valueOf(valeur.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Destinataires inconnus : TOUTE_ECOLE, ELEVES, PARENTS, PROFESSEURS ou PERSONNEL attendu.");
        }
    }

    private static String nature(Object valeur) {
        if (valeur == null) {
            return "ANNONCE";
        }
        String v = valeur instanceof String s ? s.trim().toUpperCase(Locale.ROOT) : "";
        if (!v.equals("ANNONCE") && !v.equals("URGENCE")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nature inconnue : ANNONCE ou URGENCE attendu.");
        }
        return v;
    }

    private static String texte(Object valeur, String libelle, int max) {
        if (!(valeur instanceof String s) || s.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, libelle + " est obligatoire.");
        }
        String v = s.strip();
        if (v.length() > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    libelle + " ne doit pas dépasser " + String.format(Locale.FRANCE, "%,d", max).replace(' ', ' ') + " caractères.");
        }
        return v;
    }

    private static String ecoleCourante() {
        return TenantContext.getCurrentOrganizationId()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun établissement sélectionné."));
    }

    private static String obligatoire(String compteId) {
        if (compteId == null || compteId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Veuillez vous connecter.");
        }
        return compteId;
    }
}
