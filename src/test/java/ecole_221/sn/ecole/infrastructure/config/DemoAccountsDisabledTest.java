package ecole_221.sn.ecole.infrastructure.config;

import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** APP-SEC-03 : aucun compte de démonstration par défaut, et aucun mot de passe écrit dans le code. */
class DemoAccountsDisabledTest {

    private final UserJpaRepository repo = mock(UserJpaRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);

    @Test
    void aucunCompteDemoParDefaut() {
        new SuperAdminInitializer(repo, encoder, new MockEnvironment()).run();
        // Sans SUPER_ADMIN_PASSWORD (non défini en test) ni activation explicite : rien n'est créé.
        verify(repo, never()).save(any());
    }

    @Test
    void activationSansMotDePasseRefusee() {
        MockEnvironment env = new MockEnvironment().withProperty("ecole.demo-accounts.enabled", "true");
        new SuperAdminInitializer(repo, encoder, env).run();
        verify(repo, never()).save(any());
    }

    @Test
    void activationRefuseeEnProd() {
        MockEnvironment env = new MockEnvironment().withProperty("ecole.demo-accounts.enabled", "true");
        env.setActiveProfiles("prod");
        new SuperAdminInitializer(repo, encoder, env).run();
        verify(repo, never()).save(any());
    }

    @Test
    void aucunMotDePasseLitteralDansLeCode() throws Exception {
        String src = Files.readString(Path.of("src/main/java/ecole_221/sn/ecole/infrastructure/config/SuperAdminInitializer.java"));
        assertThat(src).doesNotContain("\"ecole221\"").doesNotContain("Test@Ecole221!");
    }
}
