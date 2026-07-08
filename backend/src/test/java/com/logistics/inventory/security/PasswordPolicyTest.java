package com.logistics.inventory.security;

import com.logistics.inventory.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    @Test
    void acceptsStrongPassword() {
        assertThatCode(() -> PasswordPolicy.validate("Correct7Horse")).doesNotThrowAnyException();
    }

    @Test
    void rejectsShortPassword() {
        assertThatThrownBy(() -> PasswordPolicy.validate("Ab1"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsLettersOnly() {
        assertThatThrownBy(() -> PasswordPolicy.validate("abcdefghij"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsDigitsOnly() {
        assertThatThrownBy(() -> PasswordPolicy.validate("1234567891"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsCommonPassword() {
        assertThatThrownBy(() -> PasswordPolicy.validate("Password123"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("common");
    }
}
