package com.example.apigatewayratelimiter

import com.example.apigatewayratelimiter.loadbalancer.LoadBalancer
import com.example.apigatewayratelimiter.resources.GatewayController
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

class GatewayControllerTest {
    private lateinit var gatewayController: GatewayController
    private lateinit var mockLoadBalancer: LoadBalancer
    private lateinit var mockRequest: HttpServletRequest

    @BeforeEach
    fun setup() {
        mockLoadBalancer = mockk()
        mockRequest = mockk()
        gatewayController = GatewayController(mockLoadBalancer)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `should forward GET request to load balancer`() {
        // Given
        val path = "/api/users"
        val headers = HttpHeaders()
        val expectedResponse = ResponseEntity.ok("User list")

        every { mockRequest.method } returns "GET"
        every { mockRequest.requestURI } returns path
        every { mockLoadBalancer.forwardRequest(HttpMethod.GET, path, headers, null) } returns expectedResponse

        // When
        val result = gatewayController.forwardRequest(mockRequest, null, headers)

        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals("User list", result.body)
        verify(exactly = 1) { mockLoadBalancer.forwardRequest(HttpMethod.GET, path, headers, null) }
    }

    @Test
    fun `should forward POST request with body to load balancer`() {
        // Given
        val path = "/api/users"
        val body = """{"name":"John"}"""
        val headers = HttpHeaders()
        val expectedResponse = ResponseEntity.status(HttpStatus.CREATED).body("User created")

        every { mockRequest.method } returns "POST"
        every { mockRequest.requestURI } returns path
        every { mockLoadBalancer.forwardRequest(HttpMethod.POST, path, headers, body) } returns expectedResponse

        // When
        val result = gatewayController.forwardRequest(mockRequest, body, headers)

        // Then
        assertEquals(HttpStatus.CREATED, result.statusCode)
        assertEquals("User created", result.body)
        verify(exactly = 1) { mockLoadBalancer.forwardRequest(HttpMethod.POST, path, headers, body) }
    }

    @Test
    fun `should handle PUT request`() {
        // Given
        val path = "/api/users/1"
        val body = """{"name":"Jane"}"""
        val headers = HttpHeaders()
        val expectedResponse = ResponseEntity.ok("User updated")

        every { mockRequest.method } returns "PUT"
        every { mockRequest.requestURI } returns path
        every { mockLoadBalancer.forwardRequest(HttpMethod.PUT, path, headers, body) } returns expectedResponse

        // When
        val result = gatewayController.forwardRequest(mockRequest, body, headers)

        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        verify(exactly = 1) { mockLoadBalancer.forwardRequest(HttpMethod.PUT, path, headers, body) }
    }

    @Test
    fun `should handle DELETE request`() {
        // Given
        val path = "/api/users/1"
        val headers = HttpHeaders()
        val expectedResponse = ResponseEntity.noContent().build<String>()

        every { mockRequest.method } returns "DELETE"
        every { mockRequest.requestURI } returns path
        every { mockLoadBalancer.forwardRequest(HttpMethod.DELETE, path, headers, null) } returns expectedResponse

        // When
        val result = gatewayController.forwardRequest(mockRequest, null, headers)

        // Then
        assertEquals(HttpStatus.NO_CONTENT, result.statusCode)
        verify(exactly = 1) { mockLoadBalancer.forwardRequest(HttpMethod.DELETE, path, headers, null) }
    }

    @Test
    fun `should handle nested paths`() {
        // Given
        val path = "/api/v1/users/1/orders/5"
        val headers = HttpHeaders()
        val expectedResponse = ResponseEntity.ok("Order details")

        every { mockRequest.method } returns "GET"
        every { mockRequest.requestURI } returns path
        every { mockLoadBalancer.forwardRequest(HttpMethod.GET, path, headers, null) } returns expectedResponse

        // When
        val result = gatewayController.forwardRequest(mockRequest, null, headers)

        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals("Order details", result.body)
    }
}
