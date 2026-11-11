package ecole_221.sn.ecole.modules.parent.application.service;

import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.MessageParentJpaEntity;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.MessageParentJpaRepository;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

import static ecole_221.sn.ecole.modules.parent.infrastructure.persistence.MessageParentJpaEntity.ADMINISTRATION;

/**
 * Messagerie parent ↔ école. Un fil relie un parent à un interlocuteur : un professeur de l'école,
 * ou l'administration (boîte partagée par les administrateurs). Chacun ne voit que ses fils ;
 * toutes les requêtes sont limitées à l'école courante (@TenantId).
 * Remplace l'ancien envoi qui renvoyait un message inventé sans rien enregistrer (APP-TL-13).
 */
@Service
public class MessagerieParentsService {

    public static final int SUJET_MAX = 200;
    public static final int MESSAGE_MAX = 5000;

    private final MessageParentJpaRepository messages;
    private final UserJpaRepository comptes;
    private final OrganizationUserRepositoryPort appartenances;
    private final Clock horloge;

    @org.springframework.beans.factory.annotation.Autowired
    public MessagerieParentsService(MessageParentJpaRepository messages, UserJpaRepository comptes,
                                    OrganizationUserRepositoryPort appartenances) {
        this(messages, comptes, appartenances, Clock.systemDefaultZone());
    }

    MessagerieParentsService(MessageParentJpaRepository messages, UserJpaRepository comptes,
                             OrganizationUserRepositoryPort appartenances, Clock horloge) {
        this.messages = messages;
        this.comptes = comptes;
        this.appartenances = appartenances;
        this.horloge = horloge;
    }

    // ==================== Parent ====================

    /** Tous les messages du parent (envoyés et reçus), les plus récents d'abord. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> filDuParent(String parentId) {
        Noms noms = new Noms();
        return messages.findByParentIdOrderByEnvoyeLeDesc(parentId).stream().map(m -> vue(m, noms, true)).toList();
    }

    /** Destinataires possibles : l'administration, puis les professeurs actifs de l'école. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> destinataires() {
        List<Map<String, Object>> r = new ArrayList<>();
        r.add(Map.of("id", ADMINISTRATION, "nom", "Administration de l'école", "role", "ADMINISTRATION"));
        Noms noms = new Noms();
        for (String id : professeursActifs()) {
            String nom = noms.nom(id);
            if (nom != null) {
                r.add(Map.of("id", id, "nom", nom, "role", "PROFESSEUR"));
            }
        }
        return r;
    }

    @Transactional
    public Map<String, Object> envoyerParParent(String parentId, String destinataireBrut, String sujetBrut, String messageBrut) {
        String destinataire = destinataireBrut == null ? "" : destinataireBrut.trim();
        if (destinataire.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choisissez le destinataire : un professeur ou l'administration.");
        }
        if (!ADMINISTRATION.equals(destinataire) && !professeursActifs().contains(destinataire)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce destinataire n'est pas un professeur de l'école ; choisissez-en un dans la liste.");
        }
        String sujet = texte(sujetBrut, SUJET_MAX, "L'objet du message est obligatoire.", "L'objet");
        String contenu = texte(messageBrut, MESSAGE_MAX, "Le message est vide.", "Le message");
        MessageParentJpaEntity m = nouveau(parentId, destinataire, parentId, true, sujet, contenu, null);
        return vue(messages.save(m), new Noms(), true);
    }

    /** Le parent lit une réponse de l'école. Un message qui n'est pas dans ses fils → 404. */
    @Transactional
    public Map<String, Object> marquerLuParParent(String parentId, String messageId) {
        MessageParentJpaEntity m = trouver(messageId);
        if (!parentId.equals(m.getParentId())) {
            throw introuvable();
        }
        if (!m.isDeParent() && m.getLuLe() == null) {   // ses propres messages ne se « lisent » pas
            m.setLuLe(LocalDateTime.now(horloge));
            m = messages.save(m);
        }
        return vue(m, new Noms(), true);
    }

    // ==================== École : professeur ou administration ====================

    /** Boîte de l'interlocuteur (identifiant du professeur, ou ADMINISTRATION). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> boiteEcole(String interlocuteurId) {
        Noms noms = new Noms();
        return messages.findByInterlocuteurIdOrderByEnvoyeLeDesc(interlocuteurId).stream()
                .map(m -> vue(m, noms, false)).toList();
    }

    /** Réponse de l'école dans un fil existant ; le parent destinataire est celui du message d'origine. */
    @Transactional
    public Map<String, Object> repondre(String interlocuteurId, String auteurId, String messageId, String messageBrut) {
        MessageParentJpaEntity origine = trouver(messageId);
        if (!interlocuteurId.equals(origine.getInterlocuteurId())) {
            throw introuvable();
        }
        String contenu = texte(messageBrut, MESSAGE_MAX, "La réponse est vide.", "La réponse");
        String sujet = origine.getSujet().regionMatches(true, 0, "Re : ", 0, 5) ? origine.getSujet() : tronquer("Re : " + origine.getSujet());
        if (origine.isDeParent() && origine.getLuLe() == null) {   // répondre, c'est avoir lu
            origine.setLuLe(LocalDateTime.now(horloge));
            messages.save(origine);
        }
        MessageParentJpaEntity r = nouveau(origine.getParentId(), interlocuteurId, auteurId, false, sujet, contenu, origine.getId());
        return vue(messages.save(r), new Noms(), false);
    }

    @Transactional
    public Map<String, Object> marquerLuParEcole(String interlocuteurId, String messageId) {
        MessageParentJpaEntity m = trouver(messageId);
        if (!interlocuteurId.equals(m.getInterlocuteurId())) {
            throw introuvable();
        }
        if (m.isDeParent() && m.getLuLe() == null) {
            m.setLuLe(LocalDateTime.now(horloge));
            m = messages.save(m);
        }
        return vue(m, new Noms(), false);
    }

    // ==================== Outils ====================

    private Set<String> professeursActifs() {
        String ecole = TenantContext.getCurrentOrganizationId().orElse(null);
        Set<String> ids = new LinkedHashSet<>();
        if (ecole == null || appartenances == null) {
            return ids;
        }
        for (var a : appartenances.findByOrganizationIdAndRole(ecole, RoleUtilisateur.PROFESSEUR.value())) {
            if (a.isActive()) {
                ids.add(a.getUserId());
            }
        }
        return ids;
    }

    private MessageParentJpaEntity nouveau(String parentId, String interlocuteurId, String auteurId, boolean deParent,
                                           String sujet, String contenu, String repondA) {
        MessageParentJpaEntity m = new MessageParentJpaEntity();
        m.setId("msg-" + UUID.randomUUID());
        m.setParentId(parentId);
        m.setInterlocuteurId(interlocuteurId);
        m.setAuteurId(auteurId);
        m.setDeParent(deParent);
        m.setSujet(sujet);
        m.setContenu(contenu);
        m.setEnvoyeLe(LocalDateTime.now(horloge));
        m.setRepondA(repondA);
        return m;
    }

    private MessageParentJpaEntity trouver(String messageId) {
        return messages.findById(messageId == null ? "" : messageId.trim()).orElseThrow(MessagerieParentsService::introuvable);
    }

    private static ResponseStatusException introuvable() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Message introuvable dans vos échanges.");
    }

    private static String texte(String brut, int max, String siVide, String libelle) {
        String t = brut == null ? "" : brut.trim();
        if (t.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, siVide);
        }
        if (t.length() > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    libelle + " fait " + t.length() + " caractères ; " + max + " au plus.");
        }
        return t;
    }

    private static String tronquer(String s) {
        return s.length() <= SUJET_MAX ? s : s.substring(0, SUJET_MAX);
    }

    /**
     * Vue d'un message. Côté parent, « lu » signifie : réponse de l'école lue par le parent (ses propres
     * envois sont toujours « lus ») ; côté école, l'inverse. « luParDestinataire » dit si l'autre partie l'a lu.
     */
    private Map<String, Object> vue(MessageParentJpaEntity m, Noms noms, boolean pourParent) {
        boolean admin = ADMINISTRATION.equals(m.getInterlocuteurId());
        String interlocuteur = admin ? "Administration de l'école" : noms.nom(m.getInterlocuteurId());
        String parent = noms.nom(m.getParentId());
        String auteur = noms.nom(m.getAuteurId());
        boolean recu = pourParent != m.isDeParent();
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", m.getId());
        v.put("parentId", m.getParentId());
        v.put("parent", parent);
        v.put("interlocuteurId", m.getInterlocuteurId());
        v.put("interlocuteur", interlocuteur);
        v.put("envoye", !recu);
        v.put("de", m.isDeParent() ? parent : (admin && auteur != null ? "Administration — " + auteur : auteur));
        v.put("role", m.isDeParent() ? "PARENT" : (admin ? "ADMINISTRATION" : "PROFESSEUR"));
        v.put("sujet", m.getSujet());
        v.put("message", m.getContenu());
        v.put("date", m.getEnvoyeLe().toLocalDate().toString());
        v.put("envoyeLe", m.getEnvoyeLe().toString());
        v.put("lu", !recu || m.getLuLe() != null);
        v.put("luParDestinataire", m.getLuLe() != null);
        v.put("repondA", m.getRepondA());
        return v;
    }

    private class Noms {
        final Map<String, String> cache = new HashMap<>();

        String nom(String id) {
            return id == null ? null : cache.computeIfAbsent(id, k -> comptes.findById(k).map(MessagerieParentsService::nomComplet).orElse(null));
        }
    }

    private static String nomComplet(UserJpaEntity u) {
        String n = ((u.getPrenom() == null ? "" : u.getPrenom()) + " " + (u.getNom() == null ? "" : u.getNom())).trim();
        return n.isEmpty() ? null : n;
    }
}
