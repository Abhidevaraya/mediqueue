package com.mediqueue.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediqueue.common.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lightweight in-memory rate limiting filter for sensitive public auth endpoints.
 * Defends against credential stuffing and registration spam.
 */
@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 30;
    private static final long WINDOW_MILLIS = 60_000L;

    private final Map<String, RequestBucket> clientBuckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (isRateLimitedPath(path, request.getMethod())) {
            String clientIp = resolveClientIp(request);
            if (!isAllowed(clientIp)) {
                log.warn("Rate limit exceeded for client IP: {} on path: {}", clientIp, path);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                ApiResponse<Void> apiResponse = ApiResponse.error("Too many requests. Please try again after 1 minute.");
                response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimitedPath(String path, String method) {
        if ("POST".equalsIgnoreCase(method)) {
            return path.startsWith("/api/v1/auth/login") || path.startsWith("/api/v1/auth/register");
        }
        return false;
    }

    private boolean isAllowed(String clientIp) {
        long now = System.currentTimeMillis();
        cleanExpiredBuckets(now);

        RequestBucket bucket = clientBuckets.compute(clientIp, (key, existing) -> {
            if (existing == null || now - existing.windowStart > WINDOW_MILLIS) {
                return new RequestBucket(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        return bucket.count.get() <= MAX_REQUESTS_PER_MINUTE;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private void cleanExpiredBuckets(long now) {
        if (clientBuckets.size() > 5000) {
            clientBuckets.entrySet().removeIf(entry -> now - entry.getValue().windowStart > WINDOW_MILLIS);
        }
    }

    private static class RequestBucket {
        final long windowStart;
        final AtomicInteger count;

        RequestBucket(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
