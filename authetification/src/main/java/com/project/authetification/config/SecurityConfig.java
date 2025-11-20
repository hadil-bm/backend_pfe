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
import org.springframework.web.filter.CorsFilter;

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
                // 1) API REST stateless + JWT : on désactive CSRF
                .csrf(csrf -> csrf.disable())

                // 2) On branche explicitement notre configuration CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3) Pas de session (JWT stateless)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4) Autorisations
                .authorizeHttpRequests(auth -> auth
                        // Endpoints publics pour la santé et les infos
                        .requestMatchers("/api/health", "/api/info").permitAll()
                        // Endpoints d'authentification publics (login, register, etc.)
                        .requestMatchers("/api/auth/**").permitAll()
                        // Important : laisser /error public (Spring Boot 3 / Security 6)
                        .requestMatchers("/error").permitAll()
                        // Routes pour les demandeurs
                        .requestMatchers("/api/demandes/demandeur/create").hasAnyRole("DEMANDEUR", "ADMIN")
                        .requestMatchers("/api/demandes/demandeur/**").hasAnyRole("DEMANDEUR", "ADMIN")
                        // Routes pour l'équipe Cloud
                        .requestMatchers("/api/cloud-team/**").hasAnyRole("EQUIPECLOUD", "ADMIN")
                        // Routes pour l'équipe Support Système
                        .requestMatchers("/api/support-system/**").hasAnyRole("EQUIPESUPPORT", "ADMIN")
                        // Routes pour les administrateurs
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Routes pour le monitoring (authentifiés)
                        .requestMatchers("/api/monitoring/**").authenticated()
                        // Routes pour les notifications
                        .requestMatchers("/api/notifications/**").authenticated()
                        // Tout le reste nécessite une authentification
                        .anyRequest().authenticated()
                )

                // 5) Provider + filtre JWT
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ✅ Version recommandée : utiliser CorsConfigurationSource
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Autoriser l'envoi de cookies / headers d'auth (si nécessaire)
        config.setAllowCredentials(true);

        // Pour le dev : tout autoriser, à restreindre en prod
        config.setAllowedOriginPatterns(List.of("*"));

        // Headers autorisés
        config.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // Méthodes autorisées
        config.setAllowedMethods(Arrays.asList(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS",
                "HEAD"
        ));

        // Headers exposés
        config.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type"
        ));

        // Cache des préflight
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

}
