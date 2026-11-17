package ecole_221.sn.ecole.partage;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Compte connecté de la requête (15/11 : auteur d'une saisie, par exemple d'un versement). Rien n'est inventé : sans
 * compte authentifié, identifiant et nom valent null.
 */
public final class CompteCourant {

    private CompteCourant() {
    }

    private static User compte() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof User u ? u : null;
    }

    public static String id() {
        User u = compte();
        return u == null ? null : u.getId();
    }

    /** « Prénom Nom » du compte connecté, ou null (jamais l'e-mail ni une valeur par défaut). */
    public static String nomComplet() {
        User u = compte();
        if (u == null) {
            return null;
        }
        String n = ((u.getPrenom() == null ? "" : u.getPrenom().trim()) + " " + (u.getNom() == null ? "" : u.getNom().trim())).trim();
        return n.isEmpty() ? null : n;
    }
}
