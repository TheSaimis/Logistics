package com.logistics.inventory.service;

import com.logistics.inventory.dto.AuthDtos.UserDto;
import com.logistics.inventory.dto.AuthDtos.UserUpsertRequest;
import com.logistics.inventory.entity.Role;
import com.logistics.inventory.entity.User;
import com.logistics.inventory.exception.BadRequestException;
import com.logistics.inventory.exception.NotFoundException;
import com.logistics.inventory.repository.RoleRepository;
import com.logistics.inventory.repository.UserRepository;
import com.logistics.inventory.security.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<UserDto> findAll() {
        return userRepository.findAll().stream().map(UserDto::from).toList();
    }

    @Transactional
    public UserDto create(UserUpsertRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BadRequestException("Email is already registered");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new BadRequestException("Password is required");
        }
        PasswordPolicy.validate(request.password());
        User user = User.builder()
                .email(request.email().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .roles(resolveRoles(request.roles()))
                .enabled(request.enabled() == null || request.enabled())
                .build();
        userRepository.save(user);
        auditService.record("USER_CREATED", "User", user.getId(),
                user.getEmail() + " with roles " + request.roles());
        return UserDto.from(user);
    }

    @Transactional
    public UserDto update(Long id, UserUpsertRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> NotFoundException.of("User", id));
        StringBuilder changes = new StringBuilder();
        user.setFullName(request.fullName());
        if (request.password() != null && !request.password().isBlank()) {
            PasswordPolicy.validate(request.password());
            user.setPassword(passwordEncoder.encode(request.password()));
            changes.append("password changed; ");
        }
        if (request.roles() != null && !request.roles().isEmpty()) {
            Set<String> before = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
            if (!before.equals(request.roles())) {
                changes.append("roles ").append(before).append(" -> ").append(request.roles()).append("; ");
            }
            user.setRoles(resolveRoles(request.roles()));
        }
        if (request.enabled() != null && request.enabled() != user.isEnabled()) {
            changes.append(request.enabled() ? "enabled; " : "disabled; ");
            user.setEnabled(request.enabled());
        }
        auditService.record("USER_UPDATED", "User", user.getId(),
                user.getEmail() + ": " + (changes.isEmpty() ? "profile updated" : changes.toString().trim()));
        return UserDto.from(user);
    }

    @Transactional
    public void delete(Long id, String currentUserEmail) {
        User user = userRepository.findById(id).orElseThrow(() -> NotFoundException.of("User", id));
        if (user.getEmail().equalsIgnoreCase(currentUserEmail)) {
            throw new BadRequestException("You cannot delete your own account");
        }
        userRepository.delete(user);
        auditService.record("USER_DELETED", "User", id, user.getEmail());
    }

    private Set<Role> resolveRoles(Set<String> names) {
        Set<String> effective = (names == null || names.isEmpty()) ? Set.of(Role.VIEWER) : names;
        return effective.stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new BadRequestException("Unknown role: " + name)))
                .collect(Collectors.toSet());
    }
}
