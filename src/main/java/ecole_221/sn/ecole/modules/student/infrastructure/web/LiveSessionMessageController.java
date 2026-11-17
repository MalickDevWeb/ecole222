package ecole_221.sn.ecole.modules.student.infrastructure.web;

import ecole_221.sn.ecole.infrastructure.security.WebSocketAuthInterceptor;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.LiveChatMessageJpaEntity;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.LiveChatMessageJpaRepository;
import ecole_221.sn.ecole.partage.TexteNormalise;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Chat et réactions des cours en direct par WebSocket (STOMP).
 *
 * <p>Auteur = compte de la connexion, école = celle vérifiée à la connexion (WebSocketAuthInterceptor), texte
 * obligatoire et borné, diffusion sur un canal propre à l'école ({@code /topic/ecole/{école}/live-sessions/{id}/…}).</p>
 */
@RestController
public class LiveSessionMessageController {

    private static final Logger log = LoggerFactory.getLogger(LiveSessionMessageController.class);
    static final int TEXTE_MAX = 2_000;
    private static final Pattern SESSION = Pattern.compile("^[A-Za-z0-9_-]{1,100}$");
    private static final Set<String> REACTIONS = Set.of("like", "clap", "heart", "question", "laugh");

    private final LiveChatMessageJpaRepository messageRepository;
    private final SimpMessagingTemplate diffusion;
    private final OrganizationRepositoryPort ecoles;
    private final ecole_221.sn.ecole.modules.audit.infrastructure.persistence.JournalAudit journal;

    public LiveSessionMessageController(LiveChatMessageJpaRepository messageRepository, SimpMessagingTemplate diffusion,
                                        OrganizationRepositoryPort ecoles,
                                        ecole_221.sn.ecole.modules.audit.infrastructure.persistence.JournalAudit journal) {
        this.messageRepository = messageRepository;
        this.diffusion = diffusion;
        this.ecoles = ecoles;
        this.journal = journal;
    }

    /** Canal de diffusion propre à l'école. */
    static String canal(String ecole, String sessionId, String type) {
        return "/topic/ecole/" + ecole + "/live-sessions/" + sessionId + "/" + type;
    }

    /** Message de chat (client : /app/live-sessions/{sessionId}/chat, corps {text}) : enregistré puis diffusé à l'école. */
    @MessageMapping("/live-sessions/{sessionId}/chat")
    public void handleChatMessage(@DestinationVariable String sessionId, @Payload Map<String, Object> message,
                                  SimpMessageHeaderAccessor entetes) {
        User auteur = auteur(entetes);
        String ecole = ecole(entetes);
        verifierSession(sessionId);
        Object brut = message == null ? null : (message.get("text") != null ? message.get("text") : message.get("content"));
        String texte = brut instanceof String s ? TexteNormalise.normaliser(s) : null;
        if (texte == null || texte.isEmpty()) {
            throw new MessageDeliveryException("Le message est vide.");
        }
        if (texte.length() > TEXTE_MAX) {
            throw new MessageDeliveryException("Le message ne doit pas dépasser " + TEXTE_MAX + " caractères.");
        }
        String nom = ((auteur.getPrenom() == null ? "" : auteur.getPrenom()) + " " + (auteur.getNom() == null ? "" : auteur.getNom())).trim();
        String role = auteur.getRole() == null ? null : auteur.getRole().name();
        LiveChatMessageJpaEntity entite = new LiveChatMessageJpaEntity(UUID.randomUUID().toString(), sessionId, auteur.getId(),
                nom.isEmpty() ? auteur.getEmail() : nom, role, texte);
        dansLEcole(ecole, () -> messageRepository.save(entite));
        // ENF-07 : message reçu par WebSocket, hors de l'intercepteur HTTP → inscrit explicitement
        journal.inscrire(new ecole_221.sn.ecole.modules.audit.infrastructure.persistence.JournalAudit.Entree(
                ecole, auteur.getId(), entite.getUserName(), role, "CREATE", "live-sessions/chat", sessionId,
                "Création — message du cours en direct (WebSocket /app/live-sessions/{sessionId}/chat)", null, null, "live-sessions"));

        Map<String, Object> diffuse = new LinkedHashMap<>();
        diffuse.put("id", entite.getId());
        diffuse.put("sessionId", sessionId);
        diffuse.put("userId", entite.getUserId());
        diffuse.put("user", entite.getUserName());
        diffuse.put("userRole", role);
        diffuse.put("text", texte);
        diffuse.put("timestamp", java.time.Instant.now().toString());
        diffusion.convertAndSend(canal(ecole, sessionId, "chat"), (Object) diffuse);
    }

    /** Réaction (like, clap…) : diffusée à l'école, non enregistrée ; type limité à une liste fermée. */
    @MessageMapping("/live-sessions/{sessionId}/reaction")
    public void handleReaction(@DestinationVariable String sessionId, @Payload Map<String, Object> reaction,
                               SimpMessageHeaderAccessor entetes) {
        User auteur = auteur(entetes);
        String ecole = ecole(entetes);
        verifierSession(sessionId);
        Object type = reaction == null ? null : reaction.get("type");
        if (!(type instanceof String t) || !REACTIONS.contains(t)) {
            throw new MessageDeliveryException("Réaction inconnue : " + String.join(", ", REACTIONS) + ".");
        }
        Map<String, Object> diffuse = new LinkedHashMap<>();
        diffuse.put("type", type);
        diffuse.put("userId", auteur.getId());
        diffuse.put("timestamp", java.time.Instant.now().toString());
        diffusion.convertAndSend(canal(ecole, sessionId, "reaction"), (Object) diffuse);
    }

    /**
     * Endpoint REST pour récupérer l'historique du chat lorsqu'un étudiant rejoint la session en cours.
     */
    @Operation(summary = "Historique du chat d'un cours en direct", description = "Messages enregistrés de la session, pour l'élève qui la rejoint en cours de route.")
    @GetMapping("/student/live-sessions/{sessionId}/chat-history")
    public List<LiveChatMessageJpaEntity> getChatHistory(@PathVariable String sessionId) {
        return messageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
    }

    // ------------------------------------------------------------------ outils

    private static User auteur(SimpMessageHeaderAccessor entetes) {
        if (entetes.getUser() instanceof UsernamePasswordAuthenticationToken t && t.getPrincipal() instanceof User u) {
            return u;
        }
        throw new MessageDeliveryException("Connexion WebSocket non authentifiée.");
    }

    private static String ecole(SimpMessageHeaderAccessor entetes) {
        Object e = entetes.getSessionAttributes() == null ? null : entetes.getSessionAttributes().get(WebSocketAuthInterceptor.ECOLE);
        if (!(e instanceof String s) || s.isBlank()) {
            throw new MessageDeliveryException("École inconnue pour cette connexion : précisez l'en-tête X-Tenant-Slug à la connexion.");
        }
        return s;
    }

    private static void verifierSession(String sessionId) {
        if (sessionId == null || !SESSION.matcher(sessionId).matches()) {
            throw new MessageDeliveryException("Identifiant de session invalide.");
        }
    }

    /** Exécute l'enregistrement dans le contexte de l'école (le fil WebSocket n'en a pas). */
    private void dansLEcole(String ecole, Runnable action) {
        var precedente = TenantContext.getCurrentOrganization().orElse(null);
        var org = ecoles.findById(ecole).orElseThrow(() -> new MessageDeliveryException("École introuvable."));
        try {
            TenantContext.setCurrentOrganization(org);
            action.run();
        } catch (RuntimeException ex) {
            log.warn("Message de chat non enregistré : {}", ex.getMessage());
            throw new MessageDeliveryException("Le message n'a pas pu être enregistré.");
        } finally {
            TenantContext.setCurrentOrganization(precedente);
        }
    }
}
