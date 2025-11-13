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
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

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
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {}) // activer CORS
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Endpoints publics pour la santé et les informations de l'API
                        .requestMatchers("/api/health", "/api/info").permitAll()
                        // Endpoints d'authentification publics
                        .requestMatchers("/api/auth/**").permitAll() // auth/login, register etc.
                        // Routes pour les demandeurs (clients internes)
                        .requestMatchers("/api/demandes/demandeur/create").hasAnyRole("DEMANDEUR", "ADMIN")
                        .requestMatchers("/api/demandes/demandeur/**").hasAnyRole("DEMANDEUR", "ADMIN")
                        // Routes pour l'équipe Cloud
                        .requestMatchers("/api/cloud-team/**").hasAnyRole("EQUIPECLOUD", "ADMIN")
                        // Routes pour l'équipe Support Système
                        .requestMatchers("/api/support-system/**").hasAnyRole("EQUIPESUPPORT", "ADMIN")
                        // Routes pour les administrateurs
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Routes pour le monitoring (accessibles par tous les utilisateurs authentifiés)
                        .requestMatchers("/api/monitoring/**").authenticated()
                        // Routes pour les notifications
                        .requestMatchers("/api/notifications/**").authenticated()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class); // Ajouter le filtre JWT

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // Permettre les credentials (cookies, authorization headers)
        config.setAllowCredentials(true);
        
        // Autoriser les origines pour le développement (vous pouvez les restreindre en production)
        config.addAllowedOriginPattern("*"); // Pour le développement - en production, spécifiez les domaines exacts
        
        // Autoriser tous les headers nécessaires
        config.addAllowedHeader("*");
        config.addAllowedHeader("Authorization");
        config.addAllowedHeader("Content-Type");
        config.addAllowedHeader("X-Requested-With");
        config.addAllowedHeader("Accept");
        config.addAllowedHeader("Origin");
        config.addAllowedHeader("Access-Control-Request-Method");
        config.addAllowedHeader("Access-Control-Request-Headers");
        
        // Autoriser toutes les méthodes HTTP
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("PATCH");
        config.addAllowedMethod("OPTIONS");
        config.addAllowedMethod("HEAD");
        
        // Exposer les headers de réponse
        config.addExposedHeader("Authorization");
        config.addExposedHeader("Content-Type");
        
        // Durée de mise en cache des pré-requêtes OPTIONS
        config.setMaxAge(3600L);
        
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
