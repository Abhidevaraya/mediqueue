package com.mediqueue.websocket;

import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.security.AuthenticatedUser;
import com.mediqueue.security.JwtService;
import com.mediqueue.security.TokenRevocationService;
import com.mediqueue.user.entity.User;
import com.mediqueue.user.enums.AccountStatus;
import com.mediqueue.user.enums.UserRole;
import com.mediqueue.user.repository.UserRepository;
import com.mediqueue.websocket.interceptor.JwtChannelInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketSecurityTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenRevocationService tokenRevocationService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private MessageChannel messageChannel;

    @InjectMocks
    private JwtChannelInterceptor interceptor;

    private User activeUser;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .role(UserRole.PATIENT)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        userDetails = new AuthenticatedUser(activeUser);
    }

    @Test
    @DisplayName("STOMP CONNECT with valid JWT sets authenticated principal")
    void connect_validJwt_success() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer valid.jwt.token");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.isTokenValid("valid.jwt.token")).thenReturn(true);
        when(tokenRevocationService.isRevoked("valid.jwt.token")).thenReturn(false);
        when(jwtService.extractEmail("valid.jwt.token")).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(activeUser));

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertNotNull(result);
        StompHeaderAccessor postAccessor = StompHeaderAccessor.wrap(result);
        assertNotNull(postAccessor.getUser());
        assertEquals("user@example.com", postAccessor.getUser().getName());
    }

    @Test
    @DisplayName("STOMP CONNECT with invalid JWT throws AccessDeniedException")
    void connect_invalidJwt_throwsException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer invalid.jwt.token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.isTokenValid("invalid.jwt.token")).thenReturn(false);

        assertThrows(AccessDeniedException.class, () ->
                interceptor.preSend(message, messageChannel)
        );
    }

    @Test
    @DisplayName("STOMP CONNECT with revoked token throws AccessDeniedException")
    void connect_revokedJwt_throwsException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer revoked.jwt.token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.isTokenValid("revoked.jwt.token")).thenReturn(true);
        when(tokenRevocationService.isRevoked("revoked.jwt.token")).thenReturn(true);

        assertThrows(AccessDeniedException.class, () ->
                interceptor.preSend(message, messageChannel)
        );
    }

    @Test
    @DisplayName("STOMP CONNECT for blocked account throws AccessDeniedException")
    void connect_blockedAccount_throwsException() {
        activeUser.setAccountStatus(AccountStatus.BLOCKED);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer valid.jwt.token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.isTokenValid("valid.jwt.token")).thenReturn(true);
        when(tokenRevocationService.isRevoked("valid.jwt.token")).thenReturn(false);
        when(jwtService.extractEmail("valid.jwt.token")).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(activeUser));

        assertThrows(AccessDeniedException.class, () ->
                interceptor.preSend(message, messageChannel)
        );
    }

    @Test
    @DisplayName("SUBSCRIBE to doctor queue topic denies unauthorized doctor")
    void subscribe_doctorTopic_deniesOtherDoctor() {
        User doctorUser = User.builder()
                .id(2L)
                .email("doc2@example.com")
                .role(UserRole.DOCTOR)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        AuthenticatedUser docPrincipal = new AuthenticatedUser(doctorUser);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/doctors/99/queue");
        accessor.setUser(new UsernamePasswordAuthenticationToken(docPrincipal, null, docPrincipal.getAuthorities()));
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Doctor myDoctor = Doctor.builder().id(50L).build(); // ID is 50, but subscribing to 99!
        when(doctorRepository.findByUserId(2L)).thenReturn(Optional.of(myDoctor));

        assertThrows(AccessDeniedException.class, () ->
                interceptor.preSend(message, messageChannel)
        );
    }
}
