package com.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;

/**
 * @author zhaoteng
 * @date 2020/3/15
 */
@Configuration
public class RedisConfig {
    @Bean
    public JedisPool jedisPool() {
        JedisPool jedisPool = new JedisPool();
        return jedisPool;
    }
}
