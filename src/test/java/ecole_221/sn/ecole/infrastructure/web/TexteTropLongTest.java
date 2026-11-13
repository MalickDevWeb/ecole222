package ecole_221.sn.ecole.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class TexteTropLongTest {

    private static boolean tropLong(String json, int max) throws Exception {
        MockHttpServletRequest r = new MockHttpServletRequest("POST", "/x");
        r.setContent(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return new CachedBodyHttpServletRequest(r).contientTexteTropLong(max);
    }

    @Test
    void detecteUneValeurDemesureeSansFauxPositif() throws Exception {
        assertThat(tropLong("{\"nom\":\"" + "A".repeat(60_000) + "\"}", 50_000)).isTrue();
        assertThat(tropLong("{\"nom\":\"Awa\",\"texte\":\"" + "B".repeat(49_000) + "\"}", 50_000)).isFalse();
        assertThat(tropLong("{\"a\":\"un \\\"guillemet\\\" échappé\",\"b\":\"court\"}", 25)).isFalse();
        assertThat(tropLong("[" + "\"x\",".repeat(30_000) + "\"y\"]", 50_000)).isFalse();
    }
}
