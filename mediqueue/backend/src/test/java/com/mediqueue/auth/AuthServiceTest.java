package com.mediqueue.auth;

import com.mediqueue.auth.dto.AuthResponse;
import com.mediqueue.auth.dto.LoginRequest;
import com.mediqueue.auth.dto.RegisterRequest;
import com.mediqueue.auth.service.AuthService;
import com.mediqueue.exception.ConflictException;
import com.mediqueue.patient.entity.Patient;
import com.mediqueue.patient.repository.PatientRepository;
import com.mediqueue.security.JwtService;
import com.mediqueue.security.TokenRevocationService;
import com.mediqueue.user.entity.User;
import com.mediqueue.user.enums.AccountStatus;
import com.mediqueue.user.enums.UserRole;
import com.mediqueue.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private com.mediqueue.department.repository.DepartmentRepository departmentRepository;

    @Mock
    private com.mediqueue.doctor.repository.DoctorRepository doctorRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenRevocationService tokenRevocationService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtExpirationMs", 3600000L);
    }

    @Test
    @DisplayName("Should successfully register a patient with normalized email and encoded password")
    void testRegisterPatientSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Jane Doe")
                .email("  JANE.DOE@example.com ")
                .password("SecurePass123!")
                .phoneNumber("+1234567890")
                .build();

        when(userRepository.existsByEmail("jane.doe@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("$2a$10$encodedHash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("mock.jwt.token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getAccessToken());
        assertEquals("jane.doe@example.com", response.getUser().getEmail());
        assertEquals(UserRole.PATIENT, response.getUser().getRole());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("jane.doe@example.com", savedUser.getEmail());
        assertEquals("$2a$10$encodedHash", savedUser.getPasswordHash());
        assertEquals(UserRole.PATIENT, savedUser.getRole());

        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    @DisplayName("Should reject registration with duplicate email (Conflict 409)")
    void testRegisterDuplicateEmailThrowsConflict() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Jane Doe")
                .email("jane.doe@example.com")
                .password("SecurePass123!")
                .build();

        when(userRepository.existsByEmail("jane.doe@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully log in with valid credentials")
    void testLoginSuccess() {
        LoginRequest request = LoginRequest.builder()
                .email("Jane.Doe@example.com")
                .password("SecurePass123!")
                .build();

        User user = User.builder()
                .id(1L)
                .fullName("Jane Doe")
                .email("jane.doe@example.com")
                .passwordHash("$2a$10$encodedHash")
                .role(UserRole.PATIENT)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail("jane.doe@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SecurePass123!", "$2a$10$encodedHash")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("mock.jwt.token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getAccessToken());
        assertEquals("jane.doe@example.com", response.getUser().getEmail());
    }

    @Test
    @DisplayName("Should throw generic BadCredentialsException for invalid password")
    void testLoginInvalidPassword() {
        LoginRequest request = LoginRequest.builder()
                .email("jane.doe@example.com")
                .password("WrongPassword")
                .build();

        User user = User.builder()
                .id(1L)
                .email("jane.doe@example.com")
                .passwordHash("$2a$10$encodedHash")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail("jane.doe@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "$2a$10$encodedHash")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Should throw generic BadCredentialsException for unknown email without disclosing existence")
    void testLoginUnknownEmail() {
        LoginRequest request = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("Password123!")
                .build();

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Should throw LockedException when account is BLOCKED")
    void testLoginBlockedAccount() {
        LoginRequest request = LoginRequest.builder()
                .email("blocked@example.com")
                .password("Password123!")
                .build();

        User user = User.builder()
                .id(2L)
                .email("blocked@example.com")
                .passwordHash("$2a$10$encodedHash")
                .role(UserRole.PATIENT)
                .accountStatus(AccountStatus.BLOCKED)
                .build();

        when(userRepository.findByEmail("blocked@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "$2a$10$encodedHash")).thenReturn(true);

        assertThrows(LockedException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Should call tokenRevocationService when logging out")
    void testLogoutRevokesToken() {
        String token = "Bearer valid.sample.token";
        authService.logout(token);

        verify(tokenRevocationService).revokeToken("valid.sample.token");
    }
}
