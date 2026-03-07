package com.mindrevol.core.config;

import com.mindrevol.core.modules.user.entity.Role;
import com.mindrevol.core.modules.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Data initializer for development profile (H2 database)
 * Creates default roles when application starts
 */
@Slf4j
@Configuration
@Profile("dev")
@RequiredArgsConstructor
public class DevDataInitializer {

    private final RoleRepository roleRepository;

    @Bean
    public CommandLineRunner initDevData() {
        return args -> {
            log.info("🔧 Development mode: Initializing default data...");

            // Create default roles if they don't exist
            if (roleRepository.count() == 0) {
                createRole("ROLE_USER", "Standard user role");
                createRole("ROLE_ADMIN", "Administrator role");
                createRole("ROLE_MODERATOR", "Moderator role");
                log.info("✅ Default roles created successfully");
            } else {
                log.info("✅ Roles already exist, skipping initialization");
            }

            log.info("🚀 Application ready! H2 Console: http://localhost:8080/h2-console");
            log.info("   JDBC URL: jdbc:h2:mem:mindrevol_db");
            log.info("   Username: sa");
            log.info("   Password: (empty)");
        };
    }

    private void createRole(String name, String description) {
        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        roleRepository.save(role);
        log.debug("Created role: {}", name);
    }
}

