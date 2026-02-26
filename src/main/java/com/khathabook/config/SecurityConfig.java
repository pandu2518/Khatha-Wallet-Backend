package com.khathabook.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // ❌ Disable CSRF (REST API)
            .csrf(csrf -> csrf.disable())

            // ✅ Enable CORS (Picks up from WebMvcConfigurer/CorsConfig.java)
            .cors(org.springframework.security.config.Customizer.withDefaults())

            // ❌ Disable default login page
            .formLogin(form -> form.disable())

            // ❌ Disable logout
            .logout(logout -> logout.disable())

            // ❌ Stateless session (API only)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // ✅ Allow API + Google OAuth routes
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/**",
                    "/auth/**",
                    "/"
                ).permitAll()
                .anyRequest().permitAll()
            );

        return http.build();
    }
}
