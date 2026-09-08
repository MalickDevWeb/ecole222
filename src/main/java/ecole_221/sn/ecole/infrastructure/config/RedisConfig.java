package ecole_221.sn.ecole.infrastructure.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.time.Duration;

/**
 * Configuration Redis pour le cache de l'application.
 * 
 * Cette classe configure le gestionnaire de cache Redis avec :
 * - TTL de 15 minutes par défaut
 * - Sérialisation des clés en String
 * - Sérialisation des valeurs en JSON
 * - Désactivation du cache des valeurs null
 * 
 * Note: GenericJackson2JsonRedisSerializer est déprécié depuis Spring Data Redis 4.0
 * en faveur de GenericJacksonJsonRedisSerializer (Jackson 3).
 * Cependant, le remplacement nécessite Jackson 3 (tools.jackson) qui n'est pas
 * actuellement inclus dans les dépendances du projet.
 */
@Configuration
@EnableCaching
@SuppressWarnings("deprecation")
public class RedisConfig {

    @Bean
    public org.springframework.cache.CacheManager cacheManager(org.springframework.data.redis.connection.RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
                
        return org.springframework.data.redis.cache.RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
