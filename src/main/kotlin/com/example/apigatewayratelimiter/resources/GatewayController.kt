package com.example.apigatewayratelimiter.resources

import com.example.apigatewayratelimiter.loadbalancer.LoadBalancer
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class GatewayController(
    private val loadBalancer: LoadBalancer,
) {
    @RequestMapping("/**")
    fun forwardRequest(
        request: HttpServletRequest,
        @RequestBody(required = false) body: String?,
        @RequestHeader headers: HttpHeaders,
    ): ResponseEntity<String> {
        val method = HttpMethod.valueOf(request.method)
        val path = request.requestURI

        return loadBalancer.forwardRequest(method, path, headers, body)
    }
}
