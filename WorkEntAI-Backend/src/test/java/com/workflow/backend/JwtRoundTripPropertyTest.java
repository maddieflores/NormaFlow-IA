package com.workflow.backend;

// Feature: workflow-management-system, Property 1: JWT Round-Trip
// Validates: Requirements 1.1, 1.7

import com.workflow.backend.config.JwtUtil;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property test for JWT Round-Trip.
 *
 * Verifies that for any (email, rol) pair:
 * - JwtUtil.extractEmail(generateToken(email, rol)) == email
 * - JwtUtil.extractRol(generateToken(email, rol)) == rol
 */
class JwtRoundTripPropertyTest {

    private JwtUtil createJwtUtil() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "test-secret-key-that-is-at-least-256-bits-long-for-HS256-algorithm");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L);
        return jwtUtil;
    }

    @Property
    void jwtRoundTripPreservesEmailAndRol(
            @ForAll("emails") String email,
            @ForAll("roles") String rol) {

        JwtUtil jwtUtil = createJwtUtil();
        String token = jwtUtil.generateToken(email, rol);

        assertThat(jwtUtil.extractEmail(token))
                .as("extractEmail should return the original email")
                .isEqualTo(email);

        assertThat(jwtUtil.extractRol(token))
                .as("extractRol should return the original rol")
                .isEqualTo(rol);
    }

    @Provide
    Arbitrary<String> emails() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(20)
                .map(local -> local + "@example.com");
    }

    @Provide
    Arbitrary<String> roles() {
        return Arbitraries.of("ADMIN", "FUNCIONARIO", "CLIENTE");
    }
}
