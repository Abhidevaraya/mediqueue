package com.mediqueue.config;

import com.mediqueue.user.entity.User;
import com.mediqueue.user.enums.AccountStatus;
import com.mediqueue.user.enums.UserRole;
import com.mediqueue.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${mediqueue.bootstrap.admin.email:}")
    private String adminEmail;

    @Value("${mediqueue.bootstrap.admin.password:}")
    private String adminPassword;

    @Value("${mediqueue.bootstrap.admin.name:System Administrator}")
    private String adminName;

    @Override
    public void run(String... args) {
        if (!StringUtils.hasText(adminEmail) || !StringUtils.hasText(adminPassword)) {
            log.info("No admin bootstrap credentials configured. Skipping admin provisioning.");
            return;
        }

        String normalizedEmail = User.normalizeEmail(adminEmail);

        if (userRepository.countByRole(UserRole.ADMIN) > 0) {
            log.info("Administrator accounts already exist. Bootstrap skipped.");
            return;
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Account with email {} already exists with another role. Skipping admin bootstrap.", normalizedEmail);
            return;
        }

        User adminUser = User.builder()
                .fullName(adminName.trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(UserRole.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        userRepository.save(adminUser);
        log.info("Initial system administrator successfully bootstrapped for email: {}", normalizedEmail);
    }
}
