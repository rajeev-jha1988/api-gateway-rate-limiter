package com.example.apigatewayratelimiter

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ApiGatewayRateLimiterApplication

fun main(args: Array<String>) {
    runApplication<ApiGatewayRateLimiterApplication>(*args)
}
