package ecole_221.sn.ecole.modules.admin.application.service;

import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaEntity;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaRepository;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Comptes (élèves, professeurs, personnel) d'UNE école, créés et listés par son administration.
 *
 * <p>Corrige les défauts constatés le 2026-11-09 :</p>
 * <ul>
 *   <li>comptes créés SANS mot de passe → connexion impossible (401) ;</li>
 *   <li>listes construites avec {@code findByRole} / {@code findAll} → élèves de TOUTES les écoles ;</li>
 *   <li>e-mail inventé « prenom.nom@ecole221.sn » quand l'administration n'en saisissait pas ;</li>
 *   <li>création par e-mail existant → reprise du compte d'une autre personne (rôle écrasé) ;</li>
 *   <li>suppression / modification d'un compte d'une autre école par simple identifiant.</li>
 * </ul>
 *
 * <p>Règles : l'école est celle de la requête (TenantContext), jamais un repli ; un compte n'est visible
 * et modifiable que s'il est membre de cette école ; le mot de passe est soit saisi (8 caractères min.),
 * soit généré aléatoirement et renvoyé UNE SEULE FOIS à l'administrateur pour qu'il le transmette.</p>
 */
@Service
public class ComptesEcoleService {

    public static final int LONGUEUR_MIN_MOT_DE_PASSE = 8;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom HASARD = new SecureRandom();

    private final UserJpaRepository comptes;
    private final OrganizationUserJpaRepository appartenances;
    private final PasswordEncoder encodeur;

    public ComptesEcoleService(UserJpaRepository comptes, OrganizationUserJpaRepository appartenances,
                               PasswordEncoder encodeur) {
        this.comptes = comptes;
        this.appartenances = appartenances;
        this.encodeur = encodeur;
    }

    /** Résultat d'une création : le compte et, s'il a été généré, le mot de passe provisoire (affiché une fois). */
    public record CompteCree(UserJpaEntity compte, String motDePasseProvisoire) {}

    /** École de la requête ; 400 si la requête n'est rattachée à aucune école (pas de repli « ecole-221 »). */
    public String ecoleCourante() {
        return TenantContext.getCurrentOrganizationId()
                .filter(id -> !id.isBlank())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Établissement non identifié pour cette requête."));
    }

    /** Appartenances de l'école pour un rôle (toutes, actives ou suspendues). */
    public List<OrganizationUserJpaEntity> appartenancesDuRole(String role) {
        return appartenances.findByOrganizationIdAndRole(ecoleCourante(), role);
    }

    /** Comptes de l'école ayant ce rôle, avec le statut de leur appartenance. */
    public List<Map.Entry<UserJpaEntity, OrganizationUserJpaEntity>> membres(String role) {
        List<OrganizationUserJpaEntity> liens = appartenancesDuRole(role);
        Map<String, UserJpaEntity> parId = new LinkedHashMap<>();
        comptes.findAllById(liens.stream().map(OrganizationUserJpaEntity::getUserId).toList())
                .forEach(u -> parId.put(u.getId(), u));
        return liens.stream()
                .filter(l -> parId.containsKey(l.getUserId()))
                .map(l -> Map.entry(parId.get(l.getUserId()), l))
                .toList();
    }

    /** Nombre de membres ACTIFS de l'école pour ce rôle. */
    public long nombreActifs(String role) {
        return appartenancesDuRole(role).stream().filter(l -> "ACTIVE".equalsIgnoreCase(l.getStatus())).count();
    }

    /** Appartenance du compte à l'école courante, avec contrôle de rôle facultatif (null = tout rôle sauf élève). */
    public Optional<OrganizationUserJpaEntity> appartenance(String userId, String role) {
        return appartenances.findByUserIdAndOrganizationId(userId, ecoleCourante())
                .filter(l -> role == null ? !"ETUDIANT".equalsIgnoreCase(l.getRole()) : role.equalsIgnoreCase(l.getRole()));
    }

    /** Compte membre de l'école courante, sinon 404 (un compte d'une autre école est invisible). */
    public UserJpaEntity membre(String userId, String role) {
        appartenance(userId, role).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Compte introuvable dans cet établissement."));
        return comptes.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Compte introuvable dans cet établissement."));
    }

    /**
     * Crée un compte rattaché à l'école courante.
     * @param motDePasse saisi par l'administration, ou null / vide pour en générer un provisoire
     */
    @Transactional
    public CompteCree creer(String prenom, String nom, String email, String telephone, String matricule,
                            String role, String motDePasse) {
        String ecole = ecoleCourante();
        if (isBlank(prenom) && isBlank(nom)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire.");
        }
        if (isBlank(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "L'adresse e-mail est obligatoire : elle sert d'identifiant de connexion.");
        }
        String mail = email.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(mail).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adresse e-mail invalide.");
        }
        if (comptes.existsByEmail(mail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette adresse e-mail est déjà utilisée.");
        }
        String tel = isBlank(telephone) ? null : telephone.trim();
        if (tel != null && comptes.existsByTelephone(tel)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce numéro de téléphone est déjà utilisé.");
        }
        String mat = isBlank(matricule) ? nouveauMatricule(role) : matricule.trim();
        if (comptes.findFirstByMatricule(mat).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce matricule est déjà attribué.");
        }
        String provisoire = null;
        String clair = motDePasse;
        if (isBlank(clair)) {
            provisoire = motDePasseProvisoire();
            clair = provisoire;
        } else if (clair.length() < LONGUEUR_MIN_MOT_DE_PASSE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le mot de passe doit contenir au moins " + LONGUEUR_MIN_MOT_DE_PASSE + " caractères.");
        }

        UserJpaEntity u = new UserJpaEntity();
        u.setId("usr-" + UUID.randomUUID());
        u.setPrenom(prenom == null ? "" : prenom.trim());
        u.setNom(nom == null ? "" : nom.trim());
        u.setEmail(mail);
        u.setTelephone(tel);
        u.setMatricule(mat);
        u.setRole(role);
        u.setPassword(encodeur.encode(clair));
        // mot de passe GÉNÉRÉ : à changer à la première connexion (un mot de passe choisi par l'admin ne l'est pas)
        u.setDoitChangerMotDePasse(provisoire != null ? Boolean.TRUE : null);
        comptes.save(u);

        // appartenance explicite (le UserMembershipListener ne crée pas de doublon)
        OrganizationUserJpaEntity lien = new OrganizationUserJpaEntity();
        lien.setId(UUID.randomUUID().toString());
        lien.setUserId(u.getId());
        lien.setOrganizationId(ecole);
        lien.setRole(role);
        lien.setStatus("ACTIVE");
        lien.setScopeType("ORGANIZATION");
        lien.setJoinedAt(LocalDateTime.now());
        appartenances.save(lien);
        return new CompteCree(u, provisoire);
    }

    /** Suspend / réactive l'appartenance à l'école (le compte suspendu ne peut plus accéder à cette école). */
    @Transactional
    public String basculerStatut(String userId, String role) {
        OrganizationUserJpaEntity lien = appartenance(userId, role).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte introuvable dans cet établissement."));
        lien.setStatus("ACTIVE".equalsIgnoreCase(lien.getStatus()) ? "SUSPENDED" : "ACTIVE");
        appartenances.save(lien);
        return lien.getStatus();
    }

    /**
     * Retire le compte de l'école. Le compte lui-même n'est supprimé que s'il n'appartient à aucune autre école
     * (un compte partagé ne doit pas disparaître pour l'autre établissement).
     */
    @Transactional
    public void retirer(String userId, String role) {
        OrganizationUserJpaEntity lien = appartenance(userId, role).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte introuvable dans cet établissement."));
        appartenances.delete(lien);
        boolean ailleurs = appartenances.findByUserId(userId).stream()
                .anyMatch(l -> !Objects.equals(l.getId(), lien.getId()));
        if (!ailleurs) {
            comptes.deleteById(userId);
        }
    }

    public static String libelleStatut(String statut) {
        if (statut == null) return null;
        return switch (statut.toUpperCase(Locale.ROOT)) {
            case "ACTIVE" -> "Actif";
            case "SUSPENDED" -> "Suspendu";
            case "INVITED" -> "Invité";
            case "EXPIRED" -> "Expiré";
            case "TRANSFERRED" -> "Transféré";
            default -> statut;
        };
    }

    public static String nomComplet(UserJpaEntity u) {
        String n = ((u.getPrenom() == null ? "" : u.getPrenom()) + " " + (u.getNom() == null ? "" : u.getNom())).trim();
        return n.isEmpty() ? u.getEmail() : n;
    }

    /** Découpe « Prénom Nom… » : premier mot = prénom, reste = nom (un seul mot = nom). */
    public static String[] decouperNom(String complet) {
        String brut = complet == null ? "" : complet.trim();
        String[] parts = brut.split("\\s+", 2);
        return parts.length > 1 ? new String[] {parts[0], parts[1]} : new String[] {"", brut};
    }

    private String nouveauMatricule(String role) {
        String prefixe = switch (role == null ? "" : role.toUpperCase(Locale.ROOT)) {
            case "ETUDIANT" -> "ETU";
            case "PROFESSEUR" -> "PRF";
            default -> "EMP";
        };
        for (int i = 0; i < 20; i++) {
            String m = prefixe + "-" + Year.now().getValue() + "-" + aleatoire(6).toUpperCase(Locale.ROOT);
            if (comptes.findFirstByMatricule(m).isEmpty()) return m;
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Impossible d'attribuer un matricule unique.");
    }

    private static String motDePasseProvisoire() {
        return aleatoire(12);
    }

    private static String aleatoire(int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(ALPHABET.charAt(HASARD.nextInt(ALPHABET.length())));
        return sb.toString();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
