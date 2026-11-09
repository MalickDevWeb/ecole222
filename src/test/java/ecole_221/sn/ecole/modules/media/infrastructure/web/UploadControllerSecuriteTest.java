package ecole_221.sn.ecole.modules.media.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/** APP-SEC-15 : envoi public de fichiers — signature réelle et débit limité. */
class UploadControllerSecuriteTest {

    private static byte[] avec(int... debut) {
        byte[] b = new byte[16];
        for (int i = 0; i < debut.length; i++) {
            b[i] = (byte) debut[i];
        }
        return b;
    }

    @Test
    void signaturesReellesAcceptees() {
        assertTrue(UploadController.signatureConforme(avec(0x25, 0x50, 0x44, 0x46), "application/pdf"));
        assertTrue(UploadController.signatureConforme(avec(0x89, 0x50, 0x4E, 0x47), "image/png"));
        assertTrue(UploadController.signatureConforme(avec(0xFF, 0xD8, 0xFF), "image/jpeg"));
        assertTrue(UploadController.signatureConforme(
                avec(0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50), "image/webp"));
    }

    @Test
    void contenuDeguiseRefuse() {
        byte[] html = "<html><script>alert(1)</script>".getBytes();
        assertFalse(UploadController.signatureConforme(html, "application/pdf"));
        assertFalse(UploadController.signatureConforme(html, "image/png"));
        assertFalse(UploadController.signatureConforme(new byte[3], "image/jpeg"), "fichier trop court");
    }

    @Test
    void limiteurParAdresse() {
        var limiteur = new UploadController.LimiteurEnvois(3, Duration.ofHours(1));
        assertTrue(limiteur.autoriser("a"));
        assertTrue(limiteur.autoriser("a"));
        assertTrue(limiteur.autoriser("a"));
        assertFalse(limiteur.autoriser("a"), "4e envoi dans l'heure refusé");
        assertTrue(limiteur.autoriser("b"), "autre adresse non affectée");
    }

    @Test
    void adresseDuClientDerriereLeProxy() {
        MockHttpServletRequest r = new MockHttpServletRequest();
        r.setRemoteAddr("10.0.0.1");
        r.addHeader("X-Forwarded-For", "196.1.2.3, 10.0.0.1");
        assertEquals("196.1.2.3", UploadController.adresse(r));
        MockHttpServletRequest direct = new MockHttpServletRequest();
        direct.setRemoteAddr("127.0.0.1");
        assertEquals("127.0.0.1", UploadController.adresse(direct));
    }
}
