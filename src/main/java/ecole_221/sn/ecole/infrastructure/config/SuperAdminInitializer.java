package ecole_221.sn.ecole.infrastructure.config;

import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Initialise le compte SUPER_ADMIN par défaut au démarrage de l'application.
 *
 * <p>Ce composant s'exécute EN PREMIER (Order = 1) à chaque démarrage.
 * Il vérifie si un SUPER_ADMIN existe déjà en base, et s'il n'existe pas,
 * il en crée un avec des credentials définis par les variables d'environnement.</p>
 *
 * <p><b>Variables d'environnement :</b></p>
 * <ul>
 *   <li>{@code SUPER_ADMIN_EMAIL}    — Email du super admin (défaut : superadmin@ecole221.sn)</li>
 *   <li>{@code SUPER_ADMIN_PASSWORD} — Mot de passe (défaut : SuperAdmin@221! — À CHANGER EN PROD !)</li>
 *   <li>{@code SUPER_ADMIN_NOM}      — Nom (défaut : Super)</li>
 *   <li>{@code SUPER_ADMIN_PRENOM}   — Prénom (défaut : Admin)</li>
 * </ul>
 */
@Component
@Order(1)
public class SuperAdminInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminInitializer.class);

    // Valeurs par défaut — à surcharger via variables d'environnement en production
    private static final String DEFAULT_EMAIL    = "superadmin@ecole221.sn";
    private static final String DEFAULT_PASSWORD = "SuperAdmin@221!";
    private static final String DEFAULT_NOM      = "Super";
    private static final String DEFAULT_PRENOM   = "Admin";
    private static final String DEFAULT_MATRICULE = "SA-221-0001";

    private final UserJpaRepository userRepository;
    private final PasswordEncoder   passwordEncoder;

    public SuperAdminInitializer(UserJpaRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        String email = getEnv("SUPER_ADMIN_EMAIL",    DEFAULT_EMAIL);

        // Vérifier si un SUPER_ADMIN existe déjà
        if (userRepository.existsByEmail(email)) {
            logger.info("✅ SUPER_ADMIN déjà présent en base (email: {}). Aucune action requise.", email);
            return;
        }

        // Vérifier également s'il existe un user avec le rôle SUPER_ADMIN
        if (!userRepository.findByRole(RoleUtilisateur.SUPER_ADMIN.value()).isEmpty()) {
            logger.info("✅ Un compte SUPER_ADMIN existe déjà en base. Aucune action requise.");
            return;
        }

        // Récupérer les credentials depuis les variables d'environnement
        String rawPassword = getEnv("SUPER_ADMIN_PASSWORD", DEFAULT_PASSWORD);
        String nom         = getEnv("SUPER_ADMIN_NOM",      DEFAULT_NOM);
        String prenom      = getEnv("SUPER_ADMIN_PRENOM",   DEFAULT_PRENOM);

        // Encoder le mot de passe avec BCrypt
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Construire l'entité JPA
        UserJpaEntity superAdmin = new UserJpaEntity(
            UUID.randomUUID().toString(),
            nom,
            prenom,
            email,
            encodedPassword,
            RoleUtilisateur.SUPER_ADMIN.value(),
            DEFAULT_MATRICULE,
            null
        );
        superAdmin.setEmailVerified(true);  // Le super admin est vérifié par défaut
        superAdmin.setLanguage("fr");
        superAdmin.setTimezone("Africa/Dakar");

        // Sauvegarder en base
        userRepository.save(superAdmin);

        // Log de sécurité — ne jamais afficher le mot de passe en clair
        logger.warn("🚨 SUPER_ADMIN créé avec succès !");
        logger.warn("   Email    : {}", email);
        logger.warn("   Matricule: {}", DEFAULT_MATRICULE);
        if (DEFAULT_PASSWORD.equals(rawPassword)) {
            logger.warn("   ⚠️  MOT DE PASSE PAR DÉFAUT UTILISÉ — À CHANGER IMMÉDIATEMENT EN PRODUCTION !");
            logger.warn("      Définissez la variable d'environnement SUPER_ADMIN_PASSWORD.");
        }
    }

    /**
     * Lit une variable d'environnement avec une valeur par défaut en fallback.
     */
    private String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}
