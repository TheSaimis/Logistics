package com.logistics.inventory.dto;

import com.logistics.inventory.entity.Role;
import com.logistics.inventory.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public final class AuthDtos {

    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {}

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank @Size(max = 255) String fullName) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record TokenResponse(String accessToken, String refreshToken, UserDto user) {}

    public record UserDto(Long id, String email, String fullName, Set<String> roles,
                          boolean enabled, String provider, Instant createdAt) {

        public static UserDto from(User user) {
            return new UserDto(
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                    user.isEnabled(),
                    user.getProvider().name(),
                    user.getCreatedAt());
        }
    }

    public record UserUpsertRequest(
            @NotBlank @Email String email,
            @Size(min = 8, max = 100) String password,
            @NotBlank @Size(max = 255) String fullName,
            Set<String> roles,
            Boolean enabled) {}
}
