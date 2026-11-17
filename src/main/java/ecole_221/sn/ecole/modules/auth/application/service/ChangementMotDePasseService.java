package ecole_221.sn.ecole.modules.auth.application.service;

import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Changement du mot de passe par son titulaire (lot E).
 *
 * <p>Les comptes créés avec un mot de passe GÉNÉRÉ (élèves, professeurs, personnel, administrateur d'école
 * créé par le super-admin) portent le drapeau {@code doitChangerMotDePasse} : tant qu'il est présent, les
 * routes métier sont refusées (voir {@code MotDePasseProvisoireInterceptor}). Le changement l'efface.</p>
 *
 * <p>Règles : PolitiqueMotDePasse (longueur selon le rôle, liste de refus, nom, suites, ancien) ; l'ancien
 * mot de passe doit être exact.</p>
 */
@Service
public class ChangementMotDePasseService {

    public static final int LONGUEUR_MIN = 10;

    private final UserJpaRepository comptes;
    private final PasswordEncoder encodeur;
    /** Les jetons émis avant le changement sont refusés (absent seulement dans certains tests). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ecole_221.sn.ecole.infrastructure.security.revocation.RevocationJetons revocation;

    public ChangementMotDePasseService(UserJpaRepository comptes, PasswordEncoder encodeur) {
        this.comptes = comptes;
        this.encodeur = encodeur;
    }

    /**
     * Règle UNIQUE de robustesse (PolitiqueMotDePasse) — création, activation, réinitialisation, changement,
     * inscription publique. {@code compte} null : inscription publique d'une école (règles de l'administration).
     * Message d'erreur en français, ou null si le mot de passe est accepté.
     */
    public static String erreurRegles(String motDePasse, UserJpaEntity compte) {
        return PolitiqueMotDePasse.erreur(motDePasse, compte, java.util.List.of());
    }

    /** Même règle, avec des mots interdits en plus (nom et identifiant de l'école à l'inscription publique…). */
    public static String erreurRegles(String motDePasse, UserJpaEntity compte, java.util.Collection<String> motsInterdits) {
        return PolitiqueMotDePasse.erreur(motDePasse, compte, motsInterdits);
    }

    /** Même règle ; chaque mot interdit dit ce qu'il représente, pour un refus précis (« contient votre prénom »). */
    public static String erreurReglesDetaillee(String motDePasse, UserJpaEntity compte,
                                               java.util.List<PolitiqueMotDePasse.MotInterdit> motsInterdits) {
        return PolitiqueMotDePasse.erreurDetaillee(motDePasse, compte, motsInterdits);
    }

    @Transactional
    public void changer(String userId, String ancien, String nouveau) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Veuillez vous connecter.");
        }
        if (ancien == null || ancien.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'ancien mot de passe est obligatoire.");
        }
        UserJpaEntity compte = comptes.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Compte introuvable."));
        if (compte.getPassword() == null || !encodeur.matches(ancien, compte.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'ancien mot de passe est incorrect.");
        }
        String regle = erreurRegles(nouveau, compte);   // règle unique (longueur selon le rôle, liste, nom, ancien…)
        if (regle != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, regle);
        }
        if (ancien.equals(nouveau) || encodeur.matches(nouveau, compte.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le nouveau mot de passe doit être différent de l'ancien.");
        }
        compte.setPassword(encodeur.encode(nouveau));
        compte.setDoitChangerMotDePasse(null);
        comptes.save(compte);
        if (revocation != null) {
            revocation.invaliderTout(compte.getId());
        }
    }
}
