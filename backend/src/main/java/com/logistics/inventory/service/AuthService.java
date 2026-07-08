package com.logistics.inventory.service;

import com.logistics.inventory.dto.AuthDtos.*;
import com.logistics.inventory.entity.RefreshToken;
import com.logistics.inventory.entity.Role;
import com.logistics.inventory.entity.User;
import com.logistics.inventory.exception.BadRequestException;
import com.logistics.inventory.repository.RefreshTokenRepository;
import com.logistics.inventory.repository.RoleRepository;
import com.logistics.inventory.repository.UserRepository;
import com.logistics.inventory.security.JwtService;
import com.logistics.inventory.security.LoginAttemptService;
import com.logistics.inventory.security.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    @Value("${app.jwt.refresh-token-days}")
    private long refreshTokenDays;

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BadRequestException("Email is already registered");
        }
        PasswordPolicy.validate(request.password());
        Role viewer = roleRepository.findByName(Role.VIEWER).orElseThrow();
        User user = User.builder()
                .email(request.email().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .roles(Set.of(viewer))
                .build();
        userRepository.save(user);
        return tokensFor(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        if (loginAttemptService.isLocked(request.email())) {
            throw new LockedException("Too many failed login attempts. Try again in "
                    + loginAttemptService.minutesRemaining(request.email()) + " minute(s).");
        }
        User user = userRepository.findByEmailIgnoreCase(request.email()).orElse(null);
        if (user == null || user.getPassword() == null
                || !passwordEncoder.matches(request.password(), user.getPassword())) {
            loginAttemptService.recordFailure(request.email());
            throw new BadCredentialsException("Invalid email or password");
        }
        if (!user.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }
        loginAttemptService.recordSuccess(request.email());
        return tokensFor(user);
    }

    /** Rotates the refresh token: the presented token is revoked and a new one issued. */
    @Transactional
    public TokenResponse refresh(String refreshToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (!stored.isActive()) {
            throw new BadCredentialsException("Refresh token expired or revoked");
        }
        stored.setRevoked(true);
        User user = stored.getUser();
        if (!user.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }
        return tokensFor(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(t -> t.setRevoked(true));
    }

    @Transactional
    public User findOrCreateOAuthUser(String email, String fullName) {
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            Role viewer = roleRepository.findByName(Role.VIEWER).orElseThrow();
            return userRepository.save(User.builder()
                    .email(email.toLowerCase())
                    .fullName(fullName != null ? fullName : email)
                    .provider(User.AuthProvider.GOOGLE)
                    .roles(Set.of(viewer))
                    .build());
        });
    }

    @Transactional
    public String issueRefreshToken(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokenRepository.save(RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(Instant.now().plus(Duration.ofDays(refreshTokenDays)))
                .build());
        return token;
    }

    private TokenResponse tokensFor(User user) {
        return new TokenResponse(
                jwtService.generateAccessToken(user),
                issueRefreshToken(user),
                UserDto.from(user));
    }
}
