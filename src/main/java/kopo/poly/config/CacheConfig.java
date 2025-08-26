package kopo.poly.config;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class CacheConfig {

    @Bean("melonKeyGen")
    public KeyGenerator melonKeyGen() {
        return (target, method, params) ->
                "MELON_" + java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul"))
                        .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
    }

    @Bean
    RedisCacheManager redisCacheManager(RedisConnectionFactory cf) {
        var json = new GenericJackson2JsonRedisSerializer();
        var conf = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(json))
                .entryTtl(Duration.ofHours(3)) // 필요 TTL
                .disableCachingNullValues();
        return RedisCacheManager.builder(cf).cacheDefaults(conf).build();
    }
}
