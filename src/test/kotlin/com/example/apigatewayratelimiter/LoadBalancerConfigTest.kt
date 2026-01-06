package com.example.apigatewayratelimiter

import com.example.apigatewayratelimiter.algo.impl.RoundRobinLoadBalancingStrategy
import com.example.apigatewayratelimiter.algo.impl.TokenBucketRateLimiterStrategy
import com.example.apigatewayratelimiter.config.LoadBalancerConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestTemplate

class LoadBalancerConfigTest {
    private val config = LoadBalancerConfig()

    @Test
    fun `should create RestTemplate bean`() {
        // When
        val restTemplate = config.restTemplate()

        // Then
        assertNotNull(restTemplate)
        assertTrue(restTemplate is RestTemplate)
    }

    @Test
    fun `should create LoadBalancer with three servers`() {
        // Given
        val loadBalancingStrategy = RoundRobinLoadBalancingStrategy()
        val rateLimiter = TokenBucketRateLimiterStrategy()

        // When
        val loadBalancer = config.loadBalancer(loadBalancingStrategy, rateLimiter)

        // Then
        assertNotNull(loadBalancer)
        assertEquals(3, loadBalancer.servers.size)
        assertEquals("http://localhost:8081", loadBalancer.servers[0].url)
        assertEquals("http://localhost:8082", loadBalancer.servers[1].url)
        assertEquals("http://localhost:8083", loadBalancer.servers[2].url)
        assertTrue(loadBalancer.servers.all { it.healthy })
    }

    @Test
    fun `should configure servers with correct IDs`() {
        // Given
        val loadBalancingStrategy = RoundRobinLoadBalancingStrategy()
        val rateLimiter = TokenBucketRateLimiterStrategy()

        // When
        val loadBalancer = config.loadBalancer(loadBalancingStrategy, rateLimiter)

        // Then
        assertEquals(1, loadBalancer.servers[0].serverId)
        assertEquals(2, loadBalancer.servers[1].serverId)
        assertEquals(3, loadBalancer.servers[2].serverId)
    }
}
