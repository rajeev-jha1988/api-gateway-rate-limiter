package com.example.apigatewayratelimiter.algo.impl

import com.example.apigatewayratelimiter.algo.RateLimiterStrategy
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.TimeUnit

@Service
class TokenBucketRateLimiterStrategy(
    val refillRate: Int = TimeUnit.SECONDS.toSeconds(60).toInt(), // tokens per second
    val maxTokenWindow: Int = 100, // maximum tokens in the bucket
    val windowSize: Int = TimeUnit.SECONDS.toSeconds(60).toInt(), // window size in seconds
    val clientBucket: MutableMap<String, ClientData> = mutableMapOf(),
) : RateLimiterStrategy {
    override fun isAllowed(clientId: String): Boolean {
        // Implement token bucket algorithm logic here
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
        return true
    }
}

data class ClientData(
    var tokens: Int,
    var lastRefillTimestamp: Long,
)
