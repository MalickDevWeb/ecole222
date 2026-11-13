package ecole_221.sn.ecole.modules.auth.application.service;

import ecole_221.sn.ecole.infrastructure.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ecole_221.sn.ecole.modules.auth.application.port.command.LoginCommand;
import ecole_221.sn.ecole.modules.auth.application.port.dto.LoginResponseDto;
import ecole_221.sn.ecole.modules.auth.application.port.usecase.LoginUseCase;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;

import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUser;
import java.util.List;

@Service
public class LoginService implements LoginUseCase {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    private final UserRepositoryPort userRepositoryPort;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final OrganizationUserRepositoryPort organizationUserRepositoryPort;
    private final OrganizationRepositoryPort organizationRepositoryPort;

    public LoginService(UserRepositoryPort userRepositoryPort,
                        JwtService jwtService,
                        PasswordEncoder passwordEncoder,
                        OrganizationUserRepositoryPort organizationUserRepositoryPort,
                        OrganizationRepositoryPort organizationRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.jwtService         = jwtService;
        this.passwordEncoder    = passwordEncoder;
        this.organizationUserRepositoryPort = organizationUserRepositoryPort;
        this.organizationRepositoryPort = organizationRepositoryPort;
    }

    /**
     * Freinage des essais de mot de passe (2026-11-13). Avant : après 8 mots de passe faux, le bon passait aussitôt —
     * rien ne ralentissait un essai massif de mots de passe sur un compte. Désormais : 5 échecs en 15 minutes pour
     * un même e-mail (existant ou non, pour ne pas révéler l'existence du compte) → 429 jusqu'à la fin de la fenêtre,
     * même avec le bon mot de passe. Une connexion réussie remet le compteur à zéro. Mémoire du serveur : en cas de
     * plusieurs instances, chaque instance compte de son côté (limite connue).
     */
    static final int ECHECS_MAX = 5;
    static final java.time.Duration FENETRE = java.time.Duration.ofMinutes(15);
    private final java.util.Map<String, java.util.Deque<java.time.Instant>> echecs = new java.util.concurrent.ConcurrentHashMap<>();
    java.time.Clock horloge = java.time.Clock.systemUTC();

    private void verifierFrein(String cle) {
        java.time.Instant maintenant = horloge.instant();
        java.util.Deque<java.time.Instant> recents = echecs.get(cle);
        if (recents == null) {
            return;
        }
        synchronized (recents) {
            while (!recents.isEmpty() && recents.peekFirst().isBefore(maintenant.minus(FENETRE))) recents.pollFirst();
            if (recents.isEmpty()) {
                echecs.remove(cle, recents);
                return;
            }
            if (recents.size() >= ECHECS_MAX) {
                long minutes = Math.max(1, (java.time.Duration.between(maintenant, recents.peekFirst().plus(FENETRE)).getSeconds() + 59) / 60);
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Trop de tentatives de connexion pour ce compte. Réessayez dans " + minutes + " minute" + (minutes > 1 ? "s" : "") + ".");
            }
        }
    }

    private volatile String leurre;

    /** Empreinte calculée une fois, au coût courant, pour les adresses inconnues. */
    private String empreinteLeurre() {
        String e = leurre;
        if (e == null) {
            e = passwordEncoder.encode("leurre-" + java.util.UUID.randomUUID());
            leurre = e;
        }
        return e;
    }

    private void noterEchec(String cle) {
        if (echecs.size() > 10_000) {   // garde-fou mémoire : purge des fenêtres expirées
            java.time.Instant limite = horloge.instant().minus(FENETRE);
            echecs.entrySet().removeIf(e -> { synchronized (e.getValue()) { return e.getValue().isEmpty() || e.getValue().peekLast().isBefore(limite); } });
        }
        java.util.Deque<java.time.Instant> recents = echecs.computeIfAbsent(cle, k -> new java.util.ArrayDeque<>());
        synchronized (recents) { recents.addLast(horloge.instant()); }
    }

    @Override
    public LoginResponseDto login(LoginCommand command) {
        log.info("Tentative de connexion pour : {}", command.getEmail());
        // corps vide / champs absents : 400 clair (avant : NullPointerException → 500)
        if (command.getEmail() == null || command.getEmail().isBlank()
                || command.getPassword() == null || command.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "L'e-mail et le mot de passe sont obligatoires.");
        }

        // SEC-002 : aucun compte n'est créé à la connexion (les comptes de démo sont créés au
        // démarrage par SuperAdminInitializer, hors profil prod) et aucun mot de passe universel.
        // E-mail tapé avec espaces ou majuscules (clavier de téléphone) : retrouvé quand même (2026-11-13 ;
        // avant : « Email ou mot de passe incorrect. » pour « Eleve@Ecole.sn »)
        String email = ecole_221.sn.ecole.partage.TexteNormalise.normaliser(command.getEmail());
        String cleFrein = email.toLowerCase(java.util.Locale.ROOT);
        if (cleFrein.length() > 320) cleFrein = cleFrein.substring(0, 320);
        verifierFrein(cleFrein);
        final String cle = cleFrein;
        User user = userRepositoryPort.findByEmail(email)
                .or(() -> userRepositoryPort.findByEmail(email.toLowerCase(java.util.Locale.ROOT)))
                .orElseThrow(() -> {
                    // Même coût de calcul qu'un compte existant : la durée de la réponse ne révèle pas si l'adresse
                    // existe (énumération des comptes par chronométrage, 2026-11-13).
                    passwordEncoder.matches(command.getPassword(), empreinteLeurre());
                    noterEchec(cle);
                    log.warn("Email non trouvé : {}", command.getEmail());
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "Email ou mot de passe incorrect.");
                });

        boolean passwordValid = command.getPassword() != null
                && passwordEncoder.matches(command.getPassword(), user.getPassword());

        if (!passwordValid) {
            noterEchec(cle);
            log.warn("Mot de passe incorrect pour : {}", command.getEmail());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Email ou mot de passe incorrect.");
        }

        echecs.remove(cle);
        // BR-03 (2026-11-13) : empreinte d'un coût inférieur (ancien coût 10) recalculée au coût actuel.
        if (passwordEncoder.upgradeEncoding(user.getPassword())) {
            try {
                user.setPassword(passwordEncoder.encode(command.getPassword()));
                userRepositoryPort.save(user);
            } catch (RuntimeException e) {
                log.warn("Mise à niveau de l'empreinte du mot de passe non enregistrée : {}", e.getMessage());
            }
        }
        log.info("Connexion réussie pour : {} (rôle={})", command.getEmail(), user.getRole());
        String token = jwtService.generateToken(user);
        String tenantSlug = resolveTenantSlug(user, command.getEmail());

        LoginResponseDto reponse = new LoginResponseDto(
                user.getId(),
                user.getNom(),
                user.getPrenom(),
                user.getEmail(),
                user.getRole(),
                token,
                tenantSlug
        );
        reponse.setDoitChangerMotDePasse(user.isDoitChangerMotDePasse());
        return reponse;
    }

    private String resolveTenantSlug(User user, String inputEmail) {
        String email = (inputEmail != null ? inputEmail : user.getEmail()).trim().toLowerCase();

        // 1. Appartenance réelle (organization_users) — prioritaire (SEC-003)
        try {
            List<OrganizationUser> memberships = organizationUserRepositoryPort.findByUserId(user.getId());
            if (memberships != null && !memberships.isEmpty()) {
                String orgId = memberships.get(0).getOrganizationId();
                if (orgId != null) {
                    var orgOpt = organizationRepositoryPort.findById(orgId);
                    if (orgOpt.isPresent()) {
                        return orgOpt.get().getSlug();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Impossible de résoudre l'organisation pour l'utilisateur {} : {}", user.getId(), e.getMessage());
        }

        // 2. À défaut : déduction depuis l'email <role>@<slug>.sn (comptes historiques sans appartenance)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^.+@([a-zA-Z0-9_-]+)\\.sn$");
        java.util.regex.Matcher matcher = pattern.matcher(email);
        if (matcher.matches()) {
            return matcher.group(1);
        }

        // 3. Fallback École 221
        return "ecole221";
    }
}
