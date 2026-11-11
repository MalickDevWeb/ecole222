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

    private final JwtService jwtService;
    private final UserRepositoryPort utilisateurs;

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
            acces.setUser(authentifier(acces.getFirstNativeHeader("Authorization")));
        } else if ((StompCommand.SEND.equals(commande) || StompCommand.SUBSCRIBE.equals(commande)) && acces.getUser() == null) {
            throw new MessageDeliveryException("Connexion WebSocket non authentifiée.");
        }
        return message;
    }

    UsernamePasswordAuthenticationToken authentifier(String entete) {
        if (entete == null || !entete.startsWith("Bearer ")) {
            throw new MessageDeliveryException("Authentification requise : jeton manquant.");
        }
        String jwt = entete.substring(7).trim();
        try {
            String email = jwtService.extractUsername(jwt);
            UserDetails u = email == null ? null : utilisateurs.findByEmail(email).orElse(null);
            if (u != null && jwtService.isTokenValid(jwt, u)) {
                return new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities());
            }
        } catch (RuntimeException e) {
            // jeton illisible, falsifié ou expiré : même refus
        }
        throw new MessageDeliveryException("Authentification requise : jeton invalide ou expiré.");
    }
}
