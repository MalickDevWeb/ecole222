package ecole_221.sn.ecole.modules.auth.application.service;

import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Changement du mot de passe par son titulaire (lot E, 2026-11-10).
 *
 * <p>Les comptes créés avec un mot de passe GÉNÉRÉ (élèves, professeurs, personnel, administrateur d'école
 * créé par le super-admin) portent le drapeau {@code doitChangerMotDePasse} : tant qu'il est présent, les
 * routes métier sont refusées (voir {@code MotDePasseProvisoireInterceptor}). Le changement l'efface.</p>
 *
 * <p>Règles : au moins {@value #LONGUEUR_MIN} caractères, au moins une lettre et un chiffre, différent de
 * l'ancien ; l'ancien mot de passe doit être exact.</p>
 */
@Service
public class ChangementMotDePasseService {

    public static final int LONGUEUR_MIN = 10;

    private final UserJpaRepository comptes;
    private final PasswordEncoder encodeur;

    public ChangementMotDePasseService(UserJpaRepository comptes, PasswordEncoder encodeur) {
        this.comptes = comptes;
        this.encodeur = encodeur;
    }

    /** Message d'erreur de la règle de robustesse, ou null si le mot de passe est acceptable. */
    public static String erreurRegles(String nouveau) {
        if (nouveau == null || nouveau.isBlank()) return "Le nouveau mot de passe est obligatoire.";
        if (nouveau.length() < LONGUEUR_MIN) {
            return "Le nouveau mot de passe doit contenir au moins " + LONGUEUR_MIN + " caractères.";
        }
        boolean lettre = nouveau.chars().anyMatch(Character::isLetter);
        boolean chiffre = nouveau.chars().anyMatch(Character::isDigit);
        if (!lettre || !chiffre) {
            return "Le nouveau mot de passe doit contenir au moins une lettre et un chiffre.";
        }
        return null;
    }

    @Transactional
    public void changer(String userId, String ancien, String nouveau) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Veuillez vous connecter.");
        }
        if (ancien == null || ancien.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'ancien mot de passe est obligatoire.");
        }
        String regle = erreurRegles(nouveau);
        if (regle != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, regle);
        }
        UserJpaEntity compte = comptes.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Compte introuvable."));
        if (compte.getPassword() == null || !encodeur.matches(ancien, compte.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'ancien mot de passe est incorrect.");
        }
        if (ancien.equals(nouveau) || encodeur.matches(nouveau, compte.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le nouveau mot de passe doit être différent de l'ancien.");
        }
        compte.setPassword(encodeur.encode(nouveau));
        compte.setDoitChangerMotDePasse(null);
        comptes.save(compte);
    }
}
