package ecole_221.sn.ecole.infrastructure.security.revocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HexFormat;

/**
 * Révocation des jetons JWT (sécurité maximale).
 * <ul>
 *   <li>déconnexion : le jeton présenté est révoqué jusqu'à son expiration (seule son empreinte est stockée) ;</li>
 *   <li>changement / réinitialisation du mot de passe : tous les jetons émis avant ce moment sont refusés.</li>
 * </ul>
 * En cas de panne de la base, le contrôle échoue « fermé » : le jeton est refusé.
 */
@Service
public class RevocationJetons {

    private static final Logger log = LoggerFactory.getLogger(RevocationJetons.class);

    private final JetonRevoqueJpaRepository revoques;
    private final InvalidationJetonsJpaRepository invalidations;

    public RevocationJetons(JetonRevoqueJpaRepository revoques, InvalidationJetonsJpaRepository invalidations) {
        this.revoques = revoques;
        this.invalidations = invalidations;
    }

    static String empreinte(String jeton) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(jeton.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    /** Révoque ce jeton jusqu'à son expiration. */
    public void revoquer(String jeton, Date expiration) {
        if (jeton == null || jeton.isBlank()) {
            return;
        }
        Instant fin = expiration == null ? Instant.now().plus(1, ChronoUnit.DAYS) : expiration.toInstant();
        revoques.save(new JetonRevoqueJpaEntity(empreinte(jeton), fin));
        purger();
    }

    /**
     * Invalide tous les jetons du compte émis avant maintenant. Arrondi à la seconde inférieure : un jeton émis juste
     * après (même seconde, champ « iat » en secondes) reste accepté.
     */
    public void invaliderTout(String utilisateurId) {
        if (utilisateurId == null) {
            return;
        }
        Instant maintenant = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        InvalidationJetonsJpaEntity e = invalidations.findById(utilisateurId)
                .orElseGet(() -> new InvalidationJetonsJpaEntity(utilisateurId, maintenant));
        e.setValidesApres(maintenant);
        invalidations.save(e);
    }

    /** Vrai si le jeton est révoqué, émis avant une invalidation du compte, ou si la vérification est impossible. */
    public boolean estRefuse(String jeton, String utilisateurId, Date emisLe) {
        try {
            if (revoques.existsById(empreinte(jeton))) {
                return true;
            }
            if (utilisateurId != null) {
                Instant apres = invalidations.findById(utilisateurId).map(InvalidationJetonsJpaEntity::getValidesApres).orElse(null);
                if (apres != null && (emisLe == null || emisLe.toInstant().isBefore(apres))) {
                    return true;
                }
            }
            return false;
        } catch (RuntimeException e) {
            log.warn("Vérification de révocation impossible, jeton refusé : {}", e.getMessage());
            return true;
        }
    }

    /** Ménage à chaque révocation : les jetons expirés n'ont plus besoin d'être retenus. */
    void purger() {
        try {
            int n = revoques.supprimerExpires(Instant.now());
            if (n > 0) {
                log.info("{} jeton(s) révoqué(s) expiré(s) purgé(s)", n);
            }
        } catch (RuntimeException e) {
            log.warn("Purge des jetons révoqués impossible : {}", e.getMessage());
        }
    }
}
