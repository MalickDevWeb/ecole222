package ecole_221.sn.ecole.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    /** Longueur minimale (en caractères) exigée pour le secret HMAC (HS256 => 256 bits). */
    public static final int MIN_SECRET_LENGTH = 32;

    /**
     * Empreintes SHA-256 de secrets publics connus (ancienne valeur de repli publiée dans le dépôt).
     * Comparées par empreinte pour ne jamais réécrire ces valeurs en clair.
     */
    static final java.util.Set<String> SECRETS_PUBLICS_SHA256 = java.util.Set.of(
            "6c4449aac8611dad481a2d72aabdc2fbfaf4ac4d751c950d05e7b00ef41f80cc");

    private final String secretKey;

    /**
     * APP-SEC-02 : aucun secret par défaut. Le secret provient exclusivement de la propriété
     * {@code jwt.secret} (alimentée par la variable d'environnement JWT_SECRET).
     * S'il est absent ou trop court, la création du bean échoue et l'application refuse de démarrer.
     */
    public JwtService(@Value("${jwt.secret:}") String secretKey) {
        this.secretKey = validateSecret(secretKey);
    }

    static String validateSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "[SECURITY] JWT_SECRET est absent : l'application refuse de démarrer. "
                            + "Définissez la variable d'environnement JWT_SECRET (au moins "
                            + MIN_SECRET_LENGTH + " caractères aléatoires).");
        }
        if (secret.length() < MIN_SECRET_LENGTH) {
            // Ne jamais inclure la valeur du secret dans le message.
            throw new IllegalStateException(
                    "[SECURITY] JWT_SECRET est trop court (" + secret.length() + " caractères, minimum "
                            + MIN_SECRET_LENGTH + ") : l'application refuse de démarrer.");
        }
        if (SECRETS_PUBLICS_SHA256.contains(sha256(secret))) {
            throw new IllegalStateException(
                    "[SECURITY] JWT_SECRET correspond à une valeur publique connue (ancienne valeur par défaut) : "
                            + "l'application refuse de démarrer. Générez un nouveau secret.");
        }
        return secret;
    }

    static String sha256(String value) {
        try {
            byte[] d = java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(d);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails
    ) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // 24 hours
                .signWith(getSignInKey())
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
