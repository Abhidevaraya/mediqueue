package com.mediqueue.user.service;

import com.mediqueue.common.PageResponse;
import com.mediqueue.exception.BusinessException;
import com.mediqueue.exception.ResourceNotFoundException;
import com.mediqueue.user.dto.UserResponse;
import com.mediqueue.user.entity.User;
import com.mediqueue.user.enums.AccountStatus;
import com.mediqueue.user.enums.UserRole;
import com.mediqueue.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mediqueue.audit.service.ClinicalAuditService;
import com.mediqueue.common.PageResponse;
import com.mediqueue.exception.BusinessException;
import com.mediqueue.exception.ResourceNotFoundException;
import com.mediqueue.user.dto.UserResponse;
import com.mediqueue.user.entity.User;
import com.mediqueue.user.enums.AccountStatus;
import com.mediqueue.user.enums.UserRole;
import com.mediqueue.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ClinicalAuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsers(UserRole role, AccountStatus status, Pageable pageable) {
        Page<User> page = userRepository.findByRoleAndStatus(role, status, pageable);
        return PageResponse.of(page, page.getContent().stream().map(UserResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> searchUsers(UserRole role, AccountStatus status, String search, Pageable pageable) {
        Page<User> page = userRepository.searchUsers(role, status, search, pageable);
        return PageResponse.of(page, page.getContent().stream().map(UserResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateUserStatus(Long targetUserId, Long adminUserId, AccountStatus newStatus) {
        if (targetUserId.equals(adminUserId) && newStatus == AccountStatus.BLOCKED) {
            throw new BusinessException("Administrators cannot block their own account");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + targetUserId));

        user.setAccountStatus(newStatus);
        User updated = userRepository.save(user);

        if (auditService != null) {
            auditService.recordEvent(
                    adminUserId,
                    "ADMIN",
                    "USER_STATUS_UPDATED",
                    "USER",
                    targetUserId,
                    null,
                    "SUCCESS",
                    "Status updated to " + newStatus
            );
        }

        return UserResponse.from(updated);
    }

    @Transactional
    public UserResponse updateUserRole(Long targetUserId, Long adminUserId, UserRole newRole) {
        if (targetUserId.equals(adminUserId) && newRole != UserRole.ADMIN) {
            throw new BusinessException("Administrators cannot demote their own account");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + targetUserId));

        user.setRole(newRole);
        User updated = userRepository.save(user);

        if (auditService != null) {
            auditService.recordEvent(
                    adminUserId,
                    "ADMIN",
                    "USER_ROLE_UPDATED",
                    "USER",
                    targetUserId,
                    null,
                    "SUCCESS",
                    "Role updated to " + newRole
            );
        }

        return UserResponse.from(updated);
    }
}
