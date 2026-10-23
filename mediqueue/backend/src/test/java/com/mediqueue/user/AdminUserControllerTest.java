package com.mediqueue.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediqueue.common.PageResponse;
import com.mediqueue.exception.BusinessException;
import com.mediqueue.exception.GlobalExceptionHandler;
import com.mediqueue.security.AuthenticatedUser;
import com.mediqueue.security.JwtService;
import com.mediqueue.security.TokenRevocationService;
import com.mediqueue.user.controller.AdminUserController;
import com.mediqueue.user.dto.UpdateAccountStatusRequest;
import com.mediqueue.user.dto.UserResponse;
import com.mediqueue.user.entity.User;
import com.mediqueue.user.enums.AccountStatus;
import com.mediqueue.user.enums.UserRole;
import com.mediqueue.user.repository.UserRepository;
import com.mediqueue.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private TokenRevocationService tokenRevocationService;

    @MockBean
    private UserRepository userRepository;

    private void setupAdminAuthentication(Long adminId) {
        User admin = User.builder()
                .id(adminId)
                .email("admin@mediqueue.com")
                .fullName("System Admin")
                .role(UserRole.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        AuthenticatedUser principal = new AuthenticatedUser(admin);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("GET /api/v1/admin/users should list all users")
    void testListUsers() throws Exception {
        setupAdminAuthentication(1L);

        UserResponse user = UserResponse.builder()
                .id(2L)
                .fullName("Jane Doe")
                .email("jane@example.com")
                .role(UserRole.PATIENT)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        PageResponse<UserResponse> pageResponse = PageResponse.<UserResponse>builder()
                .content(List.of(user))
                .pageNumber(0)
                .pageSize(20)
                .totalElements(1)
                .totalPages(1)
                .isFirst(true)
                .isLast(true)
                .build();

        when(userService.getUsers(any(), any(), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].email").value("jane@example.com"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{id}/status should prevent admin self-lockout")
    void testPreventAdminSelfLockout() throws Exception {
        setupAdminAuthentication(1L);

        UpdateAccountStatusRequest request = UpdateAccountStatusRequest.builder()
                .status(AccountStatus.BLOCKED)
                .build();

        when(userService.updateUserStatus(eq(1L), eq(1L), eq(AccountStatus.BLOCKED)))
                .thenThrow(new BusinessException("Administrators cannot block their own account"));

        mockMvc.perform(patch("/api/v1/admin/users/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_ERROR"))
                .andExpect(jsonPath("$.message").value("Administrators cannot block their own account"));
    }
}
