package com.planer.planner.runner;

import com.planer.planner.model.Admin;
import com.planer.planner.repository.AdminRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Seed an initial admin user if it doesn't exist
        if (adminRepository.findByUsername("admin").isEmpty()) {
            Admin admin = new Admin(
                    "admin",
                    passwordEncoder.encode("admin123"),
                    "ROLE_ADMIN"
            );
            adminRepository.save(admin);
            System.out.println("---- Usuario Admin creado correctamente (admin / admin123) ----");
        }
    }
}
