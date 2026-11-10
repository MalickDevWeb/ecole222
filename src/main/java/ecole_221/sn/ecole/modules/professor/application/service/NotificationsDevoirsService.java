package ecole_221.sn.ecole.modules.professor.application.service;

import ecole_221.sn.ecole.modules.notifications.application.port.usecase.NotificationUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Notifications des élèves pour les devoirs (lot B) : nouveau devoir publié, correction publiée.
 *
 * <p>Réutilise le module notifications existant. Une notification qui échoue n'annule JAMAIS le devoir ni la
 * correction : le résultat (envoyées / échecs / message) est renvoyé au professeur pour qu'il soit informé honnêtement.</p>
 */
@Service
public class NotificationsDevoirsService {

    private static final Logger log = LoggerFactory.getLogger(NotificationsDevoirsService.class);

    private final NotificationUseCase notifications;

    @org.springframework.beans.factory.annotation.Autowired
    public NotificationsDevoirsService(org.springframework.beans.factory.ObjectProvider<NotificationUseCase> notifications) {
        this(notifications.getIfAvailable());
    }

    /** Construction directe (tests) ; {@code null} = service de notifications indisponible. */
    public NotificationsDevoirsService(NotificationUseCase notifications) {
        this.notifications = notifications;
    }

    /** Prévient chaque élève de la classe qu'un devoir a été publié. */
    public Map<String, Object> nouveauDevoir(List<String> eleves, String titreCours, String titreDevoir, String dateLimite) {
        String titre = "Nouveau devoir : " + titreDevoir;
        String message = "Un devoir a été publié" + (titreCours == null ? "" : " dans le cours « " + titreCours + " »")
                + (dateLimite == null ? "." : ". Date limite : " + dateLimite + ".");
        return envoyer(eleves, titre, message, "high");
    }

    /** Prévient l'élève que sa copie a été corrigée. */
    public Map<String, Object> correctionPubliee(String eleve, String titreDevoir, double note) {
        String titre = "Devoir corrigé : " + titreDevoir;
        String message = "Votre devoir « " + titreDevoir + " » a été corrigé. Note : " + formaterNote(note) + "/20.";
        return envoyer(List.of(eleve), titre, message, "normal");
    }

    static String formaterNote(double note) {
        return note == Math.rint(note) ? String.valueOf((long) note) : String.valueOf(note).replace('.', ',');
    }

    private Map<String, Object> envoyer(List<String> eleves, String titre, String message, String priorite) {
        Map<String, Object> r = new LinkedHashMap<>();
        int attendues = eleves == null ? 0 : eleves.size();
        int envoyees = 0;
        if (notifications == null) {
            r.put("attendues", attendues);
            r.put("envoyees", 0);
            r.put("echecs", attendues);
            r.put("message", "Le service de notifications est indisponible : les élèves n'ont pas été prévenus.");
            return r;
        }
        if (eleves != null) {
            for (String eleve : eleves) {
                try {
                    notifications.sendNotification(eleve, "ETUDIANT", titre, message, "academic", priorite);
                    envoyees++;
                } catch (RuntimeException e) {
                    log.warn("Notification de devoir non envoyée à {} : {}", eleve, e.getMessage());
                }
            }
        }
        int echecs = attendues - envoyees;
        r.put("attendues", attendues);
        r.put("envoyees", envoyees);
        r.put("echecs", echecs);
        String texte;
        if (attendues == 0) texte = "Aucun élève inscrit dans la classe : personne n'a été notifié.";
        else if (echecs == 0) texte = envoyees == 1 ? "L'élève a été notifié." : envoyees + " élèves ont été notifiés.";
        else if (envoyees == 0) texte = "La notification des élèves a échoué : personne n'a été prévenu.";
        else texte = envoyees + " élève(s) notifié(s) sur " + attendues + " : " + echecs + " notification(s) en échec.";
        r.put("message", texte);
        return r;
    }
}
