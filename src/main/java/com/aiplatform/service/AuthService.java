package com.aiplatform.service;

import com.aiplatform.config.JwtUtil;
import com.aiplatform.dto.AuthResponse;
import com.aiplatform.dto.LoginRequest;
import com.aiplatform.dto.RegisterRequest;
import com.aiplatform.entity.Plan;
import com.aiplatform.entity.User;
import com.aiplatform.repository.PlanRepository;
import com.aiplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        String planName = request.getPlan().toUpperCase();
        Plan plan = planRepository.findByName(planName)
                .orElseThrow(() -> new IllegalArgumentException("Invalid plan: " + planName));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .plan(plan)
                .build();

        userRepository.save(user);

        String token = jwtUtil.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .plan(plan.getName())
                .expiresIn(jwtUtil.getExpirationMs())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow();

        String token = jwtUtil.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .plan(user.getPlan().getName())
                .expiresIn(jwtUtil.getExpirationMs())
                .build();
    }
}
