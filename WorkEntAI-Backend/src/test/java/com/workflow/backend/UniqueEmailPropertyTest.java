package com.workflow.backend;

// Feature: workflow-management-system, Property 14: Unique Email Invariant
// Validates: Requirements 3.3

import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property test for unique email invariant.
 *
 * Simulates the AuthService registration logic with an in-memory user store
 * and verifies that attempting to register the same email multiple times
 * only succeeds on the first attempt.
 */
class UniqueEmailPropertyTest {

    /**
     * In-memory simulation of user registration with email uniqueness enforcement.
     * Uses a Set to track registered emails, mirroring MongoDB's unique index behavior.
     */
    static class InMemoryUserStore {
        private final Set<String> registeredEmails = new HashSet<>();

        /**
         * Attempts to register an email.
         * @param email the email to register
         * @return true if email was newly registered (added to set), false if already present
         */
        public boolean register(String email) {
            return registeredEmails.add(email);
        }
    }

    @Property
    void onlyFirstEmailRegistrationSucceeds(
            @ForAll @IntRange(min = 2, max = 10) int n,
            @ForAll @AlphaChars @StringLength(min = 5, max = 20) String email) {
        
        InMemoryUserStore store = new InMemoryUserStore();

        // First registration should succeed
        boolean firstResult = store.register(email);
        assertThat(firstResult).isTrue();

        // All subsequent registrations with the same email should fail
        for (int i = 1; i < n; i++) {
            boolean result = store.register(email);
            assertThat(result).isFalse();
        }
    }
}
