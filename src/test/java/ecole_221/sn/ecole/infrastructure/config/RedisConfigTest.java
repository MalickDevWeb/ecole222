package ecole_221.sn.ecole.infrastructure.config;

import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.data.redis.RedisConnectionFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Cache : clés cloisonnées par école et panne de Redis sans erreur 500. */
class RedisConfigTest {

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    private static void school(String id) {
        Organization org = mock(Organization.class);
        when(org.getId()).thenReturn(id);
        TenantContext.setCurrentOrganization(org);
    }

    @Test
    void sameKeyInTwoSchoolsGivesTwoCacheEntries() {
        var prefix = RedisConfig.tenantPrefix("ecole");
        school("org-a");
        String a = prefix.compute("professorCoursesV2");
        school("org-b");
        String b = prefix.compute("professorCoursesV2");
        assertThat(a).isEqualTo("ecole:org-a::professorCoursesV2::");
        assertThat(b).isEqualTo("ecole:org-b::professorCoursesV2::");
    }

    @Test
    void noSchoolUsesPlatformNamespace() {
        assertThat(RedisConfig.tenantPrefix("qa").compute("studentProfile")).isEqualTo("qa:platform::studentProfile::");
    }

    @Test
    void redisFailureIsSwallowed() {
        var handler = new RedisConfig().errorHandler();
        Cache cache = mock(Cache.class);
        when(cache.getName()).thenReturn("studentProfile");
        var down = new RedisConnectionFailureException("Unable to connect to Redis");
        assertThatCode(() -> {
            handler.handleCacheGetError(down, cache, "k");
            handler.handleCachePutError(down, cache, "k", "v");
            handler.handleCacheEvictError(down, cache, "k");
            handler.handleCacheClearError(down, cache);
        }).doesNotThrowAnyException();
    }
}
