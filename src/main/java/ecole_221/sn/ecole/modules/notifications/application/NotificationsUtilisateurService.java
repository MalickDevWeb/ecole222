package ecole_221.sn.ecole.modules.notifications.application;

import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.notifications.infrastructure.persistence.NotificationJpaEntity;
import ecole_221.sn.ecole.modules.notifications.infrastructure.persistence.NotificationJpaRepository;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Notifications affichées dans la cloche de l'élève (route courte {@code /notifications}) — 2026-11-13.
 *
 * <p>Avant : l'envoi ne faisait rien et renvoyait un identifiant inventé, la lecture renvoyait toujours une liste vide,
 * et n'importe quel compte (élève, parent) pouvait « envoyer ». Désormais l'administration ou un professeur notifie
 * les élèves inscrits dans une classe (modification ou annulation d'un cours), et chacun relit seulement les siennes.</p>
 */
@Service
public class NotificationsUtilisateurService {

    static final int TITRE_MAX = 200;
    static final int TEXTE_MAX = 2000;

    /** Genres de notification acceptés → (icône, couleur) affichées par l'écran. */
    private static final Map<String, String[]> GENRES = Map.of(
            "schedule", new String[]{"schedule", "text-amber-600 bg-amber-50"},
            "cancel", new String[]{"cancel", "text-red-600 bg-red-50"},
            "info", new String[]{"notifications", "text-blue-600 bg-blue-50"});

    private final NotificationJpaRepository notifications;
    private final InscriptionClasseJpaRepository inscriptions;
    private final ClasseJpaRepository classes;

    public NotificationsUtilisateurService(NotificationJpaRepository notifications, InscriptionClasseJpaRepository inscriptions,
                                           ClasseJpaRepository classes) {
        this.notifications = notifications;
        this.inscriptions = inscriptions;
        this.classes = classes;
    }

    /** Notifie chaque élève inscrit dans la classe ; renvoie le nombre d'élèves prévenus. */
    @Transactional
    public int notifierClasse(String classeId, String titre, String texte, String genre) {
        String classe = obligatoire(classeId, "La classe concernée", 100);
        String t = obligatoire(titre, "Le titre", TITRE_MAX);
        String d = obligatoire(texte, "Le message", TEXTE_MAX);
        if (!classes.existsById(classe)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe inconnue dans cet établissement : aucun élève n'a été prévenu.");
        }
        String g = genre != null && GENRES.containsKey(genre) ? genre : "info";
        List<String> eleves = inscriptions.findByClasseId(classe).stream()
                .map(InscriptionClasseJpaEntity::getUserId).filter(Objects::nonNull).distinct().toList();
        java.time.LocalDateTime recent = java.time.LocalDateTime.now().minusSeconds(10);
        for (String eleve : eleves) {
            if (notifications.existsByRecipientIdAndTitleAndMessageAndCreatedAtAfter(eleve, t, d, recent)) {
                continue;   // double clic ou double envoi : l'élève a déjà reçu exactement ce message
            }
            NotificationJpaEntity n = new NotificationJpaEntity(UUID.randomUUID().toString(), eleve, "ETUDIANT", t, d,
                    "info", g, "normal");
            n.setRecipientType("user");
            n.setIsRead(false);
            notifications.save(n);
        }
        return eleves.size();
    }

    /**
     * Demande de révision d'une note : transmise à l'administration de l'école (centre de notifications).
     * Avant (2026-11-13) : « PENDING_REVIEW » répondu sans rien enregistrer ; personne n'était prévenu.
     */
    @Transactional
    public void demanderRevisionNote(String eleveId, String eleveNom, String noteId, String module, String raison) {
        String objet = eleveId + ":" + noteId;
        if (notifications.existsByActionTypeAndActionLinkAndIsReadFalse("revision_note", objet)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une demande de révision est déjà en cours pour cette note.");
        }
        NotificationJpaEntity n = new NotificationJpaEntity(UUID.randomUUID().toString(), "role:ADMIN", "ADMIN",
                "Demande de révision de note" + (module == null || module.isBlank() ? "" : " — " + module),
                (eleveNom == null ? "Un élève" : eleveNom) + " demande la révision de sa note"
                        + (module == null || module.isBlank() ? "" : " en " + module) + ". Motif : " + raison.strip(),
                "info", "academic", "normal");
        n.setRecipientType("role");
        n.setIsRead(false);
        n.setActionRequired(true);
        n.setActionType("revision_note");
        n.setActionLink(objet);
        notifications.save(n);
    }

    /** Dernières notifications du compte, plus récentes d'abord, au format attendu par l'écran. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> mesNotifications(String utilisateurId, int limite) {
        int max = Math.max(1, Math.min(limite, 100));
        return notifications.findByRecipientId(utilisateurId).stream()
                .sorted(Comparator.comparing(NotificationJpaEntity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(max)
                .map(NotificationsUtilisateurService::versEcran)
                .toList();
    }

    private static Map<String, Object> versEcran(NotificationJpaEntity n) {
        String[] genre = GENRES.getOrDefault(n.getCategory(), GENRES.get("info"));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.getId());
        m.put("title", n.getTitle());
        m.put("desc", n.getMessage());
        m.put("read", Boolean.TRUE.equals(n.getIsRead()));
        m.put("icon", genre[0]);
        m.put("color", genre[1]);
        long quand = n.getCreatedAt() == null ? 0 : n.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        m.put("createdAt", quand);
        m.put("time", n.getCreatedAt() == null ? "" : n.getCreatedAt().toString());
        return m;
    }

    private static String obligatoire(String valeur, String libelle, int max) {
        if (valeur == null || valeur.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, libelle + " est obligatoire.");
        }
        String v = valeur.strip();
        if (v.length() > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, libelle + " ne doit pas dépasser " + max + " caractères.");
        }
        return v;
    }
}
