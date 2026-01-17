package com.example.apigatewayratelimiter.config

import io.lettuce.core.api.StatefulConnection
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer

@Suppress("DEPRECATION")
@Configuration
class RedisConfig {
    // Redis configuration beans would go here

    @Bean
    fun redisConnectionFactory(): LettuceConnectionFactory {
        // Configure and return RedisConnectionFactory
        val redisConfig = RedisStandaloneConfiguration("localhost", 6379)

        val poolConfig =
            GenericObjectPoolConfig<StatefulConnection<*, *>>().apply {
                maxTotal = 10
                maxIdle = 5
                minIdle = 1
            }
        val clientConfig =
            LettucePoolingClientConfiguration
                .builder()
                .poolConfig(poolConfig)
                .build()

        val factory = LettuceConnectionFactory(redisConfig, clientConfig)
        factory.afterPropertiesSet() // Initialize the factory
        return factory
    }

    @Bean
    fun redisTemplate(): RedisTemplate<String, String> {
        val template = RedisTemplate<String, String>()
        val stringSerializer = StringRedisSerializer()
        template.connectionFactory = redisConnectionFactory()
        template.keySerializer = stringSerializer
        template.valueSerializer = stringSerializer
        template.hashKeySerializer = stringSerializer
        template.hashValueSerializer = stringSerializer
        template.afterPropertiesSet()
        return template
    }
}
