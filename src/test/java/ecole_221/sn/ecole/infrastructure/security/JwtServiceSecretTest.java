package ecole_221.sn.ecole.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * APP-SEC-02 : le bean JwtService (donc l'application) refuse de démarrer sans JWT_SECRET valide.
 * Les secrets ci-dessous sont des valeurs de TEST explicites, sans aucune valeur en production.
 */
class JwtServiceSecretTest {

    private static final String TEST_SECRET = "unit-test-only-secret-0123456789-abcdefghij";

    @Test
    void refuseDeDemarrerSiSecretAbsent() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new JwtService(null));
        assertTrue(ex.getMessage().contains("JWT_SECRET"));
        assertTrue(ex.getMessage().contains("absent"));
    }

    @Test
    void refuseDeDemarrerSiSecretVide() {
        // Cas réel : JWT_SECRET non défini => application.yaml résout jwt.secret à ""
        assertThrows(IllegalStateException.class, () -> new JwtService(""));
        assertThrows(IllegalStateException.class, () -> new JwtService("   "));
    }

    @Test
    void refuseDeDemarrerSiSecretTropCourt_sansDivulguerLeSecret() {
        String court = "trop-court-0123456789-abcdefgh"; // 30 caractères
        assertEquals(30, court.length());
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new JwtService(court));
        assertTrue(ex.getMessage().contains("trop court"));
        assertFalse(ex.getMessage().contains(court), "le message ne doit jamais contenir le secret");
    }

    @Test
    void accepteUnSecretDe32CaracteresEtSigneUnToken() {
        String secret32 = "x".repeat(JwtService.MIN_SECRET_LENGTH);
        assertDoesNotThrow(() -> new JwtService(secret32));

        JwtService service = new JwtService(TEST_SECRET);
        UserDetails user = new User("prof@ecole221.sn", "n/a", List.of());
        String token = service.generateToken(user);
        assertEquals("prof@ecole221.sn", service.extractUsername(token));
        assertTrue(service.isTokenValid(token, user));
    }

    @Test
    void unTokenSigneAvecUnAutreSecretEstRejete() {
        JwtService a = new JwtService(TEST_SECRET);
        JwtService b = new JwtService("another-unit-test-secret-9876543210-zyxwv");
        String token = a.generateToken(new User("u@ecole221.sn", "n/a", List.of()));
        assertThrows(io.jsonwebtoken.JwtException.class, () -> b.extractUsername(token));
    }

    @Test
    void ancienneValeurPubliqueRefuseeParEmpreinte() {
        // On reconstruit la valeur publique historique sans l'écrire d'un seul tenant dans le code.
        String publique = String.join("-", "ecole221", "super", "secret", "key", "must", "be", "at", "least", "32", "chars");
        assertThrows(IllegalStateException.class, () -> new JwtService(publique));
    }
}
