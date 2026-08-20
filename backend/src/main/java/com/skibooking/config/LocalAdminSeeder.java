package com.skibooking.config;

import java.util.Locale;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.skibooking.entity.User;
import com.skibooking.entity.enums.UserRole;
import com.skibooking.repository.UserRepository;

@Component
@Profile("local")
public class LocalAdminSeeder implements ApplicationRunner {

    private final LocalAdminProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LocalAdminSeeder(
            LocalAdminProperties properties,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!isConfigured()) {
            return;
        }
        String email = properties.email().trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(existing -> {
            if (existing.getRole() != UserRole.ADMIN) {
                throw new IllegalStateException(
                        "LOCAL_ADMIN_EMAIL belongs to a non-admin user and cannot be elevated automatically.");
            }
        }, () -> createAdmin(email));
    }

    private void createAdmin(String email) {
        User admin = new User();
        admin.setFirstName("Snow Alpine");
        admin.setLastName("Admin");
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(properties.password()));
        admin.setRole(UserRole.ADMIN);
        userRepository.save(admin);
    }

    private boolean isConfigured() {
        return hasText(properties.email())
                && hasText(properties.password())
                && !properties.password().contains("replace-with-")
                && properties.password().length() >= 8;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
