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
    void plusAucuneIpDuReseauLocal() {
        assertTrue(AllowedOrigins.PAR_DEFAUT.stream().noneMatch(o -> o.contains("192.168.")));
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
