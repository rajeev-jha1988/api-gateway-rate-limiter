package com.example.apigatewayratelimiter.algo

import com.example.apigatewayratelimiter.loadbalancer.Server

interface LoadBalancingStrategy {
    fun getServer(servers: List<Server>): Server
}
