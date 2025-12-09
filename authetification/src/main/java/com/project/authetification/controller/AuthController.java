
package com.project.authetification.controller;

import com.project.authetification.model.User;
import com.project.authetification.service.AuthService;
import com.project.authetification.service.JwtService;
import com.project.authetification.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.*;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    private final AuthService authService;



    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, Object> registrationRequest) {
        String username = (String) registrationRequest.get("username");
        String password = (String) registrationRequest.get("password");
        String email = (String) registrationRequest.get("email");

        // Récupérer role comme liste depuis JSON
        List<String> roles;
        try {
            roles = (List<String>) registrationRequest.get("role");
        } catch (ClassCastException e) {
            return ResponseEntity.badRequest().body("Le rôle doit être une liste de chaînes.");
        }

        if (username == null || username.trim().isEmpty() ||
                password == null || password.trim().isEmpty() ||
                email == null || email.trim().isEmpty() ||
                roles == null || roles.isEmpty()) {
            return ResponseEntity.badRequest().body("Nom d'utilisateur, email, mot de passe et rôle(s) sont requis.");
        }

        Optional<User> registeredUser = authService.register(username, email, password, roles);

        if (registeredUser.isPresent()) {
            return ResponseEntity.status(HttpStatus.CREATED).body("Utilisateur enregistré avec succès.");
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email déjà existant.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Nom d'utilisateur et mot de passe requis.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            // Retrieve the full user object from the database
            Optional<User> userOptional = authService.findByUsername(username);

            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Utilisateur introuvable.");
            }

            User user = userOptional.get();
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            String jwtToken = jwtService.generateToken(userDetails);

            // Prepare response with token and user data
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Connexion réussie pour l'utilisateur : " + username);
            response.put("token", jwtToken);

            // User data (excluding sensitive information like password and reset tokens)
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getUsername());
            userData.put("email", user.getEmail());
            userData.put("roles", user.getRoles());

            response.put("user", userData);

            return ResponseEntity.ok(response);

        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Mot de passe incorrect.");
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Nom d'utilisateur introuvable.");
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Email requis.");
        }

        authService.processForgotPasswordRequest(email);
        return ResponseEntity.ok("Un email de réinitialisation du mot de passe a été envoyé si l'email existe.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");

        if (token == null || token.trim().isEmpty() || newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Le token et le nouveau mot de passe sont requis.");
        }

        boolean isPasswordReset = authService.resetPassword(token, newPassword);
        if (isPasswordReset) {
            return ResponseEntity.ok("Mot de passe réinitialisé avec succès.");
        } else {
            return ResponseEntity.badRequest().body("Token invalide ou expiré.");
        }
    }
}