package com.example.bentoreturns.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(@Value("${APP_PASSWORD}") String password, PasswordEncoder passwordEncoder) {
        UserDetails user = User.withUsername("staff")
                               .password(passwordEncoder.encode(password))
                               .roles("STAFF")
                               .build();

        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/api/csrf", "/api/authenticate")
            .permitAll()
            .anyRequest()
            .authenticated()
        );

        http.formLogin(form -> form
            .loginProcessingUrl("/api/authenticate")
            .successHandler((request, response, authentication) -> {
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Login successful\"}");
            })
            .failureHandler((request, response, exception) -> {
                response.setStatus(401);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Incorrect password\"}");
            })
        );

        http.exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint((request, response, exception) -> {
                response.setStatus(401);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Please log in\"}");
            })
        );

        http.requestCache(cache -> cache.disable());

        return http.build();
    }
}
