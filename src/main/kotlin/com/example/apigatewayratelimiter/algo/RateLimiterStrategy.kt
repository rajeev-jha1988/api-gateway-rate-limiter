package com.example.apigatewayratelimiter.algo

interface RateLimiterStrategy {
    fun isAllowed(clientId: String): Boolean
}
