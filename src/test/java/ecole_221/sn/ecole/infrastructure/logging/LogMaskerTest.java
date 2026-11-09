package ecole_221.sn.ecole.infrastructure.logging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** APP-SEC-13 : aucune donnée personnelle ni secret en clair dans les journaux. */
class LogMaskerTest {

    @Test
    void emailMasque() {
        String s = LogMasker.masquer("[CandidateDto[email=awa.ndiaye@test.sn, nom=Ndiaye]]");
        assertFalse(s.contains("awa.ndiaye@test.sn"));
        assertTrue(s.contains("[email]"));
        assertTrue(s.contains("Ndiaye"), "le reste du message est conservé");
    }

    @Test
    void telephonesSenegalaisMasques() {
        for (String tel : new String[]{"+221 77 123 45 67", "221789876543", "771234567", "33 821 00 00"}) {
            assertFalse(LogMasker.masquer("tel=" + tel + ",").contains(tel), tel);
        }
    }

    @Test
    void identifiantsNonPrisPourDesTelephones() {
        assertEquals("[CAND-2026-8033fc4f]", LogMasker.masquer("[CAND-2026-8033fc4f]"));
        assertEquals("duree=1234ms", LogMasker.masquer("duree=1234ms"));
    }

    @Test
    void jetonsEtMotsDePasseMasques() {
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4In0.abcDEF123_-x";
        String s = LogMasker.masquer("Authorization=Bearer " + jwt + ", password=Secret123, token: abc.def");
        assertFalse(s.contains(jwt));
        assertFalse(s.contains("Secret123"));
        assertFalse(s.contains("abc.def"));
    }

    @Test
    void corpsJsonDeConnexionMasque() {
        String s = LogMasker.masquer("{\"email\":\"superadmin@ecole221.sn\",\"password\":\"Tr3s-S3cret!\"}");
        assertFalse(s.contains("Tr3s-S3cret!"), s);
        assertFalse(s.contains("superadmin@ecole221.sn"), s);
    }

    @Test
    void motDePasseAvecEspacesEtGuillemetEchappe() {
        String s = LogMasker.masquer("{\"newPassword\":\"mon mot \\\"de\\\" passe\",\"x\":1}");
        assertFalse(s.contains("mot"), s);
        assertFalse(s.contains("passe\""), s);
        assertTrue(s.contains("\"x\":1"), s);
    }

    @Test
    void troncatureEtNull() {
        assertEquals("null", LogMasker.masquer(null));
        assertTrue(LogMasker.masquer("x".repeat(1000)).length() < 320);
    }
}
