package com.mediqueue.security.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RateLimitingFilterTest {

    private RateLimitingFilter rateLimitingFilter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        rateLimitingFilter = new RateLimitingFilter();
        filterChain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("Requests within rate limit pass through to FilterChain")
    void requestsWithinLimit_passThrough() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitingFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Requests exceeding 30 requests per minute return 429 Too Many Requests")
    void requestsExceedingLimit_return429() throws ServletException, IOException {
        String testIp = "192.168.1.101";

        for (int i = 0; i < 30; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            req.setRemoteAddr(testIp);
            MockHttpServletResponse res = new MockHttpServletResponse();
            rateLimitingFilter.doFilter(req, res, filterChain);
            assertThat(res.getStatus()).isEqualTo(200);
        }

        // 31st request should be rejected
        MockHttpServletRequest rejectedReq = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        rejectedReq.setRemoteAddr(testIp);
        MockHttpServletResponse rejectedRes = new MockHttpServletResponse();

        rateLimitingFilter.doFilter(rejectedReq, rejectedRes, filterChain);

        assertThat(rejectedRes.getStatus()).isEqualTo(429);
        assertThat(rejectedRes.getContentAsString()).contains("Too many requests");
        // Verify filterChain was called only 30 times, not for the 31st request
        verify(filterChain, times(30)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Non-auth paths and GET requests are not rate limited")
    void nonAuthPaths_notRateLimited() throws ServletException, IOException {
        String testIp = "192.168.1.102";

        for (int i = 0; i < 40; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/doctors");
            req.setRemoteAddr(testIp);
            MockHttpServletResponse res = new MockHttpServletResponse();
            rateLimitingFilter.doFilter(req, res, filterChain);
            assertThat(res.getStatus()).isEqualTo(200);
        }

        verify(filterChain, times(40)).doFilter(any(), any());
    }
}
