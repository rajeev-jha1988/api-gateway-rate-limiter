package com.example.apigatewayratelimiter.loadbalancer

import com.example.apigatewayratelimiter.algo.LoadBalancingStrategy
import com.example.apigatewayratelimiter.algo.RateLimiterStrategy
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class LoadBalancer(
    val servers: List<Server>,
    @Qualifier("roundRobinLoadBalancingStrategy")
    val loadBalancingStrategy: LoadBalancingStrategy,
    @Qualifier(value = "tokenBucketRateLimiterStrategy")
    val rateLimiter: RateLimiterStrategy,
    val restTemplate: RestTemplate,
) {
    fun forwardRequest(
        method: HttpMethod,
        path: String,
        headers: HttpHeaders,
        body: Any?,
    ): ResponseEntity<String> {
        val clientId = extractClientId(headers)
        if (!rateLimiter.isAllowed(clientId)) {
            return ResponseEntity.status(429).body("Rate limit exceeded for client: $clientId")
        }

        // select server using load balancing strategy
        val server = loadBalancingStrategy.getServer(servers)
        // Simulate sending request to the selected server

        // Increment active connections
        server.activeConnections++

        try {
            // Build target URL
            val targetUrl = "${server.url}$path"

            // Create HTTP entity with headers and body
            val httpEntity = HttpEntity(body, headers)

            // Forward the request
            return restTemplate.exchange(
                targetUrl,
                method,
                httpEntity,
                String::class.java,
            )
        } finally {
            // Decrement active connections
            server.activeConnections--
        }
    }

    fun extractClientId(headers: HttpHeaders): String {
        // Extract client ID from request headers or parameters
        return headers.getFirst("Client-Id") ?: "default-client"
    }
}
