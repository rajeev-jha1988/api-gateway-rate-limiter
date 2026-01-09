package com.example.apigatewayratelimiter.algo.impl

import com.example.apigatewayratelimiter.algo.RateLimiterStrategy
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.toString

@Service
class TokenBucketRateLimiterStrategy(
    private val redisTemplate: RedisTemplate<String, String>
) : RateLimiterStrategy {

    private val script = """
        local bucket = redis.call('HMGET', KEYS[1], 'tokens', 'last_refill')
        local max_tokens = tonumber(ARGV[1])
        local refill_rate = tonumber(ARGV[2])
        local window_size = tonumber(ARGV[3])
        local current_time = tonumber(ARGV[4])
        
        local tokens = tonumber(bucket[1]) or max_tokens
        local last_refill = tonumber(bucket[2]) or current_time
        
        local elapsed = math.max(0, current_time - last_refill)
        local tokens_to_add = math.floor((elapsed * refill_rate) / window_size)
        
        if tokens_to_add > 0 then
            tokens = math.min(max_tokens, tokens + tokens_to_add)
            -- Only advance last_refill by the time equivalent of added tokens
            last_refill = current_time
        end
        
        local allowed = "0"
        if tokens > 0 then
            tokens = tokens - 1
            allowed = "1"
        end
        
        redis.call('HSET', KEYS[1], 'tokens', tokens, 'last_refill', last_refill)
        redis.call('EXPIRE', KEYS[1], 86400) -- Expire after 24 hours of inactivity
        return allowed
    """.trimIndent()

    // Change Int to Long for better compatibility with Redis responses
    private val redisScript = DefaultRedisScript<String>(script).apply {
        setResultType(String::class.java)
    }

    private val maxTokens = 100
    private val refillRate = 100 // tokens per window
    private val windowSize = 60 // seconds


    override fun isAllowed(clientId: String): Boolean {
       /* // Implement token bucket algorithm logic here
        val currentTime = Instant.now().epochSecond

        val clientData =
            clientBucket.computeIfAbsent(clientId, {
                ClientData(tokens = maxTokenWindow, lastRefillTimestamp = currentTime)
            })

        val timeElapsed = currentTime - clientData.lastRefillTimestamp
        val tokensToAdd = (timeElapsed * refillRate) / windowSize
        clientData.tokens = minOf(maxTokenWindow, clientData.tokens + tokensToAdd.toInt())
        clientData.lastRefillTimestamp = currentTime
        if (clientData.tokens <= 0) {
            return false
        }
        clientData.tokens -= 1
        return true*/


        val key = "token_bucket:$clientId"
        val now = Instant.now().epochSecond
        val result = redisTemplate.execute(
            redisScript,
            listOf(key),
            maxTokens.toString(),
            refillRate.toString(),
            windowSize.toString(),
            now.toString()
        )

// Use safe call ?. and comparison to handle potential nulls
        return result == "1"
    }
}

data class ClientData(
    var tokens: Int,
    var lastRefillTimestamp: Long,
)
