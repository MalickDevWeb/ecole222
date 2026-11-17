package ecole_221.sn.ecole.partage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.NamedParameterSpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/**
 * Jeton du badge QR dynamique des élèves.
 *
 * <p>Signature <b>Ed25519</b> (décision d'accès hors ligne, EF-10). Le serveur signe avec sa clé PRIVÉE ;
 * le téléphone du vigile vérifie avec la clé PUBLIQUE ({@link #clePubliqueBase64()}), qui permet de vérifier un badge
 * mais pas d'en fabriquer. La clé est dérivée du secret du serveur (ECOLE_BADGE_SECRET, sinon JWT_SECRET) : identique
 * à chaque redémarrage et sur chaque instance ; sans secret configuré, une clé aléatoire est tirée au démarrage.</p>
 *
 * <p>Contenu du QR : {@code STU_SECURE:{matricule}:{minute}:{signature base64url}}, valable pour la minute indiquée
 * ± 1 minute.</p>
 */
@Component
public class JetonBadge {

    public static final String PREFIXE = "STU_SECURE:";
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder D64 = Base64.getUrlDecoder();

    private final PrivateKey privee;
    private final PublicKey publique;

    public JetonBadge(@Value("${ecole.badge.secret:}") String secretBadge, @Value("${jwt.secret:}") String secretJwt) {
        KeyPair paire = paire(graine(secretBadge, secretJwt));
        this.privee = paire.getPrivate();
        this.publique = paire.getPublic();
    }

    /** Clé aléatoire (tests, ou serveur sans secret configuré : les badges ne survivent pas à un redémarrage). */
    public static JetonBadge aleatoire() {
        return new JetonBadge("", "");
    }

    private static byte[] graine(String secretBadge, String secretJwt) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            if (secretBadge != null && !secretBadge.isBlank()) {
                return sha.digest(("badge-ed25519:" + secretBadge).getBytes(StandardCharsets.UTF_8));
            }
            if (secretJwt != null && !secretJwt.isBlank()) {
                return sha.digest(("badge-qr-ed25519-ecole221:" + secretJwt).getBytes(StandardCharsets.UTF_8));
            }
            byte[] g = new byte[32];
            new SecureRandom().nextBytes(g);
            return g;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    /** Paire Ed25519 déterministe : le générateur reçoit exactement la graine de 32 octets. */
    private static KeyPair paire(byte[] graine) {
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("Ed25519");
            g.initialize(NamedParameterSpec.ED25519, new SecureRandom() {
                @Override
                public void nextBytes(byte[] octets) {
                    Arrays.fill(octets, (byte) 0);
                    System.arraycopy(graine, 0, octets, 0, Math.min(graine.length, octets.length));
                }
            });
            return g.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Ed25519 indisponible", e);
        }
    }

    /** Clé publique brute (32 octets) en base64url : diffusée aux vigiles pour la vérification hors ligne. */
    public String clePubliqueBase64() {
        byte[] spki = publique.getEncoded();   // SubjectPublicKeyInfo : 12 octets d'en-tête + 32 octets de clé
        return B64.encodeToString(Arrays.copyOfRange(spki, spki.length - 32, spki.length));
    }

    private static byte[] message(String matricule, long minute) {
        return (matricule + ":" + minute).getBytes(StandardCharsets.UTF_8);
    }

    /** Signature du matricule pour la minute donnée (minutes depuis l'époque Unix), en base64url. */
    public String jeton(String matricule, long minute) {
        try {
            Signature s = Signature.getInstance("Ed25519");
            s.initSign(privee);
            s.update(message(matricule, minute));
            return B64.encodeToString(s.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Signature Ed25519 impossible", e);
        }
    }

    public static long minuteCourante() {
        return System.currentTimeMillis() / 60_000L;
    }

    /** Contenu du QR : STU_SECURE:{matricule}:{minute}:{signature}. */
    public String contenu(String matricule, long minute) {
        return PREFIXE + matricule + ":" + minute + ":" + jeton(matricule, minute);
    }

    /** Signature authentique pour ce matricule et cette minute (sans contrôle de fraîcheur). */
    public boolean signatureValide(String matricule, long minute, String jeton) {
        if (matricule == null || matricule.isBlank() || jeton == null || jeton.length() > 200) return false;
        try {
            byte[] sig = D64.decode(jeton);
            // longueur exacte et encodage canonique (refuse une autre écriture de la même signature)
            if (sig.length != 64 || !B64.encodeToString(sig).equals(jeton)) return false;
            Signature s = Signature.getInstance("Ed25519");
            s.initVerify(publique);
            s.update(message(matricule, minute));
            return s.verify(sig);
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            return false;
        }
    }

    /** Vrai si la signature est authentique ET si la minute est à ±1 de maintenant. */
    public boolean valide(String matricule, long minute, String jeton) {
        return Math.abs(minuteCourante() - minute) <= 1 && signatureValide(matricule, minute, jeton);
    }

    /** Lit un contenu scanné. Vide si ce n'est pas un badge dynamique ; sinon le matricule et la validité. */
    public Optional<Lecture> lire(String contenu) {
        return lire(contenu, null);
    }

    /**
     * Comme {@link #lire(String)}, mais la fraîcheur est jugée par rapport à {@code scanneLe} (minute du scan
     * effectué hors ligne) au lieu de maintenant.
     */
    public Optional<Lecture> lire(String contenu, Long minuteDuScan) {
        if (contenu == null || !contenu.startsWith(PREFIXE)) {
            return Optional.empty();
        }
        String[] p = contenu.split(":");
        if (p.length != 4) {
            return Optional.of(new Lecture(null, false));
        }
        try {
            long minute = Long.parseLong(p[2]);
            long reference = minuteDuScan == null ? minuteCourante() : minuteDuScan;
            boolean frais = Math.abs(reference - minute) <= 1;
            return Optional.of(new Lecture(p[1], frais && signatureValide(p[1], minute, p[3])));
        } catch (NumberFormatException e) {
            return Optional.of(new Lecture(p[1], false));
        }
    }

    /** Résultat de la lecture d'un badge dynamique. */
    public record Lecture(String matricule, boolean valide) {
    }

    // ==================== QR D'ENTRÉE AFFICHÉ PAR LE VIGILE (pointage) ====================

    /**
     * Préfixe du QR d'entrée de l'école, affiché par le terminal du vigile et scanné par l'élève ou le professeur pour
     * pointer son arrivée ou son départ. QR signé, propre à l'école et de courte durée : une photo ne permet pas de
     * pointer de chez soi.
     */
    public static final String PREFIXE_ENTREE = "ENTREE_SECURE:";

    /**
     * Message signé du QR d'entrée. Séparé de celui du badge élève par un octet NUL, absent de tout matricule :
     * une signature de badge ne peut jamais servir de QR d'entrée, ni l'inverse.
     */
    private static byte[] messageEntree(String ecoleId, long minute) {
        return ("entree\0" + ecoleId + ":" + minute).getBytes(StandardCharsets.UTF_8);
    }

    /** Contenu du QR d'entrée : ENTREE_SECURE:{école}:{minute}:{signature}, valable pour la minute ± 1. */
    public String contenuEntree(String ecoleId, long minute) {
        try {
            Signature s = Signature.getInstance("Ed25519");
            s.initSign(privee);
            s.update(messageEntree(ecoleId, minute));
            return PREFIXE_ENTREE + ecoleId + ":" + minute + ":" + B64.encodeToString(s.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Signature Ed25519 impossible", e);
        }
    }

    /**
     * Lit un QR d'entrée scanné. Vide si ce n'en est pas un ; sinon l'école, la minute et la validité (signature
     * authentique ET minute à ± 1 de maintenant).
     */
    public Optional<LectureEntree> lireEntree(String contenu) {
        if (contenu == null || !contenu.startsWith(PREFIXE_ENTREE) || contenu.length() > 400) {
            return Optional.empty();
        }
        String reste = contenu.substring(PREFIXE_ENTREE.length());
        int finSignature = reste.lastIndexOf(':');
        int finMinute = finSignature > 0 ? reste.lastIndexOf(':', finSignature - 1) : -1;
        if (finMinute <= 0) {
            return Optional.of(new LectureEntree(null, 0, false));
        }
        String ecole = reste.substring(0, finMinute);
        String jeton = reste.substring(finSignature + 1);
        try {
            long minute = Long.parseLong(reste.substring(finMinute + 1, finSignature));
            boolean frais = Math.abs(minuteCourante() - minute) <= 1;
            return Optional.of(new LectureEntree(ecole, minute, frais && signatureEntreeValide(ecole, minute, jeton)));
        } catch (NumberFormatException e) {
            return Optional.of(new LectureEntree(ecole, 0, false));
        }
    }

    private boolean signatureEntreeValide(String ecoleId, long minute, String jeton) {
        if (ecoleId.isBlank() || jeton.length() > 200) return false;
        try {
            byte[] sig = D64.decode(jeton);
            if (sig.length != 64 || !B64.encodeToString(sig).equals(jeton)) return false;
            Signature s = Signature.getInstance("Ed25519");
            s.initVerify(publique);
            s.update(messageEntree(ecoleId, minute));
            return s.verify(sig);
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            return false;
        }
    }

    /** Résultat de la lecture d'un QR d'entrée. */
    public record LectureEntree(String ecoleId, long minute, boolean valide) {
    }
}
