package ecole_221.sn.ecole.modules.groupeswhatsapp.application;

import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.groupeswhatsapp.infrastructure.persistence.GroupeWhatsappJpaEntity;
import ecole_221.sn.ecole.modules.groupeswhatsapp.infrastructure.persistence.GroupeWhatsappJpaRepository;
import ecole_221.sn.ecole.modules.groupeswhatsapp.infrastructure.persistence.PartageWhatsappJpaEntity;
import ecole_221.sn.ecole.modules.groupeswhatsapp.infrastructure.persistence.PartageWhatsappJpaRepository;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.ParentEnfantJpaEntity;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.ParentEnfantJpaRepository;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaEntity;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ecole_221.sn.ecole.partage.ChampsRequete.refus;

/**
 * Groupes WhatsApp de l'école et partage des annonces (FEAT-007, validée par le Tech Lead le 16/11/2026).
 *
 * <p>Sans API WhatsApp payante : l'école crée ses groupes dans WhatsApp et les déclare ici avec leur lien d'invitation ;
 * à l'envoi d'une annonce, l'écran ouvre WhatsApp avec le message prêt et l'administrateur choisit le groupe (aucun
 * lien gratuit ne permet de publier directement dans un groupe précis, § 7 H1). Chaque ouverture est notée (BR-05).</p>
 */
@Service
public class GroupesWhatsappService {

    public static final Set<String> PUBLICS = Set.of("TOUTE_ECOLE", "ELEVES", "PARENTS", "PROFESSEURS", "PERSONNEL");
    /** Lien d'invitation d'un groupe WhatsApp (code de 10 à 40 caractères). */
    private static final Pattern LIEN = Pattern.compile("^(?:https?://)?chat\\.whatsapp\\.com/(?:invite/)?([A-Za-z0-9]{10,40})/?$");
    private static final ZoneId FUSEAU = ZoneId.of("Africa/Dakar");

    private final GroupeWhatsappJpaRepository groupes;
    private final PartageWhatsappJpaRepository partages;
    private final ClasseJpaRepository classes;
    private final InscriptionClasseJpaRepository inscriptions;
    private final ParentEnfantJpaRepository liens;

    public GroupesWhatsappService(GroupeWhatsappJpaRepository groupes, PartageWhatsappJpaRepository partages, ClasseJpaRepository classes,
                                  InscriptionClasseJpaRepository inscriptions, ParentEnfantJpaRepository liens) {
        this.groupes = groupes;
        this.partages = partages;
        this.classes = classes;
        this.inscriptions = inscriptions;
        this.liens = liens;
    }

    // ==================== administration ====================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> lister() {
        return groupes.findAllByOrderByNomAsc().stream().map(this::vue).toList();
    }

    @Transactional
    public Map<String, Object> creer(Map<String, Object> corps) {
        GroupeWhatsappJpaEntity g = new GroupeWhatsappJpaEntity();
        g.setId("wag-" + UUID.randomUUID());
        appliquer(g, corps, true);
        if (groupes.existsByLien(g.getLien())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce lien d'invitation est déjà enregistré pour un autre groupe.");
        }
        g.setActif(true);
        g.setCreeLe(LocalDateTime.now(FUSEAU));
        g.setCreePar(compte().getId());
        return vue(groupes.save(g));
    }

    @Transactional
    public Map<String, Object> modifier(String id, Map<String, Object> corps) {
        GroupeWhatsappJpaEntity g = trouver(id);
        appliquer(g, corps, false);
        if (groupes.existsByLienAndIdNot(g.getLien(), g.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce lien d'invitation est déjà enregistré pour un autre groupe.");
        }
        if (corps.get("actif") instanceof Boolean actif) {
            g.setActif(actif);
        }
        return vue(groupes.save(g));
    }

    @Transactional
    public void supprimer(String id) {
        groupes.delete(trouver(id));   // les partages passés restent (nom du groupe conservé)
    }

    /** Note l'ouverture de WhatsApp avec l'annonce prête pour ce groupe (BR-05 : jamais « envoyé »). */
    @Transactional
    public Map<String, Object> noterPartage(String id, Map<String, Object> corps) {
        GroupeWhatsappJpaEntity g = trouver(id);
        if (!g.isActif()) {
            throw refus("Ce groupe est désactivé : réactivez-le pour y partager.");
        }
        String titre = texte(corps == null ? null : corps.get("titre"), "Le titre de l'annonce", 200, true);
        User moi = compte();
        PartageWhatsappJpaEntity p = new PartageWhatsappJpaEntity();
        p.setId("wap-" + UUID.randomUUID());
        p.setOrganizationId(g.getOrganizationId());
        p.setGroupeId(g.getId());
        p.setGroupeNom(g.getNom());
        p.setTitre(titre);
        p.setAuteurId(moi.getId());
        p.setAuteurNom(nomComplet(moi));
        p.setPartageLe(LocalDateTime.now(FUSEAU));
        return vuePartage(partages.save(p));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> historique() {
        return partages.findTop100ByOrderByPartageLeDesc().stream().map(this::vuePartage).toList();
    }

    // ==================== élèves et parents (BR-09) ====================

    /** Groupes que l'élève ou le parent connecté peut rejoindre. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> mesGroupes() {
        User moi = compte();
        String role = moi.getRole() == null ? "" : moi.getRole().value().toUpperCase(Locale.ROOT);
        String publicVise;
        Set<String> mesClasses;
        if (role.equals("ETUDIANT") || role.equals("STUDENT")) {
            publicVise = "ELEVES";
            mesClasses = classesDe(moi.getId());
        } else if (role.equals("PARENT")) {
            publicVise = "PARENTS";
            mesClasses = liens.findByParentId(moi.getId()).stream().map(ParentEnfantJpaEntity::getEnfantId)
                    .filter(Objects::nonNull).flatMap(e -> classesDe(e).stream()).collect(Collectors.toSet());
        } else {
            return List.of();
        }
        return groupes.findAllByOrderByNomAsc().stream()
                .filter(GroupeWhatsappJpaEntity::isActif)
                .filter(g -> "TOUTE_ECOLE".equals(g.getPublicVise())
                        || (publicVise.equals(g.getPublicVise()) && (g.getClasseId() == null || mesClasses.contains(g.getClasseId()))))
                .map(g -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", g.getId());
                    m.put("nom", g.getNom());
                    m.put("classe", g.getClasseNom());
                    m.put("lien", g.getLien());
                    return m;
                }).toList();
    }

    // ==================== règles ====================

    private void appliquer(GroupeWhatsappJpaEntity g, Map<String, Object> corps, boolean creation) {
        if (corps == null || corps.isEmpty()) {
            throw refus("Aucune donnée reçue.");
        }
        if (creation || corps.containsKey("nom")) {
            g.setNom(texte(corps.get("nom"), "Le nom du groupe", 120, true));
        }
        if (creation || corps.containsKey("lien")) {
            g.setLien(lienNormalise(corps.get("lien")));
        }
        if (creation || corps.containsKey("public") || corps.containsKey("classeId")) {
            String publicVise = creation || corps.containsKey("public")
                    ? texte(corps.get("public"), "Le public du groupe", 20, true).toUpperCase(Locale.ROOT) : g.getPublicVise();
            if (!PUBLICS.contains(publicVise)) {
                throw refus("Public invalide : toute l'école, élèves, parents, professeurs ou personnel.");
            }
            String classeId = corps.containsKey("classeId") ? texte(corps.get("classeId"), "La classe", 255, false) : (creation ? null : g.getClasseId());
            if (classeId != null && !publicVise.equals("ELEVES") && !publicVise.equals("PARENTS")) {
                throw refus("Seul un groupe « Élèves » ou « Parents » peut être lié à une classe.");
            }
            String classeNom = null;
            if (classeId != null) {
                ClasseJpaEntity c = classes.findById(classeId)
                        .orElseThrow(() -> refus("Classe introuvable dans cette école."));
                classeNom = c.getName() != null ? c.getName() : c.getNom();
            }
            g.setPublicVise(publicVise);
            g.setClasseId(classeId);
            g.setClasseNom(classeNom);
        }
    }

    /** BR-02 : seul un lien d'invitation WhatsApp est accepté ; il est enregistré sous la forme https://chat.whatsapp.com/CODE. */
    static String lienNormalise(Object valeur) {
        String brut = valeur instanceof String s ? s.trim() : "";
        if (brut.isEmpty()) {
            throw refus("Le lien d'invitation du groupe est obligatoire (WhatsApp › infos du groupe › Inviter via un lien).");
        }
        Matcher m = LIEN.matcher(brut);
        if (!m.matches()) {
            String ouTrouver = " Dans WhatsApp : ouvrez le groupe › infos du groupe › Inviter via un lien › Copier le lien (il commence par https://chat.whatsapp.com/).";
            if (brut.contains("call.whatsapp.com/")) {
                throw refus("C'est le lien d'un appel WhatsApp, pas celui du groupe." + ouTrouver);
            }
            throw refus("Ce n'est pas un lien d'invitation WhatsApp." + ouTrouver);
        }
        return "https://chat.whatsapp.com/" + m.group(1);
    }

    private static String texte(Object valeur, String libelle, int max, boolean obligatoire) {
        String t = valeur instanceof String s ? s.trim() : null;
        if (t == null || t.isEmpty()) {
            if (obligatoire) throw refus(libelle + " est obligatoire.");
            return null;
        }
        if (t.length() > max) {
            throw refus(libelle + " : " + max + " caractères au maximum.");
        }
        return t;
    }

    private GroupeWhatsappJpaEntity trouver(String id) {
        return groupes.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Groupe WhatsApp introuvable."));
    }

    private Set<String> classesDe(String eleveId) {
        return inscriptions.findByUserId(eleveId).stream().map(InscriptionClasseJpaEntity::getClasseId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private static User compte() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User u) {
            return u;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connexion requise.");
    }

    private static String nomComplet(User u) {
        String n = ((u.getPrenom() == null ? "" : u.getPrenom().trim()) + " " + (u.getNom() == null ? "" : u.getNom().trim())).trim();
        return n.isEmpty() ? null : n;
    }

    private Map<String, Object> vue(GroupeWhatsappJpaEntity g) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", g.getId());
        m.put("nom", g.getNom());
        m.put("public", g.getPublicVise());
        m.put("classeId", g.getClasseId());
        m.put("classe", g.getClasseNom());
        m.put("lien", g.getLien());
        m.put("actif", g.isActif());
        m.put("creeLe", g.getCreeLe() == null ? null : g.getCreeLe().toString());
        return m;
    }

    private Map<String, Object> vuePartage(PartageWhatsappJpaEntity p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("groupeId", p.getGroupeId());
        m.put("groupe", p.getGroupeNom());
        m.put("titre", p.getTitre());
        m.put("auteur", p.getAuteurNom());
        m.put("partageLe", p.getPartageLe().toString());
        return m;
    }
}
