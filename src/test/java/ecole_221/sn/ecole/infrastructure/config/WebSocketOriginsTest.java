package ecole_221.sn.ecole.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * APP-SEC-01 : l'endpoint WebSocket /ws utilise exactement les mêmes origines que le CORS REST,
 * sans joker "*", et une origine non autorisée est refusée.
 */
class WebSocketOriginsTest {

    private List<String> originesWebSocket() {
        StompWebSocketEndpointRegistration registration =
                Mockito.mock(StompWebSocketEndpointRegistration.class, Mockito.RETURNS_SELF);
        StompEndpointRegistry registry = Mockito.mock(StompEndpointRegistry.class,
                inv -> "addEndpoint".equals(inv.getMethod().getName()) ? registration : null);

        new WebSocketConfig(null).registerStompEndpoints(registry);

        Invocation appel = Mockito.mockingDetails(registration).getInvocations().stream()
                .filter(i -> "setAllowedOriginPatterns".equals(i.getMethod().getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("setAllowedOriginPatterns non appelé sur /ws"));
        assertTrue(Mockito.mockingDetails(registration).getInvocations().stream()
                        .noneMatch(i -> "setAllowedOrigins".equals(i.getMethod().getName())),
                "setAllowedOrigins ne doit pas contourner la liste partagée");
        return Arrays.asList((String[]) appel.getRawArguments()[0]);
    }

    private CorsConfiguration corsRest() {
        CorsConfiguration config = new SecurityConfig(null, null)
                .corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest("GET", "/api/health"));
        assertNotNull(config);
        return config;
    }

    @Test
    void originesWebSocketIdentiquesAuCorsRest() {
        List<String> ws = originesWebSocket();
        assertEquals(corsRest().getAllowedOriginPatterns(), ws);
        assertEquals(AllowedOrigins.PATTERNS, ws);
    }

    @Test
    void aucunJokerGlobal() {
        assertFalse(originesWebSocket().contains("*"));
        assertFalse(AllowedOrigins.PATTERNS.contains("*"));
    }

    @Test
    void origineNonAutoriseeRefusee_origineAutoriseeAcceptee() {
        CorsConfiguration wsPolicy = new CorsConfiguration();
        wsPolicy.setAllowedOriginPatterns(originesWebSocket());

        assertNull(wsPolicy.checkOrigin("https://evil.example.com"));
        assertNull(wsPolicy.checkOrigin("https://papa-221.vercel.app.evil.com"));
        assertNull(wsPolicy.checkOrigin("http://papa-221.vercel.app")); // schéma différent
        assertEquals("https://papa-221.vercel.app", wsPolicy.checkOrigin("https://papa-221.vercel.app"));
        assertEquals("http://localhost:5173", wsPolicy.checkOrigin("http://localhost:5173"));
    }
}
