package ecole_221.sn.ecole.infrastructure.web;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Wrapper de requête HTTP qui permet de lire le corps plusieurs fois
 * Nécessaire pour le logging avant que le contrôleur ne lise le corps
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    /** Taille maximale d'un corps JSON / texte (les fichiers passent par multipart, limité à part). */
    public static final int LIMITE_OCTETS = 2 * 1024 * 1024;

    /** Corps au-delà de la limite : la requête est refusée en 413. */
    public static class CorpsTropVolumineuxException extends java.io.IOException {
        public CorpsTropVolumineuxException() {
            super("Corps de requête trop volumineux.");
        }
    }

    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        // Lecture bornée (2026-11-13) : avant, readAllBytes() chargeait en mémoire un corps de n'importe quelle taille.
        byte[] lu = request.getInputStream().readNBytes(LIMITE_OCTETS + 1);
        if (lu.length > LIMITE_OCTETS) {
            throw new CorpsTropVolumineuxException();
        }
        this.cachedBody = lu;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new CachedBodyServletInputStream(this.cachedBody);
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(
            new ByteArrayInputStream(this.cachedBody), StandardCharsets.UTF_8));
    }

    private static class CachedBodyServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream inputStream;

        public CachedBodyServletInputStream(byte[] cachedBody) {
            this.inputStream = new ByteArrayInputStream(cachedBody);
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }
    }
}