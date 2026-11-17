package ecole_221.sn.ecole.modules.admin.application.service;

import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.ParentEnfantJpaEntity;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.ParentEnfantJpaRepository;
import ecole_221.sn.ecole.partage.ChampsRequete;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Parents d'un élève, gérés par l'administration de l'école (APP-BE-27).
 *
 * <p>Ici, depuis la fiche de l'élève : lier un parent par son adresse e-mail. Compte inexistant : il est créé avec un
 * mot de passe provisoire (à changer à la première connexion), affiché une seule fois à l'administration. Compte
 * parent existant (par exemple déjà parent dans une autre école) : il est rattaché à cette école et lié, sans nouveau
 * compte. Une adresse qui appartient à un compte non parent est refusée. Délier retire le lien, jamais le compte.
 * Élève d'une autre école : 404 (APP-TL-05). Chaque écriture est inscrite au journal d'audit (intercepteur).</p>
 */
@Service
public class ParentsEleveService {

    public static final String ROLE_PARENT = "PARENT";
    static final List<String> RELATIONS = List.of("PERE", "MERE", "TUTEUR", "AUTRE");
    static final int MAX_PARENTS = 4;

    private final ComptesEcoleService comptes;
    private final UserJpaRepository utilisateurs;
    private final ParentEnfantJpaRepository liens;

    public ParentsEleveService(ComptesEcoleService comptes, UserJpaRepository utilisateurs, ParentEnfantJpaRepository liens) {
        this.comptes = comptes;
        this.utilisateurs = utilisateurs;
        this.liens = liens;
    }

    private static String libelleRelation(String r) {
        return switch (r == null ? "" : r) {
            case "PERE" -> "Père";
            case "MERE" -> "Mère";
            case "TUTEUR" -> "Tuteur";
            default -> "Autre";
        };
    }

    private Map<String, Object> vue(ParentEnfantJpaEntity l, UserJpaEntity u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("parentId", l.getParentId());
        m.put("nom", u == null ? null : ComptesEcoleService.nomComplet(u));
        // identifiant technique d'un parent sans e-mail : jamais affiché
        m.put("email", u == null || u.getEmail() == null || InscriptionAdmisService.sansEmail(u.getEmail()) ? null : u.getEmail());
        m.put("telephone", u == null ? null : u.getTelephone());
        m.put("relation", l.getRelation());
        m.put("relationLibelle", libelleRelation(l.getRelation()));
        m.put("lieLe", l.getCreatedAt() == null ? null : l.getCreatedAt().toString());
        return m;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> parents(String eleveId) {
        comptes.membre(eleveId, "ETUDIANT");   // 404 si l'élève n'est pas de cette école
        List<Map<String, Object>> res = new ArrayList<>();
        for (ParentEnfantJpaEntity l : liens.findByEnfantId(eleveId)) {   // @TenantId : liens de cette école
            res.add(vue(l, utilisateurs.findById(l.getParentId()).orElse(null)));
        }
        return res;
    }

    /**
     * Corps : {email OU telephone, nom (si le compte n'existe pas), relation (PERE, MERE, TUTEUR, AUTRE)}.
     * Un parent SANS e-mail est identifié par son numéro de mobile (E.164) ; son compte
     * reçoit un identifiant technique non livrable ({@code parent-<chiffres>@sans-email.invalid}) et s'active par un
     * lien d'accès (POST /admin/comptes/{id}/lien-acces) : aucun mot de passe n'est affiché ni envoyé.
     */
    @Transactional
    public Map<String, Object> lier(String eleveId, Map<String, Object> corps) {
        ChampsRequete.corpsRequis(corps);
        comptes.membre(eleveId, "ETUDIANT");
        String emailSaisi = ChampsRequete.texteOptionnel(corps, "email", "L'adresse e-mail du parent", 255);
        String telSaisi = ChampsRequete.texteOptionnel(corps, "telephone", "Le téléphone", 30);
        String relation = ChampsRequete.choix(corps, "relation", "Le lien de parenté", RELATIONS, null);   // obligatoire
        if (emailSaisi == null && telSaisi == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Indiquez l'adresse e-mail du parent ou son numéro de téléphone mobile.");
        }
        String telephone = telSaisi == null ? null : ecole_221.sn.ecole.partage.NumeroTelephone.e164(telSaisi)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Numéro de mobile invalide : saisissez par exemple 77 123 45 67, ou +33 6 12 34 56 78 pour l'étranger."));
        boolean sansEmail = emailSaisi == null;
        String email = sansEmail ? "parent-" + telephone.substring(1) + "@" + InscriptionAdmisService.DOMAINE_SANS_EMAIL
                : emailSaisi.toLowerCase(Locale.ROOT);
        if (!sansEmail && InscriptionAdmisService.sansEmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adresse e-mail invalide.");
        }
        List<ParentEnfantJpaEntity> existants = liens.findByEnfantId(eleveId);
        if (existants.size() >= MAX_PARENTS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cet élève a déjà " + MAX_PARENTS + " parents liés : retirez-en un avant d'en ajouter un autre.");
        }

        Optional<UserJpaEntity> compte = sansEmail ? utilisateurs.findByTelephone(telephone) : utilisateurs.findByEmail(email);
        UserJpaEntity parent;
        String provisoire = null;
        boolean compteCree = false;
        if (compte.isPresent()) {
            parent = compte.get();
            // Ne révèle ni le rôle ni l'école d'un compte qui n'est pas parent
            if (!ROLE_PARENT.equalsIgnoreCase(parent.getRole())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, sansEmail
                        ? "Ce numéro de téléphone est déjà utilisé par un compte qui n'est pas un compte parent."
                        : "Cette adresse e-mail est déjà utilisée par un compte qui n'est pas un compte parent.");
            }
            comptes.rattacher(parent, ROLE_PARENT);
        } else {
            String nom = ChampsRequete.texte(corps, "nom", "Le nom du parent", 120);
            String[] n = ComptesEcoleService.decouperNom(nom);
            ComptesEcoleService.CompteCree cree = comptes.creer(n[0], n[1], email, telephone, null, ROLE_PARENT, null);
            parent = cree.compte();
            // sans e-mail : jamais de mot de passe affiché ; le compte s'active par un lien d'accès (WhatsApp / SMS)
            provisoire = sansEmail ? null : cree.motDePasseProvisoire();
            compteCree = true;
        }
        String parentId = parent.getId();
        if (existants.stream().anyMatch(l -> parentId.equals(l.getParentId()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce parent est déjà lié à cet élève.");
        }
        ParentEnfantJpaEntity l = new ParentEnfantJpaEntity();
        l.setId("pe-" + UUID.randomUUID());
        l.setParentId(parentId);
        l.setEnfantId(eleveId);
        l.setRelation(relation);
        l.setCreatedAt(LocalDateTime.now());
        liens.save(l);

        Map<String, Object> res = vue(l, parent);
        res.put("compteCree", compteCree);
        res.put("sansEmail", sansEmail);
        res.put("lienAccesRequis", compteCree && sansEmail);   // activer par POST /admin/comptes/{parentId}/lien-acces
        if (provisoire != null) {
            res.put("motDePasseProvisoire", provisoire);   // affiché une seule fois à l'administration
        }
        return res;
    }

    @Transactional
    public void delier(String eleveId, String parentId) {
        comptes.membre(eleveId, "ETUDIANT");
        ParentEnfantJpaEntity l = liens.findByParentIdAndEnfantId(parentId, eleveId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ce parent n'est pas lié à cet élève."));
        liens.delete(l);
    }
}
