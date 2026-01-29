package com.linkwave.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Configuration for Spring Session with Redis backend.
 * Enables HTTP session storage in Redis with configurable timeout.
 */
@Configuration
@EnableRedisHttpSession
@Profile("!test")
public class SessionConfig {

    private final RedisConfig redisConfig;

    public SessionConfig(RedisConfig redisConfig) {
        this.redisConfig = redisConfig;
    }

    /**
     * Configure Redis connection factory with credentials and settings.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(this.redisConfig.getHost());
        config.setPort(this.redisConfig.getPort());
        
        if (this.redisConfig.getPassword() != null && !this.redisConfig.getPassword().isEmpty()) {
            config.setPassword(RedisPassword.of(this.redisConfig.getPassword()));
        }
        
        return new LettuceConnectionFactory(config);
    }

    /**
     * Configure RedisTemplate for general Redis operations.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
}
