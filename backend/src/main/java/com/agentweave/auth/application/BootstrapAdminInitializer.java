package com.agentweave.auth.application;

import com.agentweave.auth.domain.RoleEntity;
import com.agentweave.auth.domain.UserEntity;
import com.agentweave.auth.repository.RoleRepository;
import com.agentweave.auth.repository.UserRepository;
import com.agentweave.shared.security.BootstrapAdminProperties;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final BootstrapAdminProperties properties;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public BootstrapAdminInitializer(
            BootstrapAdminProperties properties,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            Environment environment) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        rejectDefaultPasswordInProduction();
        if (userRepository.existsByUsername(properties.adminUsername())) {
            return;
        }
        RoleEntity adminRole = roleRepository.findByCode("ADMIN")
                .orElseThrow(() -> new IllegalStateException("ADMIN role seed data is missing"));
        UserEntity admin = new UserEntity(
                UUID.randomUUID(),
                properties.adminUsername(),
                "Platform Administrator",
                passwordEncoder.encode(properties.adminPassword()),
                properties.adminEmail());
        admin.replaceRoles(List.of(adminRole));
        userRepository.save(admin);
        log.info("Bootstrap admin user created: {}", properties.adminUsername());
    }

    private void rejectDefaultPasswordInProduction() {
        boolean prod = List.of(environment.getActiveProfiles()).contains("prod");
        if (prod && "admin123".equals(properties.adminPassword())) {
            throw new IllegalStateException("Default admin password is not allowed in prod profile");
        }
    }
}
