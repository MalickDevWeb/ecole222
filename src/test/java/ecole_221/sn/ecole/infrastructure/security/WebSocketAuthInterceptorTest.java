package ecole_221.sn.ecole.infrastructure.security;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.security.Principal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** WebSocket : sans jeton valide, ni connexion, ni envoi, ni abonnement (APP-QA-02). */
class WebSocketAuthInterceptorTest {

    private WebSocketAuthInterceptor intercepteur;
    private final MessageChannel canal = mock(MessageChannel.class);

    @BeforeEach
    void preparer() {
        JwtService jwt = mock(JwtService.class);
        UserRepositoryPort utilisateurs = mock(UserRepositoryPort.class);
        User u = mock(User.class);
        when(jwt.extractUsername("bon-jeton")).thenReturn("eleve@exemple.sn");
        when(jwt.extractUsername("jeton-expire")).thenReturn("eleve@exemple.sn");
        when(jwt.extractUsername("falsifie")).thenThrow(new RuntimeException("signature invalide"));
        when(utilisateurs.findByEmail("eleve@exemple.sn")).thenReturn(Optional.of(u));
        when(jwt.isTokenValid(eq("bon-jeton"), any())).thenReturn(true);
        when(jwt.isTokenValid(eq("jeton-expire"), any())).thenReturn(false);
        intercepteur = new WebSocketAuthInterceptor(jwt, utilisateurs);
    }

    private static <T> T eq(T v) { return org.mockito.ArgumentMatchers.eq(v); }

    private static Message<byte[]> trame(StompCommand cmd, String authorization, Principal user) {
        StompHeaderAccessor h = StompHeaderAccessor.create(cmd);
        if (authorization != null) h.addNativeHeader("Authorization", authorization);
        if (user != null) h.setUser(user);
        h.setDestination(cmd == StompCommand.CONNECT ? null : "/topic/live-sessions/s1/chat");
        h.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], h.getMessageHeaders());
    }

    @Test
    void connexionAvecJetonValideAuthentifiee() {
        Message<?> m = intercepteur.preSend(trame(StompCommand.CONNECT, "Bearer bon-jeton", null), canal);
        assertThat(StompHeaderAccessor.wrap(m).getUser()).isNotNull();
    }

    @Test
    void connexionSansJetonOuJetonInvalideRefusee() {
        assertThatThrownBy(() -> intercepteur.preSend(trame(StompCommand.CONNECT, null, null), canal))
                .isInstanceOf(MessageDeliveryException.class).hasMessageContaining("jeton manquant");
        assertThatThrownBy(() -> intercepteur.preSend(trame(StompCommand.CONNECT, "Bearer jeton-expire", null), canal))
                .isInstanceOf(MessageDeliveryException.class).hasMessageContaining("invalide ou expiré");
        assertThatThrownBy(() -> intercepteur.preSend(trame(StompCommand.CONNECT, "Bearer falsifie", null), canal))
                .isInstanceOf(MessageDeliveryException.class);
        assertThatThrownBy(() -> intercepteur.preSend(trame(StompCommand.CONNECT, "Basic abc", null), canal))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void envoiEtAbonnementSansAuthentificationRefuses() {
        assertThatThrownBy(() -> intercepteur.preSend(trame(StompCommand.SEND, null, null), canal))
                .isInstanceOf(MessageDeliveryException.class).hasMessageContaining("non authentifiée");
        assertThatThrownBy(() -> intercepteur.preSend(trame(StompCommand.SUBSCRIBE, null, null), canal))
                .isInstanceOf(MessageDeliveryException.class);
        Principal p = () -> "eleve@exemple.sn";
        assertThat(intercepteur.preSend(trame(StompCommand.SUBSCRIBE, null, p), canal)).isNotNull();
    }
}
