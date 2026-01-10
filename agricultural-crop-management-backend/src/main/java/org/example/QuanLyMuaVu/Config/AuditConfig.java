package org.example.QuanLyMuaVu.Config;

import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing
public class AuditConfig {

    private final UserRepository userRepository;

    public AuditConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public AuditorAware<Long> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }

            String username = authentication.getName();
            if (username == null || "anonymousUser".equals(username)) {
                return Optional.empty();
            }

            return userRepository.findByUsername(username)
                    .map(User::getId);
        };
    }
}
