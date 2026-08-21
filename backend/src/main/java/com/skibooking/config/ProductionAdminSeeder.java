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
@Profile("production")
public class ProductionAdminSeeder implements ApplicationRunner {

    private final ProductionAdminProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProductionAdminSeeder(
            ProductionAdminProperties properties,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = properties.email().trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(existing -> {
            if (existing.getRole() != UserRole.ADMIN) {
                throw new IllegalStateException(
                        "INITIAL_ADMIN_EMAIL belongs to a customer and cannot be promoted automatically.");
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
}
