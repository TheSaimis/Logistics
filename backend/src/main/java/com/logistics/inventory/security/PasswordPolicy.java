package com.logistics.inventory.security;

import com.logistics.inventory.exception.BadRequestException;

import java.util.Set;

/** Shared password rules for self-registration and admin-created accounts. */
public final class PasswordPolicy {

    private static final int MIN_LENGTH = 8;

    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password", "password1", "password123", "12345678", "123456789", "1234567890",
            "qwerty123", "qwertyuiop", "letmein1", "welcome1", "iloveyou1", "admin123",
            "changeme1", "sunshine1", "monkey123", "dragon123");

    private PasswordPolicy() {}

    public static void validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new BadRequestException("Password must be at least " + MIN_LENGTH + " characters long");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new BadRequestException("Password must contain both letters and numbers");
        }
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            throw new BadRequestException("This password is too common — please choose a stronger one");
        }
    }
}
