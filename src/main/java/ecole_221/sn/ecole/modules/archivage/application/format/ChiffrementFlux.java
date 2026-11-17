package ecole_221.sn.ecole.modules.archivage.application.format;

import ecole_221.sn.ecole.modules.archivage.domain.exception.ArchivageException;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Chiffrement des archives : AES-256-GCM (javax.crypto), en flux et par segments d'1 Mio.
 *
 * <p>Pourquoi des segments : GCM ne livre le clair qu'après avoir vérifié l'étiquette ; déchiffrer d'un seul bloc une
 * archive de plusieurs centaines de Mo la chargerait entière en mémoire (serveur Render à mémoire limitée). Chaque
 * segment est chiffré à part (nonce = préfixe aléatoire de 7 octets, numéro du segment sur 4 octets, drapeau « dernier
 * segment » sur 1 octet ; l'en-tête est authentifié avec chaque segment) : un segment modifié, déplacé, retiré ou une
 * archive tronquée sont détectés.</p>
 *
 * <p>Clés : chaque archive a sa propre clé de données (32 octets aléatoires), enveloppée (AES-GCM) par la clé maîtresse
 * et rangée en base, jamais avec le fichier. Effacer la clé enveloppée rend l'archive illisible pour toujours.</p>
 */
public final class ChiffrementFlux {

    static final byte[] MAGIQUE = "E221ARC1".getBytes(StandardCharsets.US_ASCII);
    static final byte VERSION = 1;
    static final int SEGMENT = 1 << 20;
    static final int ETIQUETTE = 16;
    static final int PREFIXE = 7;
    static final int TAILLE_ENTETE = MAGIQUE.length + 1 + 4 + PREFIXE;
    private static final SecureRandom HASARD = new SecureRandom();
    private static final String ALGO = "AES/GCM/NoPadding";

    private ChiffrementFlux() {
    }

    public static byte[] nouvelleCle() {
        byte[] cle = new byte[32];
        HASARD.nextBytes(cle);
        return cle;
    }

    /** Empreinte publique d'une clé maîtresse (16 caractères hexadécimaux) : sert à reconnaître la bonne clé. */
    public static String identifiant(byte[] cleMaitresse) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update("ecole221-archives-cle:".getBytes(StandardCharsets.US_ASCII));
            md.update(cleMaitresse);
            return HexFormat.of().formatHex(md.digest()).substring(0, 16);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Clé de données chiffrée par la clé maîtresse, liée à l'archive (donnée associée) ; base64. */
    public static String envelopper(byte[] cleMaitresse, byte[] cleDonnees, String archivageId) {
        try {
            byte[] nonce = new byte[12];
            HASARD.nextBytes(nonce);
            Cipher c = Cipher.getInstance(ALGO);
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cleMaitresse, "AES"), new GCMParameterSpec(128, nonce));
            c.updateAAD(archivageId.getBytes(StandardCharsets.UTF_8));
            byte[] chiffre = c.doFinal(cleDonnees);
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(12 + chiffre.length).put(nonce).put(chiffre).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Chiffrement de la clé de l'archive impossible", e);
        }
    }

    public static byte[] desenvelopper(byte[] cleMaitresse, String enveloppe, String archivageId) {
        try {
            byte[] brut = Base64.getDecoder().decode(enveloppe);
            Cipher c = Cipher.getInstance(ALGO);
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cleMaitresse, "AES"), new GCMParameterSpec(128, brut, 0, 12));
            c.updateAAD(archivageId.getBytes(StandardCharsets.UTF_8));
            return c.doFinal(brut, 12, brut.length - 12);
        } catch (AEADBadTagException | IllegalArgumentException e) {
            throw ArchivageException.refus("Clé de l'archive illisible : la clé des archives (ARCHIVES_CLE) n'est pas celle de l'archivage.");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Déchiffrement de la clé de l'archive impossible", e);
        }
    }

    public static OutputStream chiffrer(OutputStream sortie, byte[] cleDonnees) throws IOException {
        byte[] prefixe = new byte[PREFIXE];
        HASARD.nextBytes(prefixe);
        byte[] entete = ByteBuffer.allocate(TAILLE_ENTETE).put(MAGIQUE).put(VERSION).putInt(SEGMENT).put(prefixe).array();
        sortie.write(entete);
        return new FluxChiffrant(sortie, cleDonnees, entete, prefixe);
    }

    public static InputStream dechiffrer(InputStream entree, byte[] cleDonnees) throws IOException {
        byte[] entete = entree.readNBytes(TAILLE_ENTETE);
        if (entete.length < TAILLE_ENTETE || !Arrays.equals(Arrays.copyOf(entete, MAGIQUE.length), MAGIQUE)) {
            throw ArchivageException.refus("Fichier d'archive illisible : ce n'est pas une archive d'école.");
        }
        ByteBuffer b = ByteBuffer.wrap(entete, MAGIQUE.length, TAILLE_ENTETE - MAGIQUE.length);
        byte version = b.get();
        int segment = b.getInt();
        if (version != VERSION || segment <= 0 || segment > (64 << 20)) {
            throw ArchivageException.refus("Fichier d'archive illisible : version de format inconnue.");
        }
        byte[] prefixe = new byte[PREFIXE];
        b.get(prefixe);
        return new FluxDechiffrant(new PushbackInputStream(entree, 1), cleDonnees, entete, prefixe, segment);
    }

    static byte[] nonce(byte[] prefixe, int numero, boolean dernier) {
        return ByteBuffer.allocate(12).put(prefixe).putInt(numero).put((byte) (dernier ? 1 : 0)).array();
    }

    private static final class FluxChiffrant extends OutputStream {
        private final OutputStream sortie;
        private final SecretKeySpec cle;
        private final byte[] entete;
        private final byte[] prefixe;
        private final byte[] tampon = new byte[SEGMENT];
        private int rempli;
        private int numero;
        private boolean ferme;

        FluxChiffrant(OutputStream sortie, byte[] cleDonnees, byte[] entete, byte[] prefixe) {
            this.sortie = sortie;
            this.cle = new SecretKeySpec(cleDonnees, "AES");
            this.entete = entete;
            this.prefixe = prefixe;
        }

        @Override
        public void write(int b) throws IOException {
            write(new byte[]{(byte) b}, 0, 1);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (ferme) throw new IOException("Flux d'archive déjà fermé");
            while (len > 0) {
                if (rempli == SEGMENT) {
                    emettre(false);   // d'autres octets arrivent : ce segment n'est pas le dernier
                }
                int n = Math.min(len, SEGMENT - rempli);
                System.arraycopy(b, off, tampon, rempli, n);
                rempli += n;
                off += n;
                len -= n;
            }
        }

        private void emettre(boolean dernier) throws IOException {
            try {
                Cipher c = Cipher.getInstance(ALGO);
                c.init(Cipher.ENCRYPT_MODE, cle, new GCMParameterSpec(128, nonce(prefixe, numero, dernier)));
                c.updateAAD(entete);
                sortie.write(c.doFinal(tampon, 0, rempli));
            } catch (GeneralSecurityException e) {
                throw new IOException("Chiffrement de l'archive impossible", e);
            }
            numero++;
            rempli = 0;
        }

        @Override
        public void flush() throws IOException {
            sortie.flush();
        }

        @Override
        public void close() throws IOException {
            if (ferme) return;
            ferme = true;
            try {
                emettre(true);
            } finally {
                Arrays.fill(tampon, (byte) 0);
                sortie.close();
            }
        }
    }

    private static final class FluxDechiffrant extends InputStream {
        private final PushbackInputStream entree;
        private final SecretKeySpec cle;
        private final byte[] entete;
        private final byte[] prefixe;
        private final int segment;
        private byte[] clair = new byte[0];
        private int position;
        private int numero;
        private boolean fini;

        FluxDechiffrant(PushbackInputStream entree, byte[] cleDonnees, byte[] entete, byte[] prefixe, int segment) {
            this.entree = entree;
            this.cle = new SecretKeySpec(cleDonnees, "AES");
            this.entete = entete;
            this.prefixe = prefixe;
            this.segment = segment;
        }

        private boolean charger() throws IOException {
            while (position >= clair.length) {
                if (fini) return false;
                byte[] chiffre = entree.readNBytes(segment + ETIQUETTE);
                if (chiffre.length < ETIQUETTE) {
                    throw ArchivageException.refus("Fichier d'archive tronqué.");
                }
                int suivant = entree.read();
                boolean dernier = suivant < 0;
                if (!dernier) {
                    if (chiffre.length < segment + ETIQUETTE) {
                        throw ArchivageException.refus("Fichier d'archive altéré.");
                    }
                    entree.unread(suivant);
                }
                try {
                    Cipher c = Cipher.getInstance(ALGO);
                    c.init(Cipher.DECRYPT_MODE, cle, new GCMParameterSpec(128, nonce(prefixe, numero, dernier)));
                    c.updateAAD(entete);
                    clair = c.doFinal(chiffre);
                } catch (AEADBadTagException e) {
                    throw ArchivageException.refus("Archive illisible : fichier altéré ou mauvaise clé.");
                } catch (GeneralSecurityException e) {
                    throw new IOException("Déchiffrement de l'archive impossible", e);
                }
                position = 0;
                numero++;
                fini = dernier;
            }
            return true;
        }

        @Override
        public int read() throws IOException {
            if (!charger()) return -1;
            return clair[position++] & 0xff;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (len == 0) return 0;
            if (!charger()) return -1;
            int n = Math.min(len, clair.length - position);
            System.arraycopy(clair, position, b, off, n);
            position += n;
            return n;
        }

        @Override
        public void close() throws IOException {
            entree.close();
        }
    }

    /** Utilitaire de test : chiffre un tableau entier (mêmes règles que le flux). */
    static byte[] chiffrerTout(byte[] clair, byte[] cle) throws IOException {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try (OutputStream o = chiffrer(sortie, cle)) {
            o.write(clair);
        }
        return sortie.toByteArray();
    }
}
