package ecole_221.sn.ecole.modules.admin.application.service;

import ecole_221.sn.ecole.infrastructure.logging.ActionLogger;
import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService.CompteCree;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaEntity;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Personnel (tout compte non élève) de l'école courante.
 *
 * <p>Révision 2026-11-09 : la liste ne s'appuie plus que sur les appartenances de l'école (avant : repli sur
 * le domaine de l'e-mail, « @gmail.com » compris, et sur une école « default-tenant-id ») ; la création ne
 * reprend plus un compte existant par son e-mail (reprise de compte d'une autre école) et fixe un mot de
 * passe ; le statut affiché est le vrai statut d'appartenance (avant : « Actif » en dur).</p>
 */
@Service
public class AdminStaffService {

    /** Rôles qu'une administration d'école peut attribuer (jamais un rôle plateforme) → libellé affiché. */
    private static final Map<String, String> LIBELLES_ROLES = Map.of(
            "PROFESSEUR", "Enseignant", "ADMIN", "Administrateur", "SECRETAIRE", "Secrétaire",
            "VIGIL", "Vigile", "COMPTABLE", "Comptable", "SURVEILLANT", "Surveillant",
            "DIRECTEUR", "Directeur", "RH", "Ressources humaines");
    /** Libellés saisis dans les écrans (français) → clé de rôle utilisée par la sécurité. */
    private static final Map<String, String> SYNONYMES = Map.ofEntries(
            Map.entry("ENSEIGNANT", "PROFESSEUR"), Map.entry("PROFESSOR", "PROFESSEUR"), Map.entry("TEACHER", "PROFESSEUR"),
            Map.entry("ADMINISTRATEUR", "ADMIN"), Map.entry("SECRÉTAIRE", "SECRETAIRE"), Map.entry("SCOLARITÉ", "SECRETAIRE"),
            Map.entry("SCOLARITE", "SECRETAIRE"), Map.entry("VIGILE", "VIGIL"), Map.entry("GARDIEN", "VIGIL"),
            Map.entry("RESSOURCES HUMAINES", "RH"));

    private final UserJpaRepository userRepo;
    private final OrganizationUserJpaRepository orgUserRepo;
    private final ActionLogger actionLogger;
    private final ComptesEcoleService comptesEcole;

    public AdminStaffService(UserJpaRepository userRepo, OrganizationUserJpaRepository orgUserRepo,
                             ActionLogger actionLogger, ComptesEcoleService comptesEcole) {
        this.userRepo = userRepo;
        this.orgUserRepo = orgUserRepo;
        this.actionLogger = actionLogger;
        this.comptesEcole = comptesEcole;
    }

    public List<Map<String, Object>> getStaffMembers() {
        List<OrganizationUserJpaEntity> liens = orgUserRepo.findByOrganizationId(comptesEcole.ecoleCourante()).stream()
                .filter(l -> l.getRole() != null && !"ETUDIANT".equalsIgnoreCase(l.getRole()))
                .toList();
        Map<String, UserJpaEntity> parId = new HashMap<>();
        userRepo.findAllById(liens.stream().map(OrganizationUserJpaEntity::getUserId).toList())
                .forEach(u -> parId.put(u.getId(), u));
        return liens.stream()
                .filter(l -> parId.containsKey(l.getUserId()))
                .map(l -> toMap(parId.get(l.getUserId()), l))
                .toList();
    }

    private Map<String, Object> toMap(UserJpaEntity u, OrganizationUserJpaEntity lien) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("firstName", u.getPrenom() != null ? u.getPrenom() : "");
        m.put("lastName", u.getNom() != null ? u.getNom() : "");
        m.put("email", u.getEmail());
        m.put("phone", u.getTelephone() != null ? u.getTelephone() : "");
        String role = lien.getRole() != null ? lien.getRole() : u.getRole();
        m.put("role", role);
        m.put("roleLabel", LIBELLES_ROLES.getOrDefault(role == null ? "" : role.toUpperCase(Locale.ROOT), role));
        m.put("matricule", u.getMatricule());
        m.put("status", ComptesEcoleService.libelleStatut(lien.getStatus()));
        return m;
    }

    @Transactional
    public ResponseEntity<?> createStaffMember(Map<String, Object> body) {
        String role = role(texte(body.get("role")));
        CompteCree cree = comptesEcole.creer(texte(body.get("firstName")), texte(body.get("lastName")),
                texte(body.get("email")), texte(body.get("phone")), null, role, texte(body.get("password")));
        UserJpaEntity u = cree.compte();
        actionLogger.logAdminAction("CREATE_STAFF", "ADMIN", u.getId(), "Collaborateur : " + u.getEmail());
        OrganizationUserJpaEntity lien = comptesEcole.appartenance(u.getId(), role).orElseThrow();
        Map<String, Object> res = toMap(u, lien);
        if (cree.motDePasseProvisoire() != null) {
            res.put("motDePasseProvisoire", cree.motDePasseProvisoire());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @Transactional
    public Map<String, Object> updateStaffMember(String id, Map<String, Object> updates) {
        UserJpaEntity u = comptesEcole.membre(id, null);   // 404 si le collaborateur n'est pas de cette école
        OrganizationUserJpaEntity lien = comptesEcole.appartenance(id, null).orElseThrow();
        if (updates.containsKey("firstName")) u.setPrenom(texte(updates.get("firstName")));
        if (updates.containsKey("lastName")) u.setNom(texte(updates.get("lastName")));
        if (updates.containsKey("email")) {
            String mail = texte(updates.get("email"));
            if (mail == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'adresse e-mail est obligatoire.");
            mail = mail.toLowerCase(Locale.ROOT);
            if (!mail.equalsIgnoreCase(u.getEmail()) && userRepo.existsByEmail(mail)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette adresse e-mail est déjà utilisée.");
            }
            u.setEmail(mail);
        }
        if (updates.containsKey("phone")) {
            String tel = texte(updates.get("phone"));
            if (tel != null && !tel.equals(u.getTelephone()) && userRepo.existsByTelephone(tel)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce numéro de téléphone est déjà utilisé.");
            }
            u.setTelephone(tel);
        }
        if (updates.containsKey("role")) {
            String role = role(texte(updates.get("role")));
            lien.setRole(role);   // rôle DANS CETTE école
            u.setRole(role);
            orgUserRepo.save(lien);
        }
        userRepo.save(u);
        actionLogger.logAdminAction("UPDATE_STAFF", "ADMIN", id, "Collaborateur mis à jour : " + u.getEmail());
        return toMap(u, lien);
    }

    private static String role(String saisi) {
        if (saisi == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le rôle est obligatoire.");
        String r = saisi.trim().toUpperCase(Locale.ROOT);
        r = SYNONYMES.getOrDefault(r, r);
        if (!LIBELLES_ROLES.containsKey(r)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle non autorisé : " + saisi);
        }
        return r;
    }

    private static String texte(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }
}
