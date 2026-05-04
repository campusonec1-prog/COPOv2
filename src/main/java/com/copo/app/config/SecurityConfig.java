package com.copo.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF: disabled because app uses session-based custom auth (not Spring Security forms)
            // If you add REST API clients, re-enable with token-based CSRF
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth
                // Public routes
                .requestMatchers(
                    "/login/**",
                    "/register/**",
                    "/css/**", "/js/**", "/images/**",
                    "/students/sections",
                    "/actuator/health",
                    "/actuator/info"
                ).permitAll()

                // BLOCK setup/** in all cases at Spring Security level
                // (SetupController is also @Profile("dev") but defense-in-depth)
                .requestMatchers("/setup/**").denyAll()

                // All other requests still pass through to the RoleFilter
                // (Spring Security is NOT the primary role guard for this app)
                .anyRequest().permitAll()
            )

            // Custom login — disable Spring Security's default forms
            .formLogin(form -> form.disable())
            .logout(logout -> logout.disable())

            // Prevent session fixation
            .sessionManagement(session -> session
                .sessionFixation().migrateSession()
            )

            // Security headers
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
                .contentTypeOptions(contentType -> {})
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
            );

        return http.build();
    }
}
