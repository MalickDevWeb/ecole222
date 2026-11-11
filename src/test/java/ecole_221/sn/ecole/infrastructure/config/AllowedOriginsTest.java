package ecole_221.sn.ecole.infrastructure.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** APP-SEC-04 : origines CORS configurables, sans IP locale codée en dur ni joker global. */
class AllowedOriginsTest {

    @Test
    void sansVariableValeurParDefaut() {
        assertEquals(AllowedOrigins.PAR_DEFAUT, AllowedOrigins.depuis(null));
        assertEquals(AllowedOrigins.PAR_DEFAUT, AllowedOrigins.depuis("  "));
        assertTrue(AllowedOrigins.PAR_DEFAUT.contains("https://papa-221.vercel.app"));
    }

    @Test
    void reseauLocalSeulementPourLeServeurDeDeveloppement() {
        org.springframework.web.cors.CorsConfiguration c = new org.springframework.web.cors.CorsConfiguration();
        c.setAllowedOriginPatterns(AllowedOrigins.PAR_DEFAUT);
        c.setAllowCredentials(true);
        assertNotNull(c.checkOrigin("http://192.168.1.239:5173"), "Vite ouvert depuis le réseau local");
        assertNotNull(c.checkOrigin("http://10.0.0.12:5173"));
        assertNull(c.checkOrigin("http://192.168.1.239:8080"), "autre port du réseau local refusé");
        assertNull(c.checkOrigin("https://evil.example"));
        assertNotNull(c.checkOrigin("https://papa-221.vercel.app"));
    }

    @Test
    void listeLueDepuisLaVariable() {
        assertEquals(List.of("https://a.example", "https://b.example"),
                AllowedOrigins.depuis(" https://a.example , https://b.example ,"));
    }

    @Test
    void jokerGlobalRefuse() {
        assertThrows(IllegalStateException.class, () -> AllowedOrigins.depuis("https://a.example,*"));
        assertThrows(IllegalStateException.class, () -> AllowedOrigins.depuis("https://*"));
    }
}
