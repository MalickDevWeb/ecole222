package ecole_221.sn.ecole.infrastructure.security;

import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Authentification des connexions WebSocket / STOMP (APP-QA-02).
 * Avant : /ws était public et aucun contrôle n'était fait sur les trames : n'importe qui pouvait écouter ou
 * écrire dans le chat des cours en direct et injecter des signaux dans les sessions vocales.
 * <ul>
 *   <li>CONNECT : en-tête natif {@code Authorization: Bearer <jwt>} obligatoire, validé comme pour l'API REST ;</li>
 *   <li>SEND et SUBSCRIBE : refusés si la connexion n'est pas authentifiée.</li>
 * </ul>
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    /** Attribut de session STOMP : école de la connexion (vérifiée à CONNECT). */
    public static final String ECOLE = "ecole221.organizationId";

    private final JwtService jwtService;
    private final UserRepositoryPort utilisateurs;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort ecoles;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort appartenances;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.infrastructure.security.revocation.RevocationJetons revocation;

    public WebSocketAuthInterceptor(JwtService jwtService, UserRepositoryPort utilisateurs) {
        this.jwtService = jwtService;
        this.utilisateurs = utilisateurs;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor acces = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (acces == null || acces.getCommand() == null) {
            return message;
        }
        StompCommand commande = acces.getCommand();
        if (StompCommand.CONNECT.equals(commande) || StompCommand.STOMP.equals(commande)) {
            UsernamePasswordAuthenticationToken jeton = authentifier(acces.getFirstNativeHeader("Authorization"));
            acces.setUser(jeton);
            String ecole = ecoleDeLaConnexion(jeton, acces.getFirstNativeHeader("X-Tenant-Slug"));
            if (ecole != null && acces.getSessionAttributes() != null) {
                acces.getSessionAttributes().put(ECOLE, ecole);
            }
        } else if ((StompCommand.SEND.equals(commande) || StompCommand.SUBSCRIBE.equals(commande)) && acces.getUser() == null) {
            throw new MessageDeliveryException("Connexion WebSocket non authentifiée.");
        } else if (StompCommand.SUBSCRIBE.equals(commande)) {
            verifierAbonnement(acces.getDestination(), acces.getSessionAttributes() == null ? null
                    : (String) acces.getSessionAttributes().get(ECOLE));
        } else if (StompCommand.SEND.equals(commande)) {
            verifierEnvoi(acces.getDestination());
        }
        return message;
    }

    /**
     * Abonnements cloisonnés par école (2026-11-13). Avant : n'importe quel compte connecté, de n'importe quelle école,
     * pouvait s'abonner au chat d'un cours en direct d'une autre école ({@code /topic/live-sessions/{id}/chat}).
     * Désormais les cours en direct sont diffusés sur {@code /topic/ecole/{école}/…} et seule sa propre école est
     * accessible ; l'ancien canal non cloisonné est refusé.
     *
     * <p>Révision du 2026-11-13 (balayage des écrans) : liste blanche. Seuls les canaux {@code /topic/ecole/{école}/…}
     * sont ouverts ; tout autre canal ({@code /topic/collaboration}, {@code /topic/voice/…}) est refusé, car il serait
     * partagé entre toutes les écoles.</p>
     */
    static void verifierAbonnement(String destination, String ecole) {
        if (destination == null || destination.isBlank()) {
            throw new MessageDeliveryException("Canal d'abonnement manquant.");
        }
        if (destination.startsWith("/topic/live-sessions/")) {
            throw new MessageDeliveryException("Canal non cloisonné : abonnez-vous à /topic/ecole/{école}/live-sessions/…");
        }
        if (!destination.startsWith("/topic/ecole/")) {
            throw new MessageDeliveryException("Canal inconnu ou non cloisonné par école : " + destination);
        }
        String[] parties = destination.split("/");   // ["", "topic", "ecole", "{école}", …]
        if (ecole == null || parties.length < 5 || !ecole.equals(parties[3])) {
            throw new MessageDeliveryException("Accès refusé : ce canal appartient à une autre école.");
        }
    }

    /**
     * Envois autorisés (2026-11-13) : le chat et les réactions des cours en direct, seuls traités avec l'école de la
     * connexion. Le relais vocal ({@code /app/voice/…}, sans écran ni cloisonnement) et toute autre destination sont
     * refusés.
     */
    static void verifierEnvoi(String destination) {
        if (destination == null || !destination.startsWith("/app/live-sessions/")) {
            throw new MessageDeliveryException("Envoi refusé : destination inconnue ou non autorisée.");
        }
    }

    /** École indiquée par X-Tenant-Slug (si le compte en est membre actif), sinon l'unique école active du compte. */
    String ecoleDeLaConnexion(UsernamePasswordAuthenticationToken jeton, String slug) {
        if (appartenances == null || !(jeton.getPrincipal() instanceof ecole_221.sn.ecole.modules.auth.domain.model.User u)) {
            return null;
        }
        java.util.List<String> actives = appartenances.findByUserId(u.getId()).stream()
                .filter(ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUser::isActive)
                .map(ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUser::getOrganizationId)
                .toList();
        if (slug != null && !slug.isBlank() && ecoles != null) {
            return ecoles.findBySlug(slug.strip()).map(o -> o.getId()).filter(actives::contains).orElse(null);
        }
        return actives.size() == 1 ? actives.get(0) : null;
    }

    UsernamePasswordAuthenticationToken authentifier(String entete) {
        if (entete == null || !entete.startsWith("Bearer ")) {
            throw new MessageDeliveryException("Authentification requise : jeton manquant.");
        }
        String jwt = entete.substring(7).trim();
        try {
            String email = jwtService.extractUsername(jwt);
            UserDetails u = email == null ? null : utilisateurs.findByEmail(email).orElse(null);
            if (u != null && jwtService.isTokenValid(jwt, u)
                    && (revocation == null || !revocation.estRefuse(jwt,
                            u instanceof ecole_221.sn.ecole.modules.auth.domain.model.User user ? user.getId() : null,
                            jwtService.extractIssuedAt(jwt)))) {
                return new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities());
            }
        } catch (RuntimeException e) {
            // jeton illisible, falsifié ou expiré : même refus
        }
        throw new MessageDeliveryException("Authentification requise : jeton invalide ou expiré.");
    }
}
