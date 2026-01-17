package com.example.apigatewayratelimiter

import com.example.apigatewayratelimiter.algo.LoadBalancingStrategy
import com.example.apigatewayratelimiter.algo.RateLimiterStrategy
import com.example.apigatewayratelimiter.loadbalancer.LoadBalancer
import com.example.apigatewayratelimiter.loadbalancer.Server
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import kotlin.test.assertEquals

class LoadBalancerTest {
    private lateinit var loadBalancer: LoadBalancer
    private lateinit var mockRestTemplate: RestTemplate
    private lateinit var mockLoadBalancingStrategy: LoadBalancingStrategy
    private lateinit var mockRateLimiter: RateLimiterStrategy
    private lateinit var servers: List<Server>

    @BeforeEach
    fun setup() {
        mockRestTemplate = mockk()
        mockLoadBalancingStrategy = mockk()
        mockRateLimiter = mockk()

        servers =
            listOf(
                Server(1, url = "http://localhost:8081", healthy = true),
                Server(2, url = "http://localhost:8082", healthy = true),
                Server(3, url = "http://localhost:8083", healthy = true),
            )

        loadBalancer =
            LoadBalancer(
                servers,
                mockLoadBalancingStrategy,
                mockRateLimiter,
                mockRestTemplate,
            )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `should forward GET request successfully`() {
        // Given
        val method = HttpMethod.GET
        val path = "/api/users"
        val headers = HttpHeaders()
        val expectedResponse = ResponseEntity.ok("Success")

        every { mockLoadBalancingStrategy.getServer(any()) } returns servers[0]
        every {
            mockRestTemplate.exchange(
                "http://localhost:8081/api/users",
                HttpMethod.GET,
                any<HttpEntity<Any>>(),
                String::class.java,
            )
        } returns expectedResponse

        // When
        val result = loadBalancer.forwardRequest(method, path, headers, null)

        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals("Success", result.body)
        verify(exactly = 1) { mockRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Any>>(), String::class.java) }
    }

    @Test
    fun `should forward POST request with body`() {
        // Given
        val method = HttpMethod.POST
        val path = "/api/users"
        val headers = HttpHeaders()
        val body = """{"name":"John","email":"john@example.com"}"""
        val expectedResponse = ResponseEntity.status(HttpStatus.CREATED).body("User created")

        every { mockLoadBalancingStrategy.getServer(any()) } returns servers[1]
        every {
            mockRestTemplate.exchange(
                "http://localhost:8082/api/users",
                HttpMethod.POST,
                any<HttpEntity<String>>(),
                String::class.java,
            )
        } returns expectedResponse

        // When
        val result = loadBalancer.forwardRequest(method, path, headers, body)

        // Then
        assertEquals(HttpStatus.CREATED, result.statusCode)
        assertEquals("User created", result.body)
    }

    @Test
    fun `should increment and decrement active connections`() {
        // Given
        val method = HttpMethod.GET
        val path = "/api/test"
        val headers = HttpHeaders()
        val server = servers[0]

        every { mockLoadBalancingStrategy.getServer(any()) } returns server
        every {
            mockRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Any>>(), String::class.java)
        } returns ResponseEntity.ok("OK")

        val initialConnections = server.activeConnections

        // When
        loadBalancer.forwardRequest(method, path, headers, null)

        // Then
        assertEquals(initialConnections, server.activeConnections)
    }

    @Test
    fun `should use different servers for round robin`() {
        // Given
        val method = HttpMethod.GET
        val path = "/api/test"
        val headers = HttpHeaders()

        every { mockLoadBalancingStrategy.getServer(any()) } returnsMany
            listOf(
                servers[0],
                servers[1],
                servers[2],
            )

        every {
            mockRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Any>>(), String::class.java)
        } returns ResponseEntity.ok("OK")

        // When & Then
        loadBalancer.forwardRequest(method, path, headers, null)
        verify {
            mockRestTemplate.exchange(
                match<String> { it.contains("8081") }, // Fix: Use match<String> instead of contains
                any(),
                any<HttpEntity<Any>>(),
                String::class.java,
            )
        }

        loadBalancer.forwardRequest(method, path, headers, null)
        verify {
            mockRestTemplate.exchange(
                match<String> { it.contains("8082") }, // Fix: Use match<String> instead of contains
                any(),
                any<HttpEntity<Any>>(),
                String::class.java,
            )
        }

        loadBalancer.forwardRequest(method, path, headers, null)
        verify {
            mockRestTemplate.exchange(
                match<String> { it.contains("8083") }, // Fix: Use match<String> instead of contains
                any(),
                any<HttpEntity<Any>>(),
                String::class.java,
            )
        }
    }

    @Test
    fun `should preserve request headers`() {
        // Given
        val method = HttpMethod.GET
        val path = "/api/test"
        val headers =
            HttpHeaders().apply {
                set("Authorization", "Bearer token123")
                set("Content-Type", "application/json")
            }

        every { mockLoadBalancingStrategy.getServer(any()) } returns servers[0]
        every {
            mockRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Any>>(), String::class.java)
        } returns ResponseEntity.ok("OK")

        // When
        loadBalancer.forwardRequest(method, path, headers, null)

        // Then
        verify {
            mockRestTemplate.exchange(
                any<String>(),
                any(),
                match<HttpEntity<Any>> {
                    it.headers["Authorization"]?.contains("Bearer token123") == true &&
                        it.headers["Content-Type"]?.contains("application/json") == true
                },
                String::class.java,
            )
        }
    }
}
