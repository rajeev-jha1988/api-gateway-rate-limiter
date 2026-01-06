package com.example.apigatewayratelimiter.algo.impl

import com.example.apigatewayratelimiter.algo.LoadBalancingStrategy
import com.example.apigatewayratelimiter.loadbalancer.Server
import org.springframework.stereotype.Service

@Service
class LeastConnectionLoadBalancingStrategy : LoadBalancingStrategy {
    override fun getServer(servers: List<Server>): Server =
        servers.filter { it.healthy }.minByOrNull { server -> server.activeConnections }!!
}
