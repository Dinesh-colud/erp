package com.erp.config;

import com.erp.entity.Role;
import com.erp.entity.User;
import com.erp.enums.AccountStatus;
import com.erp.enums.RoleName;
import com.erp.repository.RoleRepository;
import com.erp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.username:superadmin}")
    private String adminUsername;

    @Value("${app.bootstrap-admin.email:admin@college.edu}")
    private String adminEmail;

    @Value("${app.bootstrap-admin.password:ChangeMe@123}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        for (RoleName roleName : RoleName.values()) {
            if (!roleRepository.existsByName(roleName)) {
                roleRepository.save(Role.builder().name(roleName).build());
            }
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        boolean adminExists = userRepository.existsByUsername(adminUsername)
                || !userRepository.findByEmail(adminEmail).isEmpty();

        if (!adminExists) {
            User admin = User.builder()
                    .username(adminUsername)
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .firstName("System")
                    .lastName("Administrator")
                    .status(AccountStatus.ACTIVE)
                    .emailVerified(true)
                    .roles(Set.of(adminRole))
                    .build();
            userRepository.save(admin);

            log.warn("=================================================================");
            log.warn(" Bootstrap admin account created: username='{}'", adminUsername);
            log.warn(" Sign in and change this password immediately — it is NOT secure");
            log.warn(" for production. Override app.bootstrap-admin.* to set your own.");
            log.warn("=================================================================");
        }
    }
}
