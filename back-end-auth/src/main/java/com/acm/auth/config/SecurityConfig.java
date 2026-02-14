package com.acm.auth.config;

import org.springframework.security.config.Customizer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

        private static final String[] SWAGGER_ENDPOINTS = {
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**"
        };

        // Constructor injection (DIP compliant) - easier to test and mock
        private final CustomJwtDecoder customJwtDecoder;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
                log.info("Configuring security filter chain...");

                httpSecurity
                                .cors(Customizer.withDefaults())
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // Swagger endpoints
                                                .requestMatchers(SWAGGER_ENDPOINTS).permitAll()
                                                // CORS preflight
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                // Public auth endpoints
                                                .requestMatchers("/api/v1/auth/sign-in", "/api/v1/auth/sign-up",
                                                                "/api/v1/auth/sign-up/verify-otp",
                                                                "/api/v1/auth/google",
                                                                "/api/v1/auth/forgot-password",
                                                                "/api/v1/auth/forgot-password/verify-otp",
                                                                "/api/v1/auth/forgot-password/reset",
                                                                "/api/v1/auth/introspect", "/api/v1/auth/refresh")
                                                .permitAll()
                                                // All other requests require authentication
                                                .anyRequest().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwtConfigurer -> jwtConfigurer
                                                                .decoder(customJwtDecoder)
                                                                .jwtAuthenticationConverter(
                                                                                jwtAuthenticationConverter()))
                                                .authenticationEntryPoint(new JwtAuthenticationEntryPoint()));

                log.info("Security configuration completed");
                return httpSecurity.build();
        }

        @Bean
        JwtAuthenticationConverter jwtAuthenticationConverter() {
                JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
                jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");
                jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName("scope");

                JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
                jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

                return jwtAuthenticationConverter;
        }

        @Bean
        PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(10);
        }
}
