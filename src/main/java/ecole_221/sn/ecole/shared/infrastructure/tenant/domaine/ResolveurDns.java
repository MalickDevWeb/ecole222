package ecole_221.sn.ecole.shared.infrastructure.tenant.domaine;

import java.util.Optional;

/**
 * Lecture d'UN saut de la chaîne CNAME d'un nom d'hôte (interface : un résolveur simulé la remplace dans les tests).
 *
 * <p>Deux issues distinctes (revue DevSecOps phase 2, critère 5) : vide = la réponse DNS dit qu'il n'y a pas de
 * CNAME (nom inexistant ou sans CNAME) — résultat gardé en mémoire 60 s ; {@link Indisponible} = la lecture n'a pas
 * pu se faire (délai dépassé, serveur injoignable, trop de lectures simultanées) — jamais gardé en mémoire, et jamais
 * compté comme un échec de pointage d'un domaine déclaré.</p>
 */
public interface ResolveurDns {

    /** Cible de l'enregistrement CNAME de {@code hote} (sans point final), ou vide s'il n'y en a pas. */
    Optional<String> cname(String hote);

    /** La lecture DNS n'a pas pu se faire : on ne sait pas si le CNAME existe. */
    class Indisponible extends RuntimeException {
        public Indisponible(String message) {
            super(message);
        }
    }
}
