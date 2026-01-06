package com.example.apigatewayratelimiter.algo.impl

import com.example.apigatewayratelimiter.algo.RateLimiterStrategy
import java.time.Instant

class FixedWindowRateLimiterStrategy(
    val maxWindowSizeInSec: Int,
    val clientBucket: MutableMap<String, Int>,
    val maxToken: Int,
) : RateLimiterStrategy {
    override fun isAllowed(clientId: String): Boolean {
        val currTime = Instant.now().epochSecond / maxWindowSizeInSec
        val clientId = clientId + "_" + currTime

        clientBucket.putIfAbsent(clientId, 0)

        if (clientBucket[clientId]!! < maxToken) {
            clientBucket[clientId] = clientBucket[clientId]!! + 1
            return true
        }

        return false
    }
}
