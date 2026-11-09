package ecole_221.sn.ecole.infrastructure.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** APP-SEC-12 : les admissions ne sont plus publiques en bloc ; seules les routes du portail le sont. */
class PortailCandidatPublicTest {

    @Test
    void plusDeJokerAdmissionsDansLesRoutesPubliques() {
        assertTrue(Stream.of(SecurityConfig.PUBLIC_PATHS, SecurityConfig.PORTAIL_CANDIDAT_LECTURE,
                        SecurityConfig.PORTAIL_CANDIDAT_ECRITURE)
                .flatMap(Arrays::stream)
                .noneMatch(p -> (p.contains("admissions") || p.contains("candidatures")) && p.contains("*")));
    }

    @Test
    void reglagesCandidatureEnLectureSeulementPourLePublic() {
        // POST /candidatures/settings modifie la campagne : jamais public (toutes méthodes)
        assertFalse(Arrays.asList(SecurityConfig.PUBLIC_PATHS).contains("/candidatures/settings"));
        assertFalse(Arrays.asList(SecurityConfig.PORTAIL_CANDIDAT_ECRITURE).contains("/candidatures/settings"));
    }

    @Test
    void routesAdministrativesNonPubliques() {
        String[] tout = Stream.of(SecurityConfig.PUBLIC_PATHS, SecurityConfig.PORTAIL_CANDIDAT_LECTURE,
                SecurityConfig.PORTAIL_CANDIDAT_ECRITURE).flatMap(Arrays::stream).toArray(String[]::new);
        for (String admin : new String[]{"/admissions/candidates", "/candidatures/candidatures"}) {
            assertFalse(Arrays.asList(tout).contains(admin), admin + " ne doit pas être public");
        }
    }
}
