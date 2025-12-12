package com.project.authetification.config;

import com.project.authetification.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // API REST stateless + JWT : CSRF désactivé sauf pour les endpoints non-API
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/actuator/**") // important pour Actuator/Prometheus
                .disable()
            )

            // CORS global
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Pas de session (JWT stateless)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Autorisations
            .authorizeHttpRequests(auth -> auth
                // Tous les endpoints Actuator ouverts (dont /actuator/prometheus)
                .requestMatchers("/actuator/**").permitAll()
                // Endpoints publics pour la santé et info API
                .requestMatchers("/api/health", "/api/info").permitAll()
                // Auth endpoints
                .requestMatchers("/api/auth/**").permitAll()
                // Important pour Spring Boot 3
                .requestMatchers("/error").permitAll()

                // Routes demandeurs
                .requestMatchers("/api/demandes/demandeur/create")
                    .hasAnyRole("DEMANDEUR", "ADMIN","EQUIPECLOUD","EQUIPESUPPORT")
                .requestMatchers("/api/demandes/demandeur/**")
                    .hasAnyRole("DEMANDEUR", "ADMIN","EQUIPECLOUD","EQUIPESUPPORT")

                // Routes équipes
                .requestMatchers("/api/cloud-team/**").hasAnyRole("EQUIPECLOUD", "ADMIN")
                .requestMatchers("/api/support-system/**").hasAnyRole("EQUIPESUPPORT", "ADMIN")
                .requestMatchers("/api/terraform/local/**").hasAnyRole("EQUIPESUPPORT", "ADMIN")
                .requestMatchers("/api/terraform/**").hasAnyRole("EQUIPESUPPORT", "ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/vms").hasAnyRole("ADMIN", "EQUIPESUPPORT")
                .requestMatchers("/api/monitoring/**").authenticated()
                .requestMatchers("/api/notifications/**").authenticated()

                // Tout le reste nécessite authentification
                .anyRequest().authenticated()
            )

            // Provider + filtre JWT
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin",
            "Access-Control-Request-Method", "Access-Control-Request-Headers"
        ));
        config.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
        ));
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
