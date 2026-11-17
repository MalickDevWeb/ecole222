package ecole_221.sn.ecole.modules.auth.infrastructure.persistence;

import ecole_221.sn.ecole.partage.NumeroTelephone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Remise au format E.164 des numéros de téléphone déjà enregistrés sur les comptes (2026-11-16).
 *
 * <p>La connexion par téléphone (LoginService) cherche le compte par la forme E.164 de la saisie (« +221771234567 ») ;
 * un numéro enregistré avant le 15/11 sous une autre forme (« +221 77 123 45 67 », « 771234567 », « 00221… ») ne
 * correspondait jamais : le parent ne pouvait pas se connecter avec son numéro.</p>
 *
 * <p>Au démarrage, chaque numéro de {@code users.telephone} qui n'est pas déjà en E.164 est converti avec
 * {@link NumeroTelephone#e164(String)} (mêmes règles que la saisie : mobiles sénégalais, numéros internationaux avec
 * indicatif), et SEULEMENT si la conversion est sans ambiguïté :</p>
 * <ul>
 *   <li>forme non reconnue (fixe, indicatif absent hors Sénégal, plusieurs numéros, texte…) : laissé tel quel ;</li>
 *   <li>forme E.164 déjà portée par un autre compte, ou visée par plusieurs comptes : tous laissés tels quels
 *       (la contrainte d'unicité l'interdirait, et on ne choisit pas à qui appartient le numéro).</li>
 * </ul>
 * <p>Idempotent (un numéro déjà en E.164 n'est jamais réécrit) et prudent (UPDATE conditionné à la valeur lue). Le
 * journal ne montre jamais un numéro en clair : identifiant du compte et numéro masqué seulement. Désactivable par
 * {@code ecole.telephones.normalisation.active=false}.</p>
 */
@Component
@Order(20)
@ConditionalOnProperty(name = "ecole.telephones.normalisation.active", havingValue = "true", matchIfMissing = true)
public class NormalisationTelephones implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(NormalisationTelephones.class);

    private final JdbcTemplate jdbc;

    public NormalisationTelephones(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Résultat d'un passage : numéros convertis, laissés faute de forme reconnue, laissés pour conflit. */
    public record Bilan(int convertis, int nonReconnus, int conflits) {}

    @Override
    public void run(ApplicationArguments args) {
        try {
            Bilan b = normaliser();
            if (b.convertis() + b.nonReconnus() + b.conflits() > 0) {
                log.info("Téléphones des comptes : {} remis au format international, {} non reconnus et {} en conflit laissés tels quels.",
                        b.convertis(), b.nonReconnus(), b.conflits());
            }
        } catch (DataAccessException e) {
            // base sans table users (initialisation en cours) : rien à faire, l'application démarre
            log.warn("Normalisation des téléphones impossible : {}", e.getMostSpecificCause().getClass().getSimpleName());
        }
    }

    public Bilan normaliser() {
        List<Map<String, Object>> lignes = jdbc.queryForList(
                "SELECT id, telephone FROM users WHERE telephone IS NOT NULL AND telephone <> ''");

        // Nombre de comptes qui portent ou viseraient chaque forme E.164
        Map<String, Integer> demandes = new HashMap<>();
        for (Map<String, Object> l : lignes) {
            String t = (String) l.get("telephone");
            NumeroTelephone.e164(t).ifPresentOrElse(e -> demandes.merge(e, 1, Integer::sum), () -> demandes.merge(t, 1, Integer::sum));
        }

        int convertis = 0, nonReconnus = 0, conflits = 0;
        for (Map<String, Object> l : lignes) {
            String id = String.valueOf(l.get("id"));
            String t = (String) l.get("telephone");
            Optional<String> e164 = NumeroTelephone.e164(t);
            if (e164.isPresent() && e164.get().equals(t)) continue;   // déjà au bon format
            if (e164.isEmpty()) {
                nonReconnus++;
                log.warn("Téléphone du compte {} laissé tel quel : forme non reconnue.", id);
                continue;
            }
            String cible = e164.get();
            if (demandes.getOrDefault(cible, 0) > 1) {
                conflits++;
                log.warn("Téléphone du compte {} laissé tel quel : {} est aussi le numéro d'un autre compte.", id, NumeroTelephone.masquer(cible));
                continue;
            }
            if (jdbc.update("UPDATE users SET telephone = ? WHERE id = ? AND telephone = ?", cible, id, t) == 1) {
                convertis++;
                log.info("Téléphone du compte {} remis au format international ({}).", id, NumeroTelephone.masquer(cible));
            }
        }
        return new Bilan(convertis, nonReconnus, conflits);
    }
}
