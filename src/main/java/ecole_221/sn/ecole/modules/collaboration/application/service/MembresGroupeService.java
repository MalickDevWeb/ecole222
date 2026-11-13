package ecole_221.sn.ecole.modules.collaboration.application.service;

import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence.MembreGroupeJpaEntity;
import ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence.MembreGroupeJpaRepository;
import ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence.WorkgroupJpaEntity;
import ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence.WorkgroupJpaRepository;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaRepository;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import ecole_221.sn.ecole.partage.ChampsRequete;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Membres, chef et classe des groupes de travail (Collaboration) — V10, 2026-11-13.
 *
 * <p>Avant : seul un compteur existait. Les membres, le chef et la classe choisis par l'enseignant étaient ignorés par
 * le serveur (« Chef : » vide, « 0 membres ») ; le chef n'étant jamais reconnu, aucun élève ne pouvait rendre le devoir
 * de son groupe ; « changer de chef » recréait le groupe en double.</p>
 *
 * <p>Règles : un enseignant ou l'administration compose le groupe avec des élèves actifs de l'école (inscrits dans la
 * classe choisie, s'il y en a une) ; un élève qui crée un groupe en devient le seul membre et le chef ; le chef est
 * toujours un membre ; seuls l'encadrement ou le chef actuel désignent un nouveau chef.</p>
 */
@Service
public class MembresGroupeService {

    static final int MEMBRES_MAX = 60;
    private static final Set<String> ROLES_ENCADRANTS = Set.of("ROLE_ADMIN", "ROLE_ADMINISTRATOR", "ROLE_SUPER_ADMIN",
            "ROLE_PLATFORM_ADMIN", "ROLE_PROFESSEUR", "ROLE_PROFESSOR", "ROLE_TEACHER");

    /** Composition validée, prête à être enregistrée après la création du groupe. */
    public record Composition(String classeId, List<String> membres, String chefId) {}

    private final MembreGroupeJpaRepository membres;
    private final WorkgroupJpaRepository groupes;
    private final OrganizationUserRepositoryPort appartenances;
    private final UserRepositoryPort comptes;
    private final ClasseJpaRepository classes;
    private final InscriptionClasseJpaRepository inscriptions;

    public MembresGroupeService(MembreGroupeJpaRepository membres, WorkgroupJpaRepository groupes,
                                OrganizationUserRepositoryPort appartenances, UserRepositoryPort comptes,
                                ClasseJpaRepository classes, InscriptionClasseJpaRepository inscriptions) {
        this.membres = membres;
        this.groupes = groupes;
        this.appartenances = appartenances;
        this.comptes = comptes;
        this.classes = classes;
        this.inscriptions = inscriptions;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.partage.VerrouCreation verrouCreation;

    /**
     * Création complète d'un groupe dans UNE transaction, sous verrou du nom (2026-11-13) : avant, 10 envois
     * simultanés du même nom créaient 7 groupes, la vérification « nom déjà pris » passant pour tous.
     */
    @Transactional
    public <T> T creerSousVerrou(String nom, java.util.function.Supplier<T> creation) {
        if (verrouCreation != null) {
            verrouCreation.verrouiller("groupe:" + TenantContext.getCurrentOrganizationId().orElse("") + ":"
                    + ecole_221.sn.ecole.partage.TexteNormalise.cle(nom));
        }
        return creation.get();
    }

    /** Vérifie nom, classe, membres et chef AVANT toute création : rien n'est créé si un élément est refusé. */
    @Transactional(readOnly = true)
    public Composition preparer(Map<String, Object> corps, String nom, User moi) {
        if (groupes.existsByNameIgnoreCase(nom)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un groupe porte déjà le nom « " + nom + " » : choisissez un autre nom.");
        }
        if (!encadrant()) {
            if (corps.get("memberIds") != null || corps.get("leaderId") != null || corps.get("classId") != null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Seul un enseignant ou l'administration choisit les membres, le chef et la classe d'un groupe.");
            }
            return new Composition(null, List.of(moi.getId()), moi.getId());
        }
        String classe = ChampsRequete.texteOptionnel(corps, "classId", "La classe", 255);
        if (classe != null && !classes.existsById(classe)) {
            throw ChampsRequete.refus("Classe inconnue dans cette école.");
        }
        List<String> ids = identifiants(corps.get("memberIds"));
        String ecole = TenantContext.getCurrentOrganizationId()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "École courante inconnue."));
        Set<String> inscrits = classe == null ? null : inscriptions.findByClasseId(classe).stream()
                .map(InscriptionClasseJpaEntity::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        for (String id : ids) {
            boolean eleveActif = appartenances.findByUserIdAndOrganizationId(id, ecole)
                    .filter(m -> m.isActive() && "ETUDIANT".equalsIgnoreCase(m.getRole())).isPresent();
            if (!eleveActif) {
                throw ChampsRequete.refus("Membre refusé : « " + id + " » n'est pas un élève actif de cette école.");
            }
            if (inscrits != null && !inscrits.contains(id)) {
                throw ChampsRequete.refus(nom(id) + " n'est pas inscrit(e) dans la classe choisie.");
            }
        }
        String chef = ChampsRequete.texteOptionnel(corps, "leaderId", "Le chef de groupe", 255);
        if (chef != null && !ids.contains(chef)) {
            throw ChampsRequete.refus("Le chef de groupe doit être l'un des membres sélectionnés.");
        }
        return new Composition(classe, ids, chef != null ? chef : (ids.isEmpty() ? null : ids.get(0)));
    }

    @Transactional
    public void enregistrer(String groupeId, Composition c) {
        for (String id : c.membres()) {
            membres.save(new MembreGroupeJpaEntity("mbr-" + UUID.randomUUID(), groupeId, id, id.equals(c.chefId())));
        }
        groupes.findById(groupeId).ifPresent(g -> {
            g.setMembersCount(c.membres().size());
            g.setClasseId(c.classeId());
            groupes.save(g);
        });
    }

    /** Pour chaque groupe de l'école : membres (id, nom, email), chef, classe. */
    @Transactional(readOnly = true)
    public Map<String, Map<String, Object>> detailsParGroupe() {
        Map<String, User> annuaire = new HashMap<>();
        Map<String, List<MembreGroupeJpaEntity>> parGroupe = membres.findAllByOrderByAjouteLeAsc().stream()
                .collect(Collectors.groupingBy(MembreGroupeJpaEntity::getGroupeId, LinkedHashMap::new, Collectors.toList()));
        Map<String, Map<String, Object>> r = new HashMap<>();
        for (WorkgroupJpaEntity g : groupes.findAll()) {
            r.put(g.getId(), details(g.getClasseId(), parGroupe.getOrDefault(g.getId(), List.of()), annuaire));
        }
        return r;
    }

    /** Désigne un nouveau chef parmi les membres (encadrement ou chef actuel uniquement). */
    @Transactional
    public Map<String, Object> changerChef(String groupeId, Map<String, Object> corps, User moi) {
        ChampsRequete.corpsRequis(corps);
        WorkgroupJpaEntity groupe = groupes.findById(groupeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Groupe de travail introuvable."));
        String nouveau = ChampsRequete.texte(corps, "leaderId", "Le nouveau chef", 255);
        List<MembreGroupeJpaEntity> liste = membres.findByGroupeIdOrderByAjouteLeAsc(groupeId);
        boolean estChef = liste.stream().anyMatch(m -> m.isChef() && m.getUtilisateurId().equals(moi.getId()));
        if (!encadrant() && !estChef) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul l'enseignant ou le chef actuel peut désigner un nouveau chef.");
        }
        if (liste.stream().noneMatch(m -> m.getUtilisateurId().equals(nouveau))) {
            throw ChampsRequete.refus("Le nouveau chef doit être un membre du groupe.");
        }
        for (MembreGroupeJpaEntity m : liste) {
            boolean chef = m.getUtilisateurId().equals(nouveau);
            if (m.isChef() != chef) {
                m.setChef(chef);
                membres.save(m);
            }
        }
        return details(groupe.getClasseId(), membres.findByGroupeIdOrderByAjouteLeAsc(groupeId), new HashMap<>());
    }

    /** Élèves inscrits dans une classe (filtre de la liste des élèves proposés). */
    @Transactional(readOnly = true)
    public Set<String> elevesDeLaClasse(String classeId) {
        if (!classes.existsById(classeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe inconnue dans cette école.");
        }
        return inscriptions.findByClasseId(classeId).stream()
                .map(InscriptionClasseJpaEntity::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private Map<String, Object> details(String classeId, List<MembreGroupeJpaEntity> liste, Map<String, User> annuaire) {
        List<Map<String, Object>> vus = new ArrayList<>();
        String chefId = "";
        String chefNom = "";
        for (MembreGroupeJpaEntity m : liste) {
            User u = annuaire.computeIfAbsent(m.getUtilisateurId(), id -> comptes.findById(id).orElse(null));
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("id", m.getUtilisateurId());
            e.put("name", u == null ? "Compte supprimé" : nomAffiche(u));
            e.put("email", u == null ? null : u.getEmail());
            e.put("leader", m.isChef());
            vus.add(e);
            if (m.isChef()) {
                chefId = m.getUtilisateurId();
                chefNom = (String) e.get("name");
            }
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("members", vus);
        r.put("leaderId", chefId);
        r.put("leaderName", chefNom);
        r.put("classId", classeId);
        return r;
    }

    private static List<String> identifiants(Object valeur) {
        if (valeur == null) {
            return List.of();
        }
        if (!(valeur instanceof List<?> liste)) {
            throw ChampsRequete.refus("Les membres doivent être une liste d'identifiants d'élèves.");
        }
        Set<String> ids = new LinkedHashSet<>();   // un élève coché deux fois n'est compté qu'une fois
        for (Object o : liste) {
            if (!(o instanceof String s) || s.isBlank() || s.length() > 255) {
                throw ChampsRequete.refus("Identifiant d'élève invalide dans la liste des membres.");
            }
            ids.add(s.strip());
        }
        if (ids.size() > MEMBRES_MAX) {
            throw ChampsRequete.refus("Un groupe compte au plus " + MEMBRES_MAX + " membres.");
        }
        return List.copyOf(ids);
    }

    private String nom(String id) {
        return comptes.findById(id).map(MembresGroupeService::nomAffiche).orElse(id);
    }

    private static String nomAffiche(User u) {
        String n = ((u.getPrenom() == null ? "" : u.getPrenom()) + " " + (u.getNom() == null ? "" : u.getNom())).trim();
        return n.isEmpty() ? u.getEmail() : n;
    }

    private static boolean encadrant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(ROLES_ENCADRANTS::contains);
    }
}
