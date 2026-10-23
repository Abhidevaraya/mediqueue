package com.mediqueue.websocket.interceptor;

import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.security.AuthenticatedUser;
import com.mediqueue.security.JwtService;
import com.mediqueue.security.TokenRevocationService;
import com.mediqueue.user.entity.User;
import com.mediqueue.user.enums.AccountStatus;
import com.mediqueue.user.enums.UserRole;
import com.mediqueue.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtChannelInterceptor implements ChannelInterceptor {

    private static final Pattern DOCTOR_TOPIC_PATTERN = Pattern.compile("^/topic/doctors/(\\d+)/queue$");

    private final JwtService jwtService;
    private final TokenRevocationService tokenRevocationService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }

        return message;
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        String token = resolveToken(accessor);
        if (!StringUtils.hasText(token)) {
            log.warn("STOMP CONNECT rejected: missing authorization token");
            throw new AccessDeniedException("Missing authorization token for WebSocket connection");
        }

        if (!jwtService.isTokenValid(token)) {
            log.warn("STOMP CONNECT rejected: invalid or expired JWT token");
            throw new AccessDeniedException("Invalid or expired JWT token");
        }

        if (tokenRevocationService.isRevoked(token)) {
            log.warn("STOMP CONNECT rejected: token has been revoked");
            throw new AccessDeniedException("JWT token has been revoked");
        }

        String username = jwtService.extractEmail(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new AccessDeniedException("User not found"));

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            log.warn("STOMP CONNECT rejected: account status is {}", user.getAccountStatus());
            throw new AccessDeniedException("Account is inactive or blocked");
        }

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        accessor.setUser(auth);
        log.info("STOMP connection authenticated successfully for user: {}", username);
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new AccessDeniedException("Authentication required to subscribe to queue topics");
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        // Restrict /topic/doctors/{doctorId}/queue
        Matcher matcher = DOCTOR_TOPIC_PATTERN.matcher(destination);
        if (matcher.matches()) {
            Long doctorId = Long.parseLong(matcher.group(1));
            Object principal = ((UsernamePasswordAuthenticationToken) accessor.getUser()).getPrincipal();

            if (principal instanceof AuthenticatedUser authUser) {
                if (authUser.getRole() == UserRole.ADMIN) {
                    return; // Admin can inspect any queue
                }
                if (authUser.getRole() == UserRole.DOCTOR) {
                    Doctor doctor = doctorRepository.findByUserId(authUser.getId()).orElse(null);
                    if (doctor == null || !doctor.getId().equals(doctorId)) {
                        log.warn("Doctor {} denied subscription to another doctor's queue topic {}", authUser.getUsername(), doctorId);
                        throw new AccessDeniedException("Access denied: you may only subscribe to your own queue topic");
                    }
                    return;
                }
                // Patients can subscribe to see the public queue progress of this doctor
                return;
            }
        }
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String header = authHeaders.get(0);
            if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
                return header.substring(7);
            }
            return header;
        }

        List<String> tokenHeaders = accessor.getNativeHeader("token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.get(0);
        }

        return accessor.getPasscode();
    }
}
