package com.jobchef.shared.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class TokenHasherTest {
    @Test
    void hashesCredentialDeterministicallyWithoutReturningTheCredential() {
        String credential = "generated-opaque-device-credential";
        String first = TokenHasher.sha256(credential);

        assertEquals(64, first.length());
        assertEquals(first, TokenHasher.sha256(credential));
        assertNotEquals(credential, first);
    }

    @Test
    void differentCredentialsHaveDifferentHashes() {
        assertNotEquals(TokenHasher.sha256("first"), TokenHasher.sha256("second"));
    }
}
