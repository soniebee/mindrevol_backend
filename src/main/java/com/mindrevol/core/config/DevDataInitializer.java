package com.mindrevol.core.config;

import com.mindrevol.core.modules.user.entity.Role;
import com.mindrevol.core.modules.user.entity.User;
import com.mindrevol.core.modules.user.repository.RoleRepository;
import com.mindrevol.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder; // <-- Import thêm cái này

@Slf4j
@Configuration
@Profile("dev")
@RequiredArgsConstructor
public class DevDataInitializer {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // <-- Khai báo thêm cái này

    @Bean
    public CommandLineRunner initDevData() {
        return args -> {
            log.info("🔧 Development mode: Initializing default data for PostgreSQL...");

            if (roleRepository.count() == 0) {
                createRole("ROLE_USER", "Standard user role");
                createRole("ROLE_ADMIN", "Administrator role");
                createRole("ROLE_MODERATOR", "Moderator role");
                log.info("✅ Default roles created successfully");
            }

            if (userRepository.count() == 0) {
                User user1 = User.builder()
                        .email("hoanggia.admin@mindrevol.com")
                        .password(passwordEncoder.encode("123456")) // <-- Đã mã hóa mật khẩu
                        .handle("admin_gia")
                        .fullname("Hoàng Gia Admin")
                        .build();
                userRepository.save(user1);

                User user2 = User.builder()
                        .email("thanhvien.test@mindrevol.com")
                        .password(passwordEncoder.encode("123456")) // <-- Đã mã hóa mật khẩu
                        .handle("test_user_01")
                        .fullname("Thành Viên Test")
                        .build();
                userRepository.save(user2);

                log.info("✅ Default test users created successfully in PostgreSQL!");
            } else {
                log.info("✅ Users already exist in PostgreSQL. Ready to test!");
            }
            log.info("🚀 Application is successfully connected to PostgreSQL and Ready!");
        };
    }

    private void createRole(String name, String description) {
        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        roleRepository.save(role);
    }
}