package com.example.apigatewayratelimiter.algo.impl

import com.example.apigatewayratelimiter.algo.LoadBalancingStrategy
import com.example.apigatewayratelimiter.loadbalancer.Server
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger

@Service
class RoundRobinLoadBalancingStrategy : LoadBalancingStrategy {
    private val counter = AtomicInteger(0)

    override fun getServer(servers: List<Server>): Server {
        val healthyServers = servers.filter { it.healthy }
        if (healthyServers.isEmpty()) {
            throw Exception("No healthy servers available")
        }
        val serverIndex = counter.getAndIncrement().mod(healthyServers.size)
        return healthyServers[serverIndex]
    }
}
