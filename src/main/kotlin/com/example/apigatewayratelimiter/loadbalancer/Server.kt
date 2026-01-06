package com.example.apigatewayratelimiter.loadbalancer

data class Server(
    val serverId: Int,
    val healthy: Boolean,
    var activeConnections: Int = 0,
    val url: String,
)
