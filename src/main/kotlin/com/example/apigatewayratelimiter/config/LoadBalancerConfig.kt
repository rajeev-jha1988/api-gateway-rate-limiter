package com.example.apigatewayratelimiter.config

import com.example.apigatewayratelimiter.algo.impl.RoundRobinLoadBalancingStrategy
import com.example.apigatewayratelimiter.algo.impl.TokenBucketRateLimiterStrategy
import com.example.apigatewayratelimiter.loadbalancer.LoadBalancer
import com.example.apigatewayratelimiter.loadbalancer.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class LoadBalancerConfig {
    @Bean
    fun loadBalancer(
        loadBalancingStrategy: RoundRobinLoadBalancingStrategy,
        rateLimiter: TokenBucketRateLimiterStrategy,
    ): LoadBalancer {
        val servers =
            listOf(
                Server(1, url = "http://localhost:8081", healthy = true),
                Server(2, url = "http://localhost:8082", healthy = true),
                Server(3, url = "http://localhost:8083", healthy = true),
            )
        return LoadBalancer(servers, loadBalancingStrategy, rateLimiter, restTemplate())
    }

    @Bean
    fun restTemplate(): RestTemplate = RestTemplate()
}
