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
        // 1. Initialiser le SUPER_ADMIN
        seedSuperAdmin();

        // 2. Initialiser les comptes démo pour l'école par défaut (École 221)
        seedDemoUsers();
    }

    private void seedSuperAdmin() {
        String email = getEnv("SUPER_ADMIN_EMAIL", DEFAULT_EMAIL);

        if (userRepository.existsByEmail(email) || !userRepository.findByRole(RoleUtilisateur.SUPER_ADMIN.value()).isEmpty()) {
            logger.info("✅ SUPER_ADMIN déjà présent en base. Aucune action requise.");
            return;
        }

        String rawPassword = getEnv("SUPER_ADMIN_PASSWORD", DEFAULT_PASSWORD);
        String nom         = getEnv("SUPER_ADMIN_NOM",      DEFAULT_NOM);
        String prenom      = getEnv("SUPER_ADMIN_PRENOM",   DEFAULT_PRENOM);

        String encodedPassword = passwordEncoder.encode(rawPassword);

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
        superAdmin.setEmailVerified(true);
        superAdmin.setLanguage("fr");
        superAdmin.setTimezone("Africa/Dakar");
        userRepository.save(superAdmin);
        logger.warn("🚨 SUPER_ADMIN créé avec succès (email: {}) !", email);
    }

    private void seedDemoUsers() {
        seedSingleUser("admin@ecole221.sn", "Test@Ecole221!", "Admin", "École 221", RoleUtilisateur.ADMIN.value(), "ADM-221-001");
        seedSingleUser("admin221@gmail.com", "ecole221", "Admin", "École 221", RoleUtilisateur.ADMIN.value(), "ADM-221-002");
        seedSingleUser("prof@ecole221.sn", "Test@Ecole221!", "Professeur", "Test", RoleUtilisateur.PROFESSEUR.value(), "PRF-221-001");
        seedSingleUser("professeur221@gmail.com", "ecole221", "Professeur", "Test", RoleUtilisateur.PROFESSEUR.value(), "PRF-221-002");
        seedSingleUser("etudiant@ecole221.sn", "Test@Ecole221!", "Étudiant", "Test", RoleUtilisateur.ETUDIANT.value(), "ETU-221-001");
        seedSingleUser("etudiant221@gmail.com", "ecole221", "Étudiant", "Test", RoleUtilisateur.ETUDIANT.value(), "ETU-221-002");
        seedSingleUser("vigil@ecole221.sn", "Test@Ecole221!", "Vigile", "Sécurité", RoleUtilisateur.VIGIL.value(), "VIG-221-001");
        seedSingleUser("vigile221@gmail.com", "ecole221", "Vigile", "Sécurité", RoleUtilisateur.VIGIL.value(), "VIG-221-002");
        seedSingleUser("parent221@gmail.com", "ecole221", "Diallo", "Mamadou", RoleUtilisateur.PARENT.value(), "PAR-221-001");
    }

    private void seedSingleUser(String email, String rawPassword, String nom, String prenom, String role, String matricule) {
        if (!userRepository.existsByEmail(email)) {
            UserJpaEntity user = new UserJpaEntity(
                UUID.randomUUID().toString(),
                nom,
                prenom,
                email,
                passwordEncoder.encode(rawPassword),
                role,
                matricule,
                null
            );
            user.setEmailVerified(true);
            user.setLanguage("fr");
            user.setTimezone("Africa/Dakar");
            userRepository.save(user);
            logger.info("✅ Compte démo initialisé : {} (rôle={})", email, role);
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
