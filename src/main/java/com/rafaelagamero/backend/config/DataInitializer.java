package com.rafaelagamero.backend.config;

import com.rafaelagamero.backend.model.Role;
import com.rafaelagamero.backend.model.User;
import com.rafaelagamero.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String adminEmail = "admin@rafaelagamero.com.br";
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = User.builder()
                    .name("Administrador")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("admin123"))
                    .phone("(11) 99999-9999")
                    .role(Role.ROLE_ADMIN)
                    .build();

            userRepository.save(admin);
            System.out.println(">>> Usuário ADMINISTRADOR inicial criado com sucesso: " + adminEmail);
        }
    }
}
