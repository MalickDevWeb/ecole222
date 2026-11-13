package ecole_221.sn.ecole.partage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Jeton du badge QR dynamique des élèves (2026-11-13, sécurité maximale).
 *
 * <p>Avant : hachage FNV-1a 32 bits avec une « clé secrète » écrite en dur dans le code, recopiée (avec une autre
 * valeur !) dans le frontend. N'importe qui pouvait fabriquer le badge valide de n'importe quel élève à partir de son
 * matricule, et le badge affiché par l'application était de toute façon refusé.</p>
 *
 * <p>Désormais : HMAC-SHA256, clé connue du seul serveur (ECOLE_BADGE_SECRET, sinon dérivée de JWT_SECRET, sinon
 * aléatoire au démarrage), 16 caractères hexadécimaux (64 bits), valable pour la minute indiquée ± 1 minute, comparé
 * en temps constant. Le badge n'est produit que par le serveur ({@code GET /student/digital-badge}).</p>
 */
@Component
public class JetonBadge {

    public static final String PREFIXE = "STU_SECURE:";
    private static final int LONGUEUR = 16;

    private final byte[] cle;

    public JetonBadge(@Value("${ecole.badge.secret:}") String secretBadge, @Value("${jwt.secret:}") String secretJwt) {
        this.cle = deriver(secretBadge, secretJwt);
    }

    /** Clé aléatoire (tests, ou serveur sans secret configuré : les badges ne survivent pas à un redémarrage). */
    public static JetonBadge aleatoire() {
        return new JetonBadge("", "");
    }

    private static byte[] deriver(String secretBadge, String secretJwt) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            if (secretBadge != null && !secretBadge.isBlank()) {
                return sha.digest(("badge:" + secretBadge).getBytes(StandardCharsets.UTF_8));
            }
            if (secretJwt != null && !secretJwt.isBlank()) {
                return sha.digest(("badge-qr-ecole221:" + secretJwt).getBytes(StandardCharsets.UTF_8));
            }
            byte[] k = new byte[32];
            new SecureRandom().nextBytes(k);
            return k;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    /** Jeton du matricule pour la minute donnée (minutes depuis l'époque Unix). */
    public String jeton(String matricule, long minute) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(cle, "HmacSHA256"));
            byte[] h = mac.doFinal((matricule + ":" + minute).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().withUpperCase().formatHex(h).substring(0, LONGUEUR);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 indisponible", e);
        }
    }

    public static long minuteCourante() {
        return System.currentTimeMillis() / 60_000L;
    }

    /** Contenu du QR : STU_SECURE:{matricule}:{minute}:{jeton}. */
    public String contenu(String matricule, long minute) {
        return PREFIXE + matricule + ":" + minute + ":" + jeton(matricule, minute);
    }

    /** Vrai si le jeton correspond au matricule et à la minute, et si cette minute est à ±1 de maintenant. */
    public boolean valide(String matricule, long minute, String jeton) {
        if (matricule == null || matricule.isBlank() || jeton == null || jeton.length() != LONGUEUR) {
            return false;
        }
        if (Math.abs(minuteCourante() - minute) > 1) {
            return false;
        }
        return MessageDigest.isEqual(jeton(matricule, minute).getBytes(StandardCharsets.US_ASCII),
                jeton.toUpperCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Lit un contenu scanné. Vide si ce n'est pas un badge dynamique ; sinon le matricule si le jeton est valide, ou
     * {@code null} dans l'Optional présent… (voir {@link Lecture}).
     */
    public Optional<Lecture> lire(String contenu) {
        if (contenu == null || !contenu.startsWith(PREFIXE)) {
            return Optional.empty();
        }
        String[] p = contenu.split(":");
        if (p.length != 4) {
            return Optional.of(new Lecture(null, false));
        }
        try {
            long minute = Long.parseLong(p[2]);
            return Optional.of(new Lecture(p[1], valide(p[1], minute, p[3])));
        } catch (NumberFormatException e) {
            return Optional.of(new Lecture(p[1], false));
        }
    }

    /** Résultat de la lecture d'un badge dynamique. */
    public record Lecture(String matricule, boolean valide) {
    }
}
