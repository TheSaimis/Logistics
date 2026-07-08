package com.logistics.inventory.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Refuses to start outside the dev profile with the published dev-only JWT secret.
 * Without this, anyone with repo access could forge admin tokens in production.
 */
@Component
public class SecurityStartupCheck {

    static final String DEV_DEFAULT_SECRET =
            "ZGV2LW9ubHktc2VjcmV0LWtleS1jaGFuZ2UtbWUtaW4tcHJvZHVjdGlvbi1wbGVhc2UtMTIzNDU2Nzg5MA==";

    private final Environment environment;
    private final String jwtSecret;

    public SecurityStartupCheck(Environment environment, @Value("${app.jwt.secret}") String jwtSecret) {
        this.environment = environment;
        this.jwtSecret = jwtSecret;
    }

    @PostConstruct
    void verify() {
        boolean dev = environment.acceptsProfiles(Profiles.of("dev"));
        if (!dev && DEV_DEFAULT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException(
                    "Refusing to start: JWT_SECRET is still the published dev default. "
                    + "Set the JWT_SECRET environment variable (base64, >= 64 random bytes) "
                    + "or run with the 'dev' profile for local development.");
        }
    }
}
