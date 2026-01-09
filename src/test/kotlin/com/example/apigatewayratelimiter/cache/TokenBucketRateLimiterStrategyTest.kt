package com.example.apigatewayratelimiter.cache

import com.example.apigatewayratelimiter.algo.impl.TokenBucketRateLimiterStrategy
import com.example.apigatewayratelimiter.config.RedisConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.RedisTemplate

class TokenBucketRateLimiterStrategyTest {

    private lateinit var redisTemplate: RedisTemplate<String, String>
    private lateinit var rateLimiter: TokenBucketRateLimiterStrategy

    @BeforeEach
    fun setUp() {
        val redisConfig = RedisConfig()
        redisTemplate = redisConfig.redisTemplate()
        rateLimiter = TokenBucketRateLimiterStrategy(redisTemplate)
        // Optionally clear Redis for a clean test
        redisTemplate.delete("token_bucket:test-client")
    }

    @Test
    fun `should allow requests up to max tokens and then deny`() {
        val clientId = "test-client"
        // The default maxTokens is 100
        var allowedCount = 0
        repeat(100) {
            if (rateLimiter.isAllowed(clientId)) allowedCount++
        }
        assertEquals(100, allowedCount)
        // Next request should be denied
        assertFalse(rateLimiter.isAllowed(clientId))
    }
}