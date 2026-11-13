package ecole_221.sn.ecole.modules.collaboration.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence.ElementCollaboratifJpaEntity;
import ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence.ElementCollaboratifJpaRepository;
import ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence.WorkgroupJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static ecole_221.sn.ecole.partage.ChampsRequete.booleenOptionnel;
import static ecole_221.sn.ecole.partage.ChampsRequete.corpsRequis;
import static ecole_221.sn.ecole.partage.ChampsRequete.entierOptionnel;
import static ecole_221.sn.ecole.partage.ChampsRequete.refus;
import static ecole_221.sn.ecole.partage.ChampsRequete.texte;
import static ecole_221.sn.ecole.partage.ChampsRequete.texteOptionnel;

/**
 * Wiki, notes, journal d'activité, présences aux réunions et devoirs de groupe — enregistrés en base, par école (V8).
 *
 * <p>Avant (balayage du 2026-11-13) : listes en mémoire du serveur, perdues au redémarrage et communes à toutes les
 * écoles (fuite d'une école à l'autre) ; notes privées lues par tous ; corps enregistré tel quel (nombre, liste, null…) ;
 * devoirs de groupe jamais enregistrés. Désormais : seuls les champs connus sont gardés, chacun contrôlé ; l'auteur est
 * le compte connecté ; une note privée n'est lue que par son auteur.</p>
 */
@Service
public class ElementsCollaboratifsService {

    public static final String PAGE = "wiki_page";
    public static final String NOTE = "wiki_note";
    public static final String JOURNAL = "wiki_audit";
    public static final String PRESENCE = "presence_reunion";
    public static final String DEVOIR = "devoir_groupe";

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> ROLES_ENCADRANTS = Set.of("ROLE_ADMIN", "ROLE_ADMINISTRATOR", "ROLE_SUPER_ADMIN",
            "ROLE_PLATFORM_ADMIN", "ROLE_PROFESSEUR", "ROLE_PROFESSOR", "ROLE_TEACHER");

    private final ElementCollaboratifJpaRepository elements;
    private final WorkgroupJpaRepository groupes;

    public ElementsCollaboratifsService(ElementCollaboratifJpaRepository elements, WorkgroupJpaRepository groupes) {
        this.elements = elements;
        this.groupes = groupes;
    }

    // ==================== ÉCRITURE ====================

    @Transactional
    public Map<String, Object> enregistrer(String genre, Map<String, Object> corps, String groupeDuChemin) {
        corpsRequis(corps);
        User moi = moi();
        String cle = texteOptionnel(corps, "id", "L'identifiant", 100);
        if (cle == null) {
            cle = genre + "-" + UUID.randomUUID();
        }
        String groupe = groupeDuChemin != null ? groupeDuChemin : texteOptionnel(corps, "groupId", "Le groupe", 100);
        if (DEVOIR.equals(genre) && (groupe == null || !groupes.existsById(groupe))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Groupe de travail introuvable.");
        }
        Map<String, Object> valeurs = valider(genre, corps, moi);
        if (groupe != null) {
            valeurs.put("groupId", groupe);
        }
        valeurs.put("id", cle);

        ElementCollaboratifJpaEntity e = elements.findByGenreAndCle(genre, cle).orElse(null);
        LocalDateTime maintenant = LocalDateTime.now();
        if (e == null) {
            e = new ElementCollaboratifJpaEntity();
            e.setId(UUID.randomUUID().toString());
            e.setGenre(genre);
            e.setCle(cle);
            e.setAuteurId(moi.getId());
            e.setCreeLe(maintenant);
        } else if (!modifiablePar(genre, e, moi)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, NOTE.equals(genre)
                    ? "Seul l'auteur de la note peut la modifier."
                    : "Cet élément a été enregistré par un autre compte : vous ne pouvez pas le remplacer.");
        }
        e.setGroupeId(groupe);
        e.setPrive(NOTE.equals(genre) && Boolean.TRUE.equals(valeurs.get("isPrivate")));
        e.setModifieLe(maintenant);
        try {
            e.setContenu(JSON.writeValueAsString(valeurs));
        } catch (Exception ex) {
            throw refus("Contenu illisible.");
        }
        elements.save(e);
        return valeurs;
    }

    /** Page de wiki et devoir de groupe : travail collectif (tout compte de l'école) ; le reste : son auteur seulement. */
    private static boolean modifiablePar(String genre, ElementCollaboratifJpaEntity e, User moi) {
        return PAGE.equals(genre) || DEVOIR.equals(genre) || moi.getId().equals(e.getAuteurId());
    }

    private Map<String, Object> valider(String genre, Map<String, Object> c, User moi) {
        Map<String, Object> v = new LinkedHashMap<>();
        String nom = nomAffiche(moi);
        String horodatage = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        switch (genre) {
            case PAGE -> {
                v.put("title", texte(c, "title", "Le titre de la page", 200));
                v.put("content", valeurOuVide(texteOptionnel(c, "content", "Le contenu de la page", 50_000)));
                v.put("author", nom);
                v.put("updatedAt", horodatage);
            }
            case NOTE -> {
                v.put("title", texte(c, "title", "Le titre de la note", 200));
                v.put("content", valeurOuVide(texteOptionnel(c, "content", "Le contenu de la note", 20_000)));
                Boolean prive = booleenOptionnel(c, "isPrivate", "« Note privée »");
                v.put("isPrivate", Boolean.TRUE.equals(prive));
                v.put("authorId", moi.getId());
                v.put("authorName", nom);
            }
            case JOURNAL -> {
                v.put("action", texte(c, "action", "L'action", 500));
                v.put("userName", nom);
                v.put("timestamp", horodatage);
            }
            case PRESENCE -> {
                v.put("meetId", texte(c, "meetId", "La réunion", 100));
                v.put("studentName", nom);
                v.put("emailUsed", valeurOuVide(texteOptionnel(c, "emailUsed", "L'adresse utilisée", 200)));
                v.put("entryTime", valeurOuDefaut(texteOptionnel(c, "entryTime", "L'heure d'entrée", 40), horodatage));
                putSiPresent(v, "exitTime", texteOptionnel(c, "exitTime", "L'heure de sortie", 40));
                putSiPresent(v, "durationMinutes", entierOptionnel(c, "durationMinutes", "La durée (minutes)", 0, 1440));
                putSiPresent(v, "participationScore", entierOptionnel(c, "participationScore", "Le score de participation", 0, 100));
                putSiPresent(v, "isAuthorizedAccount", booleenOptionnel(c, "isAuthorizedAccount", "« Compte autorisé »"));
            }
            case DEVOIR -> {
                v.put("title", texte(c, "title", "Le titre du devoir", 200));
                v.put("description", valeurOuVide(texteOptionnel(c, "description", "La description du devoir", 5_000)));
                v.put("deadline", texte(c, "deadline", "La date limite", 40));
                String piece = texteOptionnel(c, "attachmentUrl", "Le lien de la pièce jointe", 1_000);
                if (piece != null && !piece.matches("(?i)^https?://\\S+$")) {
                    throw refus("Le lien de la pièce jointe doit commencer par http:// ou https://.");
                }
                putSiPresent(v, "attachmentUrl", piece);
                v.put("targetGroups", textes(c.get("targetGroups"), "Les groupes visés", 50, 100));
                v.put("submissions", remises(c.get("submissions")));
            }
            default -> throw new IllegalArgumentException("Genre inconnu : " + genre);
        }
        return v;
    }

    private static List<String> textes(Object valeur, String libelle, int maxElements, int maxLongueur) {
        if (valeur == null) {
            return List.of();
        }
        if (!(valeur instanceof List<?> liste)) {
            throw refus(libelle + " doit être une liste.");
        }
        if (liste.size() > maxElements) {
            throw refus(libelle + " : " + maxElements + " éléments au maximum.");
        }
        List<String> r = new ArrayList<>();
        for (Object o : liste) {
            if (!(o instanceof String s) || s.isBlank() || s.length() > maxLongueur) {
                throw refus(libelle + " : chaque élément doit être un texte de 1 à " + maxLongueur + " caractères.");
            }
            r.add(s.strip());
        }
        return r;
    }

    private static List<Map<String, Object>> remises(Object valeur) {
        if (valeur == null) {
            return List.of();
        }
        if (!(valeur instanceof List<?> liste)) {
            throw refus("Les remises doivent être une liste.");
        }
        if (liste.size() > 200) {
            throw refus("Trop de remises (200 au maximum).");
        }
        List<Map<String, Object>> r = new ArrayList<>();
        for (Object o : liste) {
            if (!(o instanceof Map<?, ?> brut)) {
                throw refus("Chaque remise doit être un objet.");
            }
            @SuppressWarnings("unchecked") Map<String, Object> m = (Map<String, Object>) brut;
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("groupId", texte(m, "groupId", "Le groupe de la remise", 100));
            s.put("groupName", valeurOuVide(texteOptionnel(m, "groupName", "Le nom du groupe", 200)));
            s.put("fileName", texte(m, "fileName", "Le nom du fichier remis", 255));
            s.put("fileUrl", valeurOuVide(texteOptionnel(m, "fileUrl", "Le lien du fichier remis", 1_000)));
            s.put("submittedAt", valeurOuVide(texteOptionnel(m, "submittedAt", "La date de remise", 40)));
            putSiPresent(s, "feedback", texteOptionnel(m, "feedback", "L'appréciation", 2_000));
            Object note = m.get("grade");
            if (note != null) {
                if (!(note instanceof Number n) || n.doubleValue() < 0 || n.doubleValue() > 20) {
                    throw refus("La note doit être un nombre entre 0 et 20.");
                }
                s.put("grade", n);
            }
            r.add(s);
        }
        return r;
    }

    // ==================== LECTURE ====================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> lister(String genre, String groupe) {
        User moi = moi();
        boolean encadrant = encadrant();
        List<ElementCollaboratifJpaEntity> liste = groupe == null
                ? elements.findByGenreOrderByCreeLeAsc(genre)
                : elements.findByGenreAndGroupeIdOrderByCreeLeAsc(genre, groupe);
        List<Map<String, Object>> r = new ArrayList<>();
        for (ElementCollaboratifJpaEntity e : liste) {
            boolean lisible = switch (genre) {
                case NOTE -> !e.isPrive() || moi.getId().equals(e.getAuteurId());
                case PRESENCE -> encadrant || moi.getId().equals(e.getAuteurId());
                default -> true;
            };
            if (lisible) {
                r.add(lire(e));
            }
        }
        return r;
    }

    private static Map<String, Object> lire(ElementCollaboratifJpaEntity e) {
        try {
            return JSON.readValue(e.getContenu(), new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            return Map.of("id", e.getCle());
        }
    }

    // ==================== OUTILS ====================

    private static User moi() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User u && u.getId() != null) {
            return u;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Veuillez vous connecter.");
    }

    private static boolean encadrant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(ROLES_ENCADRANTS::contains);
    }

    private static String nomAffiche(User u) {
        String n = ((u.getPrenom() == null ? "" : u.getPrenom()) + " " + (u.getNom() == null ? "" : u.getNom())).trim();
        return n.isEmpty() ? u.getEmail() : n;
    }

    private static String valeurOuVide(String s) {
        return s == null ? "" : s;
    }

    private static String valeurOuDefaut(String s, String defaut) {
        return s == null ? defaut : s;
    }

    private static void putSiPresent(Map<String, Object> m, String cle, Object valeur) {
        if (valeur != null) {
            m.put(cle, valeur);
        }
    }
}
