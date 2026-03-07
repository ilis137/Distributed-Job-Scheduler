package com.scheduler.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis and Redisson configuration.
 * 
 * Configures Redisson client for distributed coordination primitives
 * (leader election, distributed locks, etc.).
 * 
 * Interview Talking Points:
 * - "I use Redisson over Jedis because it provides high-level abstractions
 *   for distributed patterns like Redlock, semaphores, and rate limiters"
 * - "Redisson handles connection pooling, retry logic, and failover automatically"
 * - "The single-server configuration is simple but can be upgraded to
 *   Redis Sentinel or Cluster for production high availability"
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
@Configuration
public class RedisConfig {
    
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;
    
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;
    
    @Value("${spring.data.redis.password:}")
    private String redisPassword;
    
    @Value("${spring.data.redis.timeout:2000ms}")
    private String redisTimeout;
    
    /**
     * Creates the Redisson client bean.
     * 
     * Interview Talking Point:
     * "I configure Redisson with a single-server setup for simplicity.
     * In production, I would use Redis Sentinel for automatic failover
     * or Redis Cluster for horizontal scaling and partitioning."
     * 
     * @return configured RedissonClient
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        String address = String.format("redis://%s:%d", redisHost, redisPort);
        
        config.useSingleServer()
            .setAddress(address)
            .setPassword(redisPassword.isEmpty() ? null : redisPassword)
            .setConnectionPoolSize(64)
            .setConnectionMinimumIdleSize(10)
            .setConnectTimeout(parseTimeout(redisTimeout))
            .setTimeout(parseTimeout(redisTimeout))
            .setRetryAttempts(3)
            .setRetryInterval(1500)
            .setPingConnectionInterval(30000) // 30 seconds
            .setKeepAlive(true);
        
        return Redisson.create(config);
    }
    
    /**
     * Creates Redis connection factory using Redisson.
     * 
     * This allows Spring Data Redis to work with Redisson.
     * 
     * @param redissonClient the Redisson client
     * @return RedisConnectionFactory
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory(RedissonClient redissonClient) {
        return new RedissonConnectionFactory(redissonClient);
    }
    
    /**
     * Creates RedisTemplate for basic Redis operations.
     * 
     * Configured with String serializers for keys and values.
     * 
     * @param connectionFactory the Redis connection factory
     * @return configured RedisTemplate
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializers for both keys and values
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
    
    /**
     * Parses timeout string (e.g., "2000ms") to milliseconds.
     * 
     * @param timeout timeout string
     * @return timeout in milliseconds
     */
    private int parseTimeout(String timeout) {
        if (timeout.endsWith("ms")) {
            return Integer.parseInt(timeout.substring(0, timeout.length() - 2));
        } else if (timeout.endsWith("s")) {
            return Integer.parseInt(timeout.substring(0, timeout.length() - 1)) * 1000;
        }
        return Integer.parseInt(timeout);
    }
}

