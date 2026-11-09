package ecole_221.sn.ecole.infrastructure.config;

import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
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
 *   <li>{@code SUPER_ADMIN_PASSWORD} — Mot de passe (OBLIGATOIRE, aucun défaut : sans lui le compte n'est pas créé)</li>
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
    private static final String DEFAULT_NOM      = "Super";
    private static final String DEFAULT_PRENOM   = "Admin";
    private static final String DEFAULT_MATRICULE = "SA-221-0001";

    private final UserJpaRepository userRepository;
    private final PasswordEncoder   passwordEncoder;
    private final Environment       environment;

    public SuperAdminInitializer(UserJpaRepository userRepository, PasswordEncoder passwordEncoder,
                                 Environment environment) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment     = environment;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Initialiser le SUPER_ADMIN
        seedSuperAdmin();

        // 2. Comptes de démonstration : DÉSACTIVÉS par défaut (APP-SEC-03).
        //    Activation explicite seulement : ECOLE_DEMO_ACCOUNTS_ENABLED=true + DEMO_ACCOUNTS_PASSWORD (≥ 12 car.).
        //    Jamais avec le profil prod, et plus aucun mot de passe écrit dans le code.
        boolean demande = Boolean.parseBoolean(environment.getProperty("ecole.demo-accounts.enabled", "false"));
        String motDePasse = getEnv("DEMO_ACCOUNTS_PASSWORD", "");
        if (!demande) {
            logger.info("Comptes de démonstration désactivés (ecole.demo-accounts.enabled=false).");
        } else if (environment.acceptsProfiles(Profiles.of("prod"))) {
            logger.warn("Comptes de démonstration demandés mais refusés : profil prod actif.");
        } else if (motDePasse.length() < 12) {
            logger.warn("Comptes de démonstration non créés : DEMO_ACCOUNTS_PASSWORD absent ou < 12 caractères.");
        } else {
            seedDemoUsers(motDePasse);
        }
    }

    private void seedSuperAdmin() {
        String email = getEnv("SUPER_ADMIN_EMAIL", DEFAULT_EMAIL);

        if (userRepository.existsByEmail(email) || !userRepository.findByRole(RoleUtilisateur.SUPER_ADMIN.value()).isEmpty()) {
            logger.info("✅ SUPER_ADMIN déjà présent en base. Aucune action requise.");
            return;
        }

        String rawPassword = getEnv("SUPER_ADMIN_PASSWORD", null);
        if (rawPassword == null) {
            logger.error("❌ SUPER_ADMIN_PASSWORD non défini : SUPER_ADMIN non créé. Ajoutez-le dans backend/.env.");
            return;
        }
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

    private void seedDemoUsers(String motDePasse) {
        seedSingleUser("admin@ecole221.sn", motDePasse, "Admin", "École 221", RoleUtilisateur.ADMIN.value(), "ADM-221-001");
        seedSingleUser("admin221@gmail.com", motDePasse, "Admin", "École 221", RoleUtilisateur.ADMIN.value(), "ADM-221-002");
        seedSingleUser("prof@ecole221.sn", motDePasse, "Professeur", "Test", RoleUtilisateur.PROFESSEUR.value(), "PRF-221-001");
        seedSingleUser("professeur221@gmail.com", motDePasse, "Professeur", "Test", RoleUtilisateur.PROFESSEUR.value(), "PRF-221-002");
        seedSingleUser("etudiant@ecole221.sn", motDePasse, "Étudiant", "Test", RoleUtilisateur.ETUDIANT.value(), "ETU-221-001");
        seedSingleUser("etudiant221@gmail.com", motDePasse, "Étudiant", "Test", RoleUtilisateur.ETUDIANT.value(), "ETU-221-002");
        seedSingleUser("vigil@ecole221.sn", motDePasse, "Vigile", "Sécurité", RoleUtilisateur.VIGIL.value(), "VIG-221-001");
        seedSingleUser("vigile221@gmail.com", motDePasse, "Vigile", "Sécurité", RoleUtilisateur.VIGIL.value(), "VIG-221-002");
        seedSingleUser("parent221@gmail.com", motDePasse, "Diallo", "Mamadou", RoleUtilisateur.PARENT.value(), "PAR-221-001");
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
