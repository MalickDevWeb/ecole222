package ecole_221.sn.ecole.infrastructure.security.revocation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/** Déconnexion réelle et invalidation au changement de mot de passe. */
class RevocationJetonsTest {

    private final Map<String, JetonRevoqueJpaEntity> revoques = new HashMap<>();
    private final Map<String, InvalidationJetonsJpaEntity> invalidations = new HashMap<>();
    private RevocationJetons revocation;

    @BeforeEach
    void preparer() {
        JetonRevoqueJpaRepository r = Mockito.mock(JetonRevoqueJpaRepository.class);
        Mockito.when(r.save(any())).thenAnswer(i -> { JetonRevoqueJpaEntity e = i.getArgument(0); revoques.put(e.getEmpreinte(), e); return e; });
        Mockito.when(r.existsById(anyString())).thenAnswer(i -> revoques.containsKey(i.<String>getArgument(0)));
        InvalidationJetonsJpaRepository v = Mockito.mock(InvalidationJetonsJpaRepository.class);
        Mockito.when(v.save(any())).thenAnswer(i -> { InvalidationJetonsJpaEntity e = i.getArgument(0); invalidations.put(e.getUtilisateurId(), e); return e; });
        Mockito.when(v.findById(anyString())).thenAnswer(i -> Optional.ofNullable(invalidations.get(i.<String>getArgument(0))));
        revocation = new RevocationJetons(r, v);
    }

    @Test
    void jetonRevoqueALaDeconnexionEstRefuse() {
        Date emis = Date.from(Instant.now().minusSeconds(60));
        assertThat(revocation.estRefuse("jeton-a", "u1", emis)).isFalse();
        revocation.revoquer("jeton-a", Date.from(Instant.now().plus(1, ChronoUnit.HOURS)));
        assertThat(revocation.estRefuse("jeton-a", "u1", emis)).isTrue();
        assertThat(revocation.estRefuse("jeton-b", "u1", emis)).isFalse();
        assertThat(revoques.keySet()).allMatch(k -> k.length() == 64 && !k.contains("jeton"));   // empreinte seulement
    }

    @Test
    void changementDeMotDePasseInvalideLesJetonsAnterieurs() {
        Date avant = Date.from(Instant.now().minusSeconds(120));
        revocation.invaliderTout("u1");
        assertThat(revocation.estRefuse("ancien", "u1", avant)).isTrue();
        // jeton émis après (ou dans la même seconde) : accepté
        assertThat(revocation.estRefuse("nouveau", "u1", Date.from(Instant.now().plusSeconds(1)))).isFalse();
        // autre compte : non concerné
        assertThat(revocation.estRefuse("autre", "u2", avant)).isFalse();
    }

    @Test
    void pannedeLaBaseRefuseLeJeton() {
        JetonRevoqueJpaRepository casse = Mockito.mock(JetonRevoqueJpaRepository.class);
        Mockito.when(casse.existsById(anyString())).thenThrow(new RuntimeException("base indisponible"));
        RevocationJetons r = new RevocationJetons(casse, Mockito.mock(InvalidationJetonsJpaRepository.class));
        assertThat(r.estRefuse("x", "u1", new Date())).isTrue();
    }
}
