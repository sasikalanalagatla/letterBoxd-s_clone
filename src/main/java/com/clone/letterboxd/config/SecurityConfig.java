package com.clone.letterboxd.config;

import com.clone.letterboxd.model.User;
import com.clone.letterboxd.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.time.LocalDateTime;
import java.util.Optional;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final UserRepository userRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Keeping it disabled as the original app didn't seem to have security
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll() // Use legacy session-based auth handled by controllers
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/auth/login")
                .successHandler(oauth2AuthenticationSuccessHandler())
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            OAuth2User principal = (OAuth2User) authentication.getPrincipal();
            String email = principal.getAttribute("email");
            String name = principal.getAttribute("name");
            String picture = principal.getAttribute("picture"); // Google profile pic URL

            log.info("Google OAuth2 login success for email: {}", email);

            Optional<User> userOpt = userRepository.findByEmail(email);
            User user;

            if (userOpt.isEmpty()) {
                log.info("Creating new user for email: {}", email);
                user = new User();
                user.setEmail(email);
                user.setUsername(email.split("@")[0] + "_" + System.currentTimeMillis() % 1000); // Simple unique username
                user.setDisplayName(name);
                user.setAvatarUrl(picture);
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                user.setPassword(java.util.UUID.randomUUID().toString());
                userRepository.save(user);
            } else {
                user = userOpt.get();
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
            }

            HttpSession session = request.getSession();
            session.setAttribute("loggedInUser", user);
            session.setAttribute("loggedInUserId", user.getId());

            response.sendRedirect("/");
        };
    }
}
