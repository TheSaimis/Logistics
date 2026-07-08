package com.logistics.inventory.config;

import com.logistics.inventory.entity.Role;
import com.logistics.inventory.entity.User;
import com.logistics.inventory.repository.RoleRepository;
import com.logistics.inventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Seeds demo users on first startup — dev profile only, so known demo
 * credentials can never appear in a production database.
 * Passwords are bcrypt-encoded here instead of in Flyway SQL so the
 * encoder stays the single source of truth.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class UserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seed("admin@logistics.local", "Admin123!", "System Administrator", Role.ADMIN);
        seed("manager@logistics.local", "Manager123!", "Warehouse Manager", Role.MANAGER);
        seed("viewer@logistics.local", "Viewer123!", "Read-only Viewer", Role.VIEWER);
    }

    private void seed(String email, String password, String fullName, String roleName) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        Role role = roleRepository.findByName(roleName).orElseThrow();
        userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .fullName(fullName)
                .roles(Set.of(role))
                .build());
        log.info("Seeded demo user {} with role {}", email, roleName);
    }
}
