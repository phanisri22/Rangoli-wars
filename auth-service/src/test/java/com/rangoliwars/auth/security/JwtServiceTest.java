package com.rangoliwars.auth.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-123";

    @Test
    void issuedTokenParsesBack() {
        JwtService jwt = new JwtService(SECRET, 1);
        var claims = jwt.parse(jwt.issue("asha", "PLAYER")).orElseThrow();
        assertEquals("asha", claims.getSubject());
        assertEquals("PLAYER", claims.get("role"));
    }

    @Test
    void tokenFromAnotherSecretIsRejected() {
        String token = new JwtService(SECRET, 1).issue("asha", "PLAYER");
        JwtService other = new JwtService("another-secret-another-secret-another-1", 1);
        assertTrue(other.parse(token).isEmpty());
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwt = new JwtService(SECRET, 0);
        assertTrue(jwt.parse(jwt.issue("asha", "PLAYER")).isEmpty());
    }

    @Test
    void shortSecretIsRefused() {
        assertThrows(IllegalStateException.class, () -> new JwtService("short", 1));
    }
}
