package ecole_221.sn.ecole.infrastructure.config;

import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.time.Duration;

/**
 * Configuration Redis pour le cache de l'application.
 *
 * - TTL de 15 minutes, clés en String, valeurs en JSON, pas de cache des valeurs null.
 * - Clé = {@code <préfixe>:<école>::<cache>::<clé>} : l'école courante (TenantContext) fait partie de
 *   la clé, un même utilisateur membre de deux écoles ne voit jamais le cache de l'autre. Le préfixe
 *   ({@code ecole.cache.key-prefix}) sépare les environnements qui partageraient un même Redis.
 * - Redis indisponible : l'erreur est journalisée et la méthode s'exécute normalement (pas de 500).
 *
 * Note: GenericJackson2JsonRedisSerializer est déprécié depuis Spring Data Redis 4.0
 * en faveur de GenericJacksonJsonRedisSerializer (Jackson 3), qui n'est pas dans les dépendances.
 */
@Configuration
@EnableCaching
@SuppressWarnings("deprecation")
public class RedisConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${ecole.cache.key-prefix:ecole}")
    private String keyPrefix;

    @Bean
    public org.springframework.cache.CacheManager cacheManager(org.springframework.data.redis.connection.RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15))
                .disableCachingNullValues()
                .computePrefixWith(tenantPrefix(keyPrefix))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return org.springframework.data.redis.cache.RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    /** Calculé à chaque lecture / écriture : dépend de l'école de la requête en cours. */
    static CacheKeyPrefix tenantPrefix(String prefix) {
        return cacheName -> prefix + ":" + TenantContext.getCurrentOrganizationId().orElse("platform")
                + "::" + cacheName + "::";
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache {} indisponible (lecture) : {}", cache.getName(), e.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Cache {} indisponible (écriture) : {}", cache.getName(), e.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache {} indisponible (invalidation) : {}", cache.getName(), e.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("Cache {} indisponible (purge) : {}", cache.getName(), e.getMessage());
            }
        };
    }
}
