package com.mathfast.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
@Slf4j
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${REDIS_URL:}")
    private String redisUrl;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        if (redisUrl != null && !redisUrl.trim().isEmpty()) {
            log.info("Redis config: url={}", redisUrl);
            try {
                java.net.URI uri = new java.net.URI(redisUrl);
                RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
                config.setHostName(uri.getHost());
                config.setPort(uri.getPort() == -1 ? 6379 : uri.getPort());
                if (uri.getUserInfo() != null) {
                    String[] userInfo = uri.getUserInfo().split(":", 2);
                    if (userInfo.length > 1) {
                        config.setPassword(userInfo[1]);
                    } else {
                        config.setPassword(userInfo[0]);
                    }
                }
                log.info("Parsed Redis URL: host={} port={}", config.getHostName(), config.getPort());
                return new LettuceConnectionFactory(config);
            } catch (Exception e) {
                log.error("Failed to parse REDIS_URL: {}", redisUrl, e);
            }
        }

        log.info("Redis config: host={} port={} url=<not set>", host, port);
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        if (password != null && !password.trim().isEmpty()) {
            config.setPassword(password);
        }
        return new LettuceConnectionFactory(config);
    }
}
